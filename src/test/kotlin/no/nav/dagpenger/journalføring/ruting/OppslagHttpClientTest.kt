package no.nav.dagpenger.journalføring.ruting

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class OppslagHttpClientTest {

    @Rule
    @JvmField
    var wireMockRule = WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort())

    @Test
    fun `hent geografisk tilknytning`() {

        val fnr = "12345678912"

        stubFor(
            WireMock.post(WireMock.urlEqualTo("/person/geografisk-tilknytning"))
                .withRequestBody(EqualToPattern(
                        """
                            {"fødselsnummer":"12345678912"}
                        """.trimIndent()
                ))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "geografiskTilknytning": "BLA",
                                "diskresjonskode" : "1"
                            }
                        """.trimIndent()
                        )
                )
        )

        val geografiskTilknytningResponse =
                OppslagHttpClient(wireMockRule.url(""))
                        .hentGeografiskTilknytning(GeografiskTilknytningRequest(fnr))
        assertEquals("BLA", geografiskTilknytningResponse.geografiskTilknytning)
        assertEquals("1", geografiskTilknytningResponse.diskresjonskode)
    }

    @Test(expected = OppslagException::class)
    fun `hent geografisk tilknytning feiler `() {

        val fnr = "12345678912"

        stubFor(
            WireMock.post(WireMock.urlEqualTo("/person/geografisk-tilknytning"))
                .withRequestBody(EqualToPattern(
                        """
                            {"fødselsnummer":"12345678912"}
                        """.trimIndent()))
                .willReturn(
                    WireMock.serverError()
                )
        )

        OppslagHttpClient(wireMockRule.url("")).hentGeografiskTilknytning(GeografiskTilknytningRequest(fnr))
    }

    @Test
    fun `hent behandlende enhet`() {
        stubFor(
            WireMock.post(WireMock.urlEqualTo("/arbeidsfordeling/behandlende-enhet"))
                .withRequestBody(
                    EqualToJsonPattern(
                        """
                     {
                        "geografiskTilknytning": "BLA",
                        "diskresjonskode" : "1"
                     }
                """.trimIndent(), true, true
                    )
                )
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                """
                            {
                                "behandlendeEnheter": [
                                    {
                                        "enhetId": "0118",
                                        "enhetNavn": "Test"
                                    }
                                ]
                            }
                            """.trimIndent())
                )
        )

        val request = BehandlendeEnhetRequest("BLA", "1")

        val response = OppslagHttpClient(wireMockRule.url("")).hentBehandlendeEnhet(request)
        assertEquals("0118", response.behandlendeEnheter[0].enhetId)
    }

    @Test(expected = OppslagException::class)
    fun `hent behandlende enhet feiler`() {

        stubFor(
            WireMock.post(WireMock.urlEqualTo("/arbeidsfordeling/behandlende-enhet"))
                .withRequestBody(
                    EqualToJsonPattern(
                        """
                     {
                        "geografiskTilknytning": "BLA",
                        "diskresjonskode" : "1"
                     }
                """.trimIndent(), true, true
                    )
                )
                .willReturn(
                    WireMock.serverError()
                )
        )

        val request = BehandlendeEnhetRequest("BLA", "1")

        OppslagHttpClient(wireMockRule.url("")).hentBehandlendeEnhet(request)
    }
}