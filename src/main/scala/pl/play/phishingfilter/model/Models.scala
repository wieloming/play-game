package pl.play.phishingfilter.model

import java.time.Instant

case class SmsRequest(
    sender: String,
    recipient: String,
    message: String
)

case class SmsResponse(
    decision: String,
    reason: String
)

sealed trait UrlVerdict {
  def value: String
}

object UrlVerdict {
  case object Safe extends UrlVerdict {
    override val value: String = "SAFE"
  }

  case object Phishing extends UrlVerdict {
    override val value: String = "PHISHING"
  }

  def fromString(value: String): UrlVerdict =
    value.toUpperCase match {
      case "PHISHING" => Phishing
      case _          => Safe
    }
}

case class CachedVerdict(
    normalizedUrl: String,
    verdict: UrlVerdict,
    checkedAt: Instant,
    expiresAt: Instant
)

object DecisionReasons {
  val SubscriptionStarted       = "SUBSCRIPTION_STARTED"
  val SubscriptionStopped       = "SUBSCRIPTION_STOPPED"
  val UserNotSubscribed         = "USER_NOT_SUBSCRIBED"
  val NoUrlsFound               = "NO_URLS_FOUND"
  val NoPhishingFound           = "NO_PHISHING_FOUND"
  val PhishingUrlDetected       = "PHISHING_URL_DETECTED"
  val PhishingCheckUnavailable  = "PHISHING_CHECK_UNAVAILABLE"
  val InvalidRequest            = "INVALID_REQUEST"
}

object Decisions {
  val Accepted = "ACCEPTED"
  val Rejected = "REJECTED"
}
