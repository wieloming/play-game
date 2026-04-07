package pl.play.phishingfilter.client

import cats.effect.IO
import io.circe.Json
import io.circe.syntax._

import java.net.URLEncoder
import org.http4s.{EntityDecoder, Method, Request, Uri}
import org.http4s.client.Client
import org.http4s.circe._
import pl.play.phishingfilter.config.PhishingConfig
import pl.play.phishingfilter.model.UrlVerdict

trait WebRiskClient {
  def evaluate(url: String): IO[UrlVerdict]
}

final class HttpWebRiskClient(
    httpClient: Client[IO],
    config: PhishingConfig
) extends WebRiskClient {

  private implicit val jsonEntityDecoder: EntityDecoder[IO, Json] = jsonOf[IO, Json]

  override def evaluate(url: String): IO[UrlVerdict] = {
    val encodedToken = URLEncoder.encode(config.apiToken, "UTF-8")
    val endpoint = s"${config.apiBaseUrl.stripSuffix("/")}/v1eap1:evaluateUri?key=$encodedToken"

    val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(endpoint))
        .withEntity(Json.obj("uri" -> url.asJson))

    httpClient.expect[Json](request).map(parseVerdict)
  }

  private def parseVerdict(json: Json): UrlVerdict = {
    val c = json.hcursor

    val isPhishing =
      c.get[Boolean]("hasThreat").contains(true) ||
        c.downField("threat").focus.exists(_.isObject) ||
        c.get[List[String]]("threatTypes").exists(_.nonEmpty) ||
        c.get[String]("verdict").exists(_.toUpperCase.contains("PHISH"))

    if (isPhishing) UrlVerdict.Phishing else UrlVerdict.Safe
  }
}
