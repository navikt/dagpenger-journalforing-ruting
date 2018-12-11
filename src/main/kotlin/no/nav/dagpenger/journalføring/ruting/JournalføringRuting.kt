package no.nav.dagpenger.journalføring.ruting

import mu.KotlinLogging
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.hasBehandlendeEnhet
import no.nav.dagpenger.events.hasHenvendelsesType
import no.nav.dagpenger.metrics.aCounter
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Service
import no.nav.dagpenger.streams.Topics.INNGÅENDE_JOURNALPOST
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfig
import no.nav.dagpenger.streams.toTopic
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class JournalføringRuting(val env: Environment, private val oppslagClient: OppslagClient) : Service() {
    override val SERVICE_APP_ID =
        "journalføring-ruting" // NB: also used as group.id for the consumer group - do not change!

    override val HTTP_PORT: Int = env.httpPort ?: super.HTTP_PORT

    private val jpCounter = aCounter(
        name = "journalpost_behandlende_enhet",
        labelNames = listOf("behandlendeEnhet", "diskresjonsKode"),
        help = "Number of Journalposts processed by journalƒøring-ruting"
    )
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val env = Environment()
            val service = JournalføringRuting(env, OppslagHttpClient(env.dagpengerOppslagUrl))
            service.start()
        }
    }

    override fun setupStreams(): KafkaStreams {
        LOGGER.info { "Initiating start of $SERVICE_APP_ID" }

        val builder = StreamsBuilder()

        val inngåendeJournalposter = builder.consumeTopic(INNGÅENDE_JOURNALPOST, env.schemaRegistryUrl)

        inngåendeJournalposter
            .peek { key, value -> LOGGER.info("Processing ${value.javaClass} with key $key") }
            .filter { _, behov -> shouldBeProcessed(behov) }
            .mapValues(this::addBehandleneEnhet)
            .peek { key, value -> LOGGER.info("Producing ${value.javaClass} with key $key") }
            .toTopic(INNGÅENDE_JOURNALPOST, env.schemaRegistryUrl)

        return KafkaStreams(builder.build(), this.getConfig())
    }

    override fun getConfig(): Properties {
        val props = streamConfig(
                appId = SERVICE_APP_ID,
                bootStapServerUrl = env.bootstrapServersUrl,
                credential = KafkaCredential(env.username, env.password)
        )
        return props
    }

    private fun addBehandleneEnhet(behov: Behov): Behov {
        val fødselsnummer = behov.getMottaker().getIdentifikator()

        val (geografiskTilknytning, diskresjonsKode) = oppslagClient.hentGeografiskTilknytning(
                GeografiskTilknytningRequest(fødselsnummer))

        val (behandlendeEnheter) = oppslagClient.hentBehandlendeEnhet(
            BehandlendeEnhetRequest(geografiskTilknytning, diskresjonsKode)
        )

        val behandlendeEnhet = behandlendeEnheter.minBy { it.enhetId } ?: throw RutingException("Missing enhetId")

        jpCounter.labels(behandlendeEnhet.enhetId, diskresjonsKode).inc()
        behov.setBehandleneEnhet(behandlendeEnhet.enhetId)
        return behov
    }
}

fun shouldBeProcessed(behov: Behov): Boolean =
        behov.hasHenvendelsesType() && !behov.hasBehandlendeEnhet()

class RutingException(override val message: String) : RuntimeException(message)
