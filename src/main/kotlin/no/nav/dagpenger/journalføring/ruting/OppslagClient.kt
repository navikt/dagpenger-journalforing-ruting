package no.nav.dagpenger.journalføring.ruting

interface OppslagClient {
    fun hentGeografiskTilknytning(fødselsNummer: String): GeografiskTilknytningResponse
    fun hentBehandlendeEnhet(request: BehandlendeEnhetRequest): String
}
