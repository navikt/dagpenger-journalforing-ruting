package no.nav.dagpenger.journalføring.ruting

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import mu.KotlinLogging
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.common.embeddedutils.getAvailablePort
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.avro.Journalpost
import no.nav.dagpenger.events.avro.JournalpostType
import no.nav.dagpenger.events.avro.Søker
import no.nav.dagpenger.streams.Topics
import no.nav.dagpenger.streams.Topics.INNGÅENDE_JOURNALPOST
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.time.Duration
import java.util.Properties
import java.util.Random
import java.util.UUID
import kotlin.test.assertEquals

class JournalføringRutingComponentTest {

    private val LOGGER = KotlinLogging.logger {}

    companion object {
        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        val embeddedEnvironment = KafkaEnvironment(
            users = listOf(JAASCredential(username, password)),
            autoStart = false,
            withSchemaRegistry = true,
            withSecurity = true,
            topics = listOf(Topics.INNGÅENDE_JOURNALPOST.name)
        )

        @BeforeClass
        @JvmStatic
        fun setup() {
            embeddedEnvironment.start()
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            embeddedEnvironment.tearDown()
        }
    }

    @Test
    fun ` embedded kafka cluster is up and running `() {
        kotlin.test.assertEquals(embeddedEnvironment.serverPark.status, KafkaEnvironment.ServerParkStatus.Started)
    }

    @Test
    fun ` Skal kunne legge på behandlende enhet`() {

        val innkommendeBehov = mapOf(
            Random().nextLong().toString() to true,
            Random().nextLong().toString() to false,
            Random().nextLong().toString() to true,
            Random().nextLong().toString() to true,
            Random().nextLong().toString() to false,
            Random().nextLong().toString() to true,
            Random().nextLong().toString() to true,
            Random().nextLong().toString() to true,
            Random().nextLong().toString() to false,
            Random().nextLong().toString() to true
        )

        // given an environment
        val env = Environment(
            dagpengerOppslagUrl = "local",
            username = username,
            password = password,
            bootstrapServersUrl = embeddedEnvironment.brokersURL,
            schemaRegistryUrl = embeddedEnvironment.schemaRegistry!!.url,
            httpPort = getAvailablePort()
        )

        val ruting = JournalføringRuting(env, DummyOppslagClient())

        //produce behov...

        val behovProducer = behovProducer(env)

        ruting.start()

        innkommendeBehov.forEach { fødselsnummer, behandlendeEnhet ->
            val innkommendeBehov: Behov = Behov
                .newBuilder()
                .setBehovId( UUID.randomUUID().toString())
                .setJournalpost(
                    Journalpost
                        .newBuilder()
                        .setJournalpostId( UUID.randomUUID().toString())
                        .setSøker(Søker(fødselsnummer))
                        .setJournalpostType(JournalpostType.NY)
                        .setBehandleneEnhet(if (behandlendeEnhet) "behandledeENHET" else null)
                        .build()
                )
                .build()
            val record = behovProducer.send(ProducerRecord(INNGÅENDE_JOURNALPOST.name, innkommendeBehov)).get()
            LOGGER.info { "Produced -> ${record.topic()}  to offset ${record.offset()}" }
        }

        val behovConsumer: KafkaConsumer<String, Behov> = behovConsumer(env)
        val behovsListe = behovConsumer.poll(Duration.ofSeconds(5)).toList()

        ruting.stop()

        assertEquals(13, behovsListe.size)

        val lagtTilBehandlendeEnhet = behovsListe.filter { kanskjeBehandletBehov ->
            innkommendeBehov.filterValues { !it }.containsKey(kanskjeBehandletBehov.value().getJournalpost().getSøker().getIdentifikator()) && kanskjeBehandletBehov.value().getJournalpost().getBehandleneEnhet() != null
        }.size
        assertEquals(innkommendeBehov.filterValues { !it }.size, lagtTilBehandlendeEnhet)
    }

    class DummyOppslagClient : OppslagClient {
        override fun hentBehandlendeEnhet(request: BehandlendeEnhetRequest): String {
            return "test"
        }

        override fun hentGeografiskTilknytning(fødselsNummer: String): GeografiskTilknytningResponse {
            return GeografiskTilknytningResponse("BLA", "1")
        }
    }

    private fun behovProducer(env: Environment): KafkaProducer<String, Behov> {
        val producer: KafkaProducer<String, Behov> = KafkaProducer(Properties().apply {
            put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, env.schemaRegistryUrl)
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
            put(ProducerConfig.CLIENT_ID_CONFIG, "dummy-behov-producer")
            put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                Topics.INNGÅENDE_JOURNALPOST.keySerde.serializer().javaClass.name
            )
            put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                Topics.INNGÅENDE_JOURNALPOST.valueSerde.serializer().javaClass.name
            )
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${env.username}\" password=\"${env.password}\";"
            )
        })

        return producer
    }

    private fun behovConsumer(env: Environment): KafkaConsumer<String, Behov> {
        val consumer: KafkaConsumer<String, Behov> = KafkaConsumer(Properties().apply {
            put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, env.schemaRegistryUrl)
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-dagpenger-ruting-consumer")
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                INNGÅENDE_JOURNALPOST.keySerde.deserializer().javaClass.name
            )
            put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                INNGÅENDE_JOURNALPOST.valueSerde.deserializer().javaClass.name
            )
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${env.username}\" password=\"${env.password}\";"
            )
        })

        consumer.subscribe(listOf(INNGÅENDE_JOURNALPOST.name))
        return consumer
    }
}