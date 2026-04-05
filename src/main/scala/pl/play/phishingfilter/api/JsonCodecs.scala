package pl.play.phishingfilter.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import pl.play.phishingfilter.model.{SmsRequest, SmsResponse}

object JsonCodecs {
  implicit val smsRequestDecoder: Decoder[SmsRequest] = deriveDecoder[SmsRequest]
  implicit val smsResponseEncoder: Encoder[SmsResponse] = deriveEncoder[SmsResponse]
}
