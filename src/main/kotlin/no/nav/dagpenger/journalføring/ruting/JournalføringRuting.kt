package no.nav.dagpenger.journalføring.ruting

import io.prometheus.client.Counter
import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.hasBehandlendeEnhet
import no.nav.dagpenger.events.hasHenvendelsesType

import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.kstream.Predicate
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class JournalføringRuting(val configuration: Configuration, private val oppslagClient: OppslagClient) : River() {

    override val SERVICE_APP_ID =
        "journalføring-ruting" // NB: also used as group.id for the consumer group - do not change!

    override val HTTP_PORT: Int = configuration.application.httpPort
    private val DAGPENGER_NAMESPACE = "dagpenger"

    private val labelNames = listOf(
        "behandlendeEnhet",
        "diskresjonsKode"
    )

    private val jpCounter = Counter
        .build()
        .namespace(DAGPENGER_NAMESPACE)
        .name("journalpost_behandlende_enhet")
        .help("Number of Journalposts processed by journalƒøring-ruting")
        .labelNames(*labelNames.toTypedArray())
        .register()

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return emptyList()
    }

    override fun onPacket(packet: Packet): Packet {
        return packet
        // TODO("not implemented")
    }

    // override fun buildTopology(): Topology {
    //     val builder = StreamsBuilder()
    //
    //     val inngåendeJournalposter = builder.consumeTopic(INNGÅENDE_JOURNALPOST, env.schemaRegistryUrl)
    //
    //     inngåendeJournalposter
    //         .peek { key, value -> LOGGER.info("Processing ${value.javaClass} with key $key") }
    //         .filter { _, behov -> shouldBeProcessed(behov) }
    //         .mapValues(this::addBehandleneEnhet)
    //         .peek { key, value -> LOGGER.info("Producing ${value.javaClass} with key $key") }
    //         .toTopic(INNGÅENDE_JOURNALPOST, co)
    //     return builder.build()
    // }

    override fun getConfig(): Properties {
        val props = streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = configuration.kafka.brokers,
            credential = KafkaCredential(configuration.application.user, configuration.application.password)
        )
        return props
    }

    private fun addBehandleneEnhet(behov: Behov): Behov {
        val fødselsnummer = behov.getMottaker().getIdentifikator()

        val (geografiskTilknytning, diskresjonsKode) = oppslagClient.hentGeografiskTilknytning(
            GeografiskTilknytningRequest(fødselsnummer)
        )

        val (behandlendeEnheter) = oppslagClient.hentBehandlendeEnhet(
            BehandlendeEnhetRequest(geografiskTilknytning, diskresjonsKode)
        )

        val behandlendeEnhet = behandlendeEnheter.minBy { it.enhetId } ?: throw RutingException("Missing enhetId")

        jpCounter.labels(behandlendeEnhet.enhetId, diskresjonsKode).inc()
        behov.setBehandleneEnhet(behandlendeEnhet.enhetId)
        return behov
    }
}

fun main(args: Array<String>) {
    val configuration = Configuration()
    val service = JournalføringRuting(configuration, OppslagHttpClient(configuration.application.oppslagUrl))
    service.start()
}

fun shouldBeProcessed(behov: Behov): Boolean =
    behov.hasHenvendelsesType() && !behov.hasBehandlendeEnhet()

class RutingException(override val message: String) : RuntimeException(message)
