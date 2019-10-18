package no.nav.dagpenger.journalføring.ruting

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.PacketDeserializer
import no.nav.dagpenger.streams.PacketSerializer
import no.nav.dagpenger.streams.Topic
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Test
import java.util.Properties

internal class JournalføringRutingTopologyTest {

    val dagpengerJournalpostTopic: Topic<String, Packet> = Topic(
        "privat-dagpenger-journalpost-mottatt-v1",
        keySerde = Serdes.String(),
        valueSerde = Serdes.serdeFrom(PacketSerializer(), PacketDeserializer())
    )

    val factory = ConsumerRecordFactory<String, Packet>(
        dagpengerJournalpostTopic.name,
        dagpengerJournalpostTopic.keySerde.serializer(),
        dagpengerJournalpostTopic.valueSerde.serializer()
    )

    val properties = Properties().apply {
        this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
        this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
    }

    @Test
    fun `skal ikke legge til behandlende enhet hvis pakken er tom`() {
        val testService = JournalføringRuting(Configuration(), mockk())

        TopologyTestDriver(testService.buildTopology(), properties).use { topologyTestDriver ->
            val inputRecord = factory.create(Packet())
            topologyTestDriver.pipeInput(inputRecord)

            val ut = topologyTestDriver.readOutput(
                dagpengerJournalpostTopic.name,
                dagpengerJournalpostTopic.keySerde.deserializer(),
                dagpengerJournalpostTopic.valueSerde.deserializer()
            )

            ut shouldBe null
        }
    }

    @Test
    fun `skal legge til behandlende enhet hvis pakken har informasjon om bruker`() {
        val oppslagsklient = mockk<OppslagHttpClient>().also {
            every { it.hentGeografiskTilknytning(any()) } returns GeografiskTilknytningResponse(
                geografiskTilknytning = "blabla",
                diskresjonskode = "6"
            )
            every { it.hentBehandlendeEnhet(any()) } returns BehandlendeEnhetResponse(
                behandlendeEnheter = listOf(BehandlendeEnhet(enhetId = "123", enhetNavn = "NAV sannergata"))
            )
        }
        val testService = JournalføringRuting(Configuration(), oppslagsklient)

        TopologyTestDriver(testService.buildTopology(), properties).use { topologyTestDriver ->
            val inputRecord = factory.create(Packet().apply {
                this.putValue("aktørId", "1234")
            })
            topologyTestDriver.pipeInput(inputRecord)

            val ut = topologyTestDriver.readOutput(
                dagpengerJournalpostTopic.name,
                dagpengerJournalpostTopic.keySerde.deserializer(),
                dagpengerJournalpostTopic.valueSerde.deserializer()
            )

            ut shouldNotBe null
            ut.value().getStringValue("behandlendeEnhet") shouldBe "123"
        }
    }
}