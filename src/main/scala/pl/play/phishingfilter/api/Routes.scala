package pl.play.phishingfilter.api

import cats.effect.IO
import io.circe.Json
import io.circe.syntax._
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.http4s.circe._
import org.http4s.dsl.io._
import pl.play.phishingfilter.model.{DecisionReasons, Decisions, SmsRequest, SmsResponse}
import pl.play.phishingfilter.service.SmsService

class Routes(smsService: SmsService) {
  import JsonCodecs._

  private implicit val smsRequestEntityDecoder: EntityDecoder[IO, SmsRequest] = jsonOf[IO, SmsRequest]
  private implicit val smsResponseEntityEncoder: EntityEncoder[IO, SmsResponse] = jsonEncoderOf[IO, SmsResponse]

  val httpRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "sms" =>
      req
        .attemptAs[SmsRequest]
        .value
        .flatMap {
          case Right(smsRequest) =>
            smsService.process(smsRequest).flatMap(Ok(_))
          case Left(_) =>
            BadRequest(SmsResponse(Decisions.Rejected, DecisionReasons.InvalidRequest))
        }
  }
}
