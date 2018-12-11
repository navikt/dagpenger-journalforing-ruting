package no.nav.dagpenger.journalføring.ruting

interface OppslagClient {
    fun hentGeografiskTilknytning(request: GeografiskTilknytningRequest): GeografiskTilknytningResponse
    fun hentBehandlendeEnhet(request: BehandlendeEnhetRequest): BehandlendeEnhetResponse
}
