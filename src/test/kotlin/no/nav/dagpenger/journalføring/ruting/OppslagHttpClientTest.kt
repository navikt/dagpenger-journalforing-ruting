package no.nav.dagpenger.journalføring.ruting

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

import kotlin.test.assertEquals

class OppslagHttpClientTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    @Test
    fun `hent geografisk tilknytning`() {

        val fnr = "12345678912"

        stubFor(
            WireMock.post(WireMock.urlEqualTo("//person/geografisk-tilknytning"))
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
                OppslagHttpClient(server.url(""))
                        .hentGeografiskTilknytning(GeografiskTilknytningRequest(fnr))
        assertEquals("BLA", geografiskTilknytningResponse.geografiskTilknytning)
        assertEquals("1", geografiskTilknytningResponse.diskresjonskode)
    }

    @Test
    fun `hent geografisk tilknytning feiler `() {

        val fnr = "12345678912"

        stubFor(
            WireMock.post(WireMock.urlEqualTo("//person/geografisk-tilknytning"))
                .withRequestBody(EqualToPattern(
                        """
                            {"fødselsnummer":"12345678912"}
                        """.trimIndent()))
                .willReturn(
                    WireMock.serverError()
                )
        )

        assertThrows<OppslagException> { OppslagHttpClient(server.url("")).hentGeografiskTilknytning(GeografiskTilknytningRequest(fnr)) }
    }

    @Test
    fun `hent behandlende enhet`() {
        stubFor(
            WireMock.post(WireMock.urlEqualTo("//arbeidsfordeling/behandlende-enhet"))
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

        val response = OppslagHttpClient(server.url("")).hentBehandlendeEnhet(request)
        assertEquals("0118", response.behandlendeEnheter[0].enhetId)
    }

    @Test
    fun `hent behandlende enhet feiler`() {

        stubFor(
            WireMock.post(WireMock.urlEqualTo("//arbeidsfordeling/behandlende-enhet"))
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

        assertThrows<OppslagException> { OppslagHttpClient(server.url("")).hentBehandlendeEnhet(request) }
    }
}