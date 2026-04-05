package pl.play.phishingfilter.service

import cats.effect.IO
import java.time.Instant
import pl.play.phishingfilter.client.WebRiskClient
import pl.play.phishingfilter.config.AppConfig
import pl.play.phishingfilter.model._
import pl.play.phishingfilter.repository.{SubscriptionRepository, UrlVerdictCacheRepository}

final class SmsService(
    config: AppConfig,
    subscriptionRepository: SubscriptionRepository,
    urlVerdictCacheRepository: UrlVerdictCacheRepository,
    webRiskClient: WebRiskClient
) {

  def process(sms: SmsRequest): IO[SmsResponse] =
    if (isStartCommand(sms)) handleCommand(sms.sender, enabled = true)
    else if (isStopCommand(sms)) handleCommand(sms.sender, enabled = false)
    else filterRegularSms(sms)

  private def isStartCommand(sms: SmsRequest): Boolean =
    sms.recipient == config.subscriptionNumber &&
      sms.message.trim.equalsIgnoreCase("START")

  private def isStopCommand(sms: SmsRequest): Boolean =
    sms.recipient == config.subscriptionNumber &&
      sms.message.trim.equalsIgnoreCase("STOP")

  private def handleCommand(sender: String, enabled: Boolean): IO[SmsResponse] = {
    val now = Instant.now()
    val reason =
      if (enabled) DecisionReasons.SubscriptionStarted
      else DecisionReasons.SubscriptionStopped

    subscriptionRepository
      .setEnabled(sender, enabled, now)
      .as(SmsResponse(Decisions.Accepted, reason))
  }

  private def filterRegularSms(sms: SmsRequest): IO[SmsResponse] =
    subscriptionRepository.isEnabled(sms.recipient).flatMap { protectionEnabled =>
      if (!protectionEnabled) {
        IO.pure(SmsResponse(Decisions.Accepted, DecisionReasons.UserNotSubscribed))
      } else {
        val normalizedUrls = UrlExtractor.extractAndNormalize(sms.message).distinct
        if (normalizedUrls.isEmpty) IO.pure(SmsResponse(Decisions.Accepted, DecisionReasons.NoUrlsFound))
        else {
          evaluateUrls(normalizedUrls).map {
            //   ScanSummary(phishingFound, lookupErrors)
            case ScanSummary(true, _) => SmsResponse(Decisions.Rejected, DecisionReasons.PhishingUrlDetected)
            case ScanSummary(false, true) => SmsResponse(Decisions.Accepted, DecisionReasons.PhishingCheckUnavailable)
            case ScanSummary(false, false) => SmsResponse(Decisions.Accepted, DecisionReasons.NoPhishingFound)
          }
        }
      }
    }

  private def evaluateUrls(urls: List[String]): IO[ScanSummary] = {
    def loop(remaining: List[String], acc: ScanSummary): IO[ScanSummary] =
      remaining match {
        case Nil => IO.pure(acc)
        case _ if acc.phishingFound => IO.pure(acc)
        case url :: tail => evaluateSingleUrl(url).flatMap(result => loop(tail, acc.combine(result)))
      }

    loop(urls, ScanSummary.empty)
  }

  private def evaluateSingleUrl(normalizedUrl: String): IO[ScanSummary] = {
    val now = Instant.now()

    urlVerdictCacheRepository.findValid(normalizedUrl, now).flatMap {
      case Some(cached) => IO.pure(ScanSummary(phishingFound = cached.verdict == UrlVerdict.Phishing, lookupErrors = false))
      case None =>
        webRiskClient.evaluate(normalizedUrl).attempt.flatMap {
          case Right(verdict) =>
            val expiresAt = now.plusSeconds(config.phishing.cacheTTLHours * 3600)
            urlVerdictCacheRepository
              .upsert(normalizedUrl, verdict, now, expiresAt)
              .as(ScanSummary(phishingFound = verdict == UrlVerdict.Phishing, lookupErrors = false))
          case Left(error) =>
            IO.println(s"WebRisk lookup failed for [$normalizedUrl]: ${error.getMessage}")
              .as(ScanSummary(phishingFound = false, lookupErrors = true))
        }
    }
  }

  private final case class ScanSummary(phishingFound: Boolean, lookupErrors: Boolean) {
    def combine(other: ScanSummary): ScanSummary =
      ScanSummary(
        phishingFound = phishingFound || other.phishingFound,
        lookupErrors = lookupErrors || other.lookupErrors
      )
  }

  private object ScanSummary {
    val empty: ScanSummary =
      ScanSummary(phishingFound = false, lookupErrors = false)
  }
}
