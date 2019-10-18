package no.nav.dagpenger.journalføring.ruting

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.PacketDeserializer
import no.nav.dagpenger.streams.PacketSerializer
import no.nav.dagpenger.streams.Topic
import org.apache.kafka.common.serialization.Serdes

private val localProperties = ConfigurationMap(
    mapOf(
        "kafka.bootstrap.servers" to "localhost:9092",
        "oidc.sts.issuerurl" to "localhost:8082",
        "application.profile" to Profile.LOCAL.toString(),
        "application.httpPort" to "8080",
        "oppslag.url" to "http://localhost:8081",
        "srvdagpenger.journalforing.ruting.username" to "user",
        "srvdagpenger.journalforing.ruting.password" to "password"
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "kafka.bootstrap.servers" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
        "application.profile" to Profile.DEV.toString(),
        "application.httpPort" to "8080",
        "oidc.sts.issuerurl" to "https://security-token-service.nais.preprod.local",
        "oppslag.url" to "http://dagpenger-oppslag.default.svc.nais.local/api"
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "kafka.bootstrap.servers" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00148.adeo.no:8443,a01apvl00149.adeo.no:8443,a01apvl00150.adeo.no:8443",
        "application.profile" to Profile.PROD.toString(),
        "application.httpPort" to "8080",
        "oidc.sts.issuerurl" to "https://security-token-service.nais.adeo.no",
        "oppslag.url" to "http://dagpenger-oppslag.default.svc.nais.local/api"
    )
)

private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> {
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
    }
}

data class Configuration(
    val kafka: Kafka = Kafka(),
    val application: Application = Application()
) {
    data class Kafka(
        val dagpengerJournalpostTopic: Topic<String, Packet> = Topic(
            "privat-dagpenger-journalpost-mottatt-v1",
            keySerde = Serdes.String(),
            valueSerde = Serdes.serdeFrom(PacketSerializer(), PacketDeserializer())
        ),
        val brokers: String = config()[Key("kafka.bootstrap.servers", stringType)]
    )

    data class Application(
        val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
        val user: String = config()[Key("srvdagpenger.journalforing.ruting.username", stringType)],
        val password: String = config()[Key("srvdagpenger.journalforing.ruting.password", stringType)],
        val httpPort: Int = config()[Key("application.httpPort", intType)],
        val oidcStsUrl: String = config()[Key("oidc.sts.issuerurl", stringType)],
        val oppslagUrl: String = config()[Key("oppslag.url", stringType)]
    )
}

enum class Profile {
    LOCAL, DEV, PROD
}