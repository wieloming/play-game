package pl.play.phishingfilter

import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import pl.play.phishingfilter.api.Routes
import pl.play.phishingfilter.client.HttpWebRiskClient
import pl.play.phishingfilter.config.AppConfig
import pl.play.phishingfilter.db.{Database, Migrations}
import pl.play.phishingfilter.repository.{DoobieSubscriptionRepository, DoobieUrlVerdictCacheRepository}
import pl.play.phishingfilter.service.SmsService

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val config = AppConfig.load()
    val xa = Database.transactor(config.db)

    val host = Host.fromString(config.http.host).getOrElse(Host.fromString("0.0.0.0").get)
    val port = Port.fromInt(config.http.port).getOrElse(Port.fromInt(8080).get)

    for {
      _ <- Migrations.run(xa)
      _ <- EmberClientBuilder.default[IO].build.use { httpClient =>
        val subscriptionRepository = new DoobieSubscriptionRepository(xa)
        val urlVerdictCacheRepository = new DoobieUrlVerdictCacheRepository(xa)
        val webRiskClient = new HttpWebRiskClient(httpClient, config.phishing)
        val smsService = new SmsService(
          config,
          subscriptionRepository,
          urlVerdictCacheRepository,
          webRiskClient
        )
        val routes = new Routes(smsService)

        EmberServerBuilder
          .default[IO]
          .withHost(host)
          .withPort(port)
          .withHttpApp(routes.httpRoutes.orNotFound)
          .build
          .useForever
      }
    } yield ExitCode.Success
  }
}
