package no.nav.dagpenger.journalføring.ruting

import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import mu.KotlinLogging
import no.nav.dagpenger.http.retryFuelHttp

private val LOGGER = KotlinLogging.logger {}

class OppslagHttpClient(private val oppslagUrl: String) : OppslagClient {

    override fun hentGeografiskTilknytning(request: GeografiskTilknytningRequest): GeografiskTilknytningResponse {
        val url = "$oppslagUrl/person/geografisk-tilknytning"
        val json = Gson().toJson(request).toString()
        val (_, response, result) = retryFuelHttp {
                with(url.httpPost()
                        .header(mapOf("Content-Type" to "application/json"))
                        .body(json)) {
                responseObject<GeografiskTilknytningResponse>()
            }
        }
        return when (result) {
                is Result.Failure -> throw OppslagException(
                        response.statusCode, response.responseMessage, result.getException())
                is Result.Success -> result.get()
        }
    }

    override fun hentBehandlendeEnhet(request: BehandlendeEnhetRequest): BehandlendeEnhetResponse {
        val url = "$oppslagUrl/arbeidsfordeling/behandlende-enhet"
        val json = Gson().toJson(request).toString()
        val (_, response, result) = retryFuelHttp {
                with(url.httpPost()
                        .header(mapOf("Content-Type" to "application/json"))
                        .body(json)) {
                responseObject<BehandlendeEnhetResponse>()
            }
        }
        return when (result) {
            is Result.Failure -> throw OppslagException(
                    response.statusCode, response.responseMessage, result.getException())
            is Result.Success -> result.get()
        }
    }
}

data class GeografiskTilknytningRequest(val fødselsnummer: String)

data class GeografiskTilknytningResponse(
    val geografiskTilknytning: String,
    val diskresjonskode: String?
)

data class BehandlendeEnhetRequest(
    val geografiskTilknytning: String,
    val diskresjonskode: String?,
    val tema: String = "DAG"
)

data class BehandlendeEnhet(
    var enhetId: String,
    var enhetNavn: String
)

data class BehandlendeEnhetResponse(val behandlendeEnheter: List<BehandlendeEnhet>)

class OppslagException(val statusCode: Int, override val message: String, override val cause: Throwable) : RuntimeException(message, cause)