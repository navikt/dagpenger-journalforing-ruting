package no.nav.dagpenger.journalføring.ruting

import no.nav.dagpenger.events.avro.Annet
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.avro.Ettersending
import no.nav.dagpenger.events.avro.Søknad
import org.junit.Test
import kotlin.test.assertFalse

class JournalføringRutingTest {

    @Test
    fun `Process behov with henvendelsesType and without behandledeEnhet`() {

        val behovNy = Behov
            .newBuilder()
            .setBehovId("000")
            .setHenvendelsesType(Søknad())
            .build()

        val behovEttersending = Behov
            .newBuilder()
            .setBehovId("000")
            .setHenvendelsesType(Ettersending())
            .build()

        val behovAnnet = Behov
            .newBuilder()
            .setBehovId("000")
            .setHenvendelsesType(Annet())
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
            .setHenvendelsesType(Søknad())
            .setBehandleneEnhet("beh")
            .build()

        assertFalse(shouldBeProcessed(behovDuplicate))
    }
}