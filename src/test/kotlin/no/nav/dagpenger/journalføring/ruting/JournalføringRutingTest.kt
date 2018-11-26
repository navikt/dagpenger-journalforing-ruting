package no.nav.dagpenger.journalføring.ruting

import no.nav.dagpenger.events.avro.Annet
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.avro.Ettersending
import no.nav.dagpenger.events.avro.HenvendelsesType
import no.nav.dagpenger.events.avro.Søknad
import org.junit.Test
import kotlin.test.assertFalse

class JournalføringRutingTest {

    @Test
    fun `Process behov with henvendelsesType and without behandledeEnhet`() {

        val behovNy = Behov
                .newBuilder()
                .setBehovId("000")
                .setHenvendelsesType(HenvendelsesType.newBuilder().setSøknad(Søknad()).build())
                .build()

        val behovEttersending = Behov
                .newBuilder()
                .setBehovId("000")
                .setHenvendelsesType(HenvendelsesType.newBuilder().setEttersending(Ettersending()).build())
                .build()

        val behovAnnet = Behov
                .newBuilder()
                .setBehovId("000")
                .setHenvendelsesType(HenvendelsesType.newBuilder().setAnnet(Annet()).build())
                .build()

        assert(shouldBeProcessed(behovNy))
        assert(shouldBeProcessed(behovEttersending))
        assert(shouldBeProcessed(behovAnnet))
    }

    @Test
    fun `Do not reprocess behov `() {
        val behovDuplicate = Behov
                .newBuilder()
                .setBehovId("000")
                .setHenvendelsesType(HenvendelsesType.newBuilder().setSøknad(Søknad()).build())
                .setBehandleneEnhet("beh")
                .build()

        assertFalse(shouldBeProcessed(behovDuplicate))
    }
}