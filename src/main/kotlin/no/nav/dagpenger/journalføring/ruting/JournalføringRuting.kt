package no.nav.dagpenger.journalføring.ruting

import mu.KotlinLogging
import no.nav.dagpenger.events.avro.Behov
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

class JournalføringRuting(val env: Environment, private val oppslagHttpClient: OppslagHttpClient) : Service() {
    override val SERVICE_APP_ID = "journalføring-ruting" // NB: also used as group.id for the consumer group - do not change!

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val env = Environment()
            val service = JournalføringRuting(env, OppslagHttpClient(env.dagpengerOppslagUrl))
            service.start()
        }
    }

    override fun setupStreams(): KafkaStreams {
        println(SERVICE_APP_ID)
        val builder = StreamsBuilder()

        val inngåendeJournalposter = builder.consumeTopic(INNGÅENDE_JOURNALPOST)

        inngåendeJournalposter
                .peek { key, value -> LOGGER.info("Processing ${value.javaClass} with key $key") }
                .filter { _, behov -> behov.getJournalpost().getJournalpostType() != null }
                .filter { _, behov -> behov.getJournalpost().getBehandleneEnhet() == null }
                .mapValues(this::addBehandleneEnhet)
                .peek { key, value -> LOGGER.info("Producing ${value.javaClass} with key $key") }
                .toTopic(INNGÅENDE_JOURNALPOST)

        return KafkaStreams(builder.build(), this.getConfig())
    }

    override fun getConfig(): Properties {
        return streamConfig(appId = SERVICE_APP_ID, bootStapServerUrl = env.bootstrapServersUrl, credential = KafkaCredential(env.username, env.password))
    }

    private fun addBehandleneEnhet(behov: Behov): Behov {
        val fødselsnummer = behov.getJournalpost().getSøker().getIdentifikator()
        val (geografiskTilknytning, diskresjonsKode) = oppslagHttpClient.hentGeografiskTilknytning(fødselsnummer)
        val behandlendeEnhet = oppslagHttpClient.hentBehandlendeEnhet(
                BehandlendeEnhetRequest(geografiskTilknytning, diskresjonsKode))

        behov.getJournalpost().setBehandleneEnhet(behandlendeEnhet)
        return behov
    }
}