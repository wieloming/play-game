package pl.play.phishingfilter.config

import com.typesafe.config.ConfigFactory

final case class AppConfig(
    http: HttpConfig,
    subscriptionNumber: String,
    phishing: PhishingConfig,
    db: DatabaseConfig
)

final case class HttpConfig(
    host: String,
    port: Int
)

final case class PhishingConfig(
      apiBaseUrl: String,
      apiToken: String,
      cacheTTLHours: Long //Time To Live
)

final case class DatabaseConfig(
    url: String,
    driver: String,
    user: String,
    password: String
)

object AppConfig {
  def load(): AppConfig = {
    val config = ConfigFactory.load()

    AppConfig(
      http = HttpConfig(
        host = config.getString("app.http.host"),
        port = config.getInt("app.http.port")
      ),
      subscriptionNumber = config.getString("app.subscription-number"),
      phishing = PhishingConfig(
        apiBaseUrl = config.getString("app.phishing.api-base-url"),
        apiToken = config.getString("app.phishing.api-token"),
        cacheTTLHours = config.getLong("app.phishing.cache-ttl-hours")
      ),
      db = DatabaseConfig(
        url = config.getString("app.db.url"),
        driver = config.getString("app.db.driver"),
        user = config.getString("app.db.user"),
        password = config.getString("app.db.password")
      )
    )
  }
}
