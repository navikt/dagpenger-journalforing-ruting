package no.nav.dagpenger.journalføring.ruting

import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson

class OppslagHttpClient(private val oppslagUrl: String) {

    fun hentGeografiskTilknytning(fødselsNummer: String): GeografiskTilknytningResponse {
        val url = "${oppslagUrl}person/geografisk-tilknytning"
        val (_, response, result) = with(url.httpPost().body(fødselsNummer)) {
            responseObject<GeografiskTilknytningResponse>()
        }
        return when (result) {
            is Result.Failure -> throw OppslagException(
                    response.statusCode, response.responseMessage, result.getException())
            is Result.Success -> result.get()
        }
    }

    fun hentBehandlendeEnhet(request: BehandlendeEnhetRequest): String {
        val url = "${oppslagUrl}arbeidsfordeling/behandlende-enhet"
        val json = Gson().toJson(request).toString()
        val (_, response, result) = with(
                url.httpPost()
                        .header(mapOf("Content-Type" to "application/json"))
                        .body(json)) {
            responseObject<String>()
        }
        return when (result) {
            is Result.Failure -> throw OppslagException(
                    response.statusCode, response.responseMessage, result.getException())
            is Result.Success -> result.get()
        }
    }
}

data class GeografiskTilknytningResponse(
    val geografiskTilknytning: String,
    val diskresjonskode: String?
)

data class BehandlendeEnhetRequest(
    val geografiskTilknytning: String,
    val diskresjonskode: String?
)

class OppslagException(val statusCode: Int, override val message: String, override val cause: Throwable) : RuntimeException(message, cause)
