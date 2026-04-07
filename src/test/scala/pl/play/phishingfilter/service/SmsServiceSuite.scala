package pl.play.phishingfilter.service

import cats.effect.IO

import java.time.Instant
import munit.CatsEffectSuite
import pl.play.phishingfilter.client.WebRiskClient
import pl.play.phishingfilter.config._
import pl.play.phishingfilter.model._
import pl.play.phishingfilter.repository._
import pl.play.phishingfilter.service.SmsServiceSuite._

class SmsServiceSuite extends CatsEffectSuite {

  private val config = AppConfig(
    http = HttpConfig("0.0.0.0", 8080),
    subscriptionNumber = "7997",
    phishing = PhishingConfig("http://localhost:9999", "dummy", 24),
    db = DatabaseConfig("jdbc:h2:mem:test", "org.h2.Driver", "sa", "")
  )

  test("START should enable subscription for sender") {
    val subscriptions = new InMemorySubscriptionRepository
    val cache = new InMemoryUrlVerdictCacheRepository
    val client = new StubWebRiskClient(Map.empty)

    val service = new SmsService(config, subscriptions, cache, client)

    val sms = SmsRequest(
      sender = "48700100200",
      recipient = "7997",
      message = "START"
    )

    service.process(sms).flatMap { result =>
      subscriptions.isEnabled("48700100200").map { enabled =>
        assertEquals(result, SmsResponse(Decisions.Accepted, DecisionReasons.SubscriptionStarted))
        assert(enabled)
      }
    }
  }

  test("regular SMS should be rejected when phishing URL is detected") {
    val subscriptions = new InMemorySubscriptionRepository(Map("48700800999" -> true))
    val cache = new InMemoryUrlVerdictCacheRepository
    val client = new StubWebRiskClient(
      Map("https://m-bonk.pl.ng/personal-data" -> UrlVerdict.Phishing)
    )

    val service = new SmsService(config, subscriptions, cache, client)

    val sms = SmsRequest(
      sender = "234100200300",
      recipient = "48700800999",
      message =
        "Dzień dobry. Potwierdź dane na https://m-bonk.pl.ng/personal-data"
    )

    service.process(sms).map { result =>
      assertEquals(result, SmsResponse(Decisions.Rejected, DecisionReasons.PhishingUrlDetected))
    }
  }
}

object SmsServiceSuite {
  private final class InMemorySubscriptionRepository(
      initial: Map[String, Boolean] = Map.empty
  ) extends SubscriptionRepository {
    private var state: Map[String, Boolean] = initial

    override def setEnabled(msisdn: String, enabled: Boolean, now: Instant): IO[Unit] =
      IO { state = state.updated(msisdn, enabled) }

    override def isEnabled(msisdn: String): IO[Boolean] =
      IO.pure(state.getOrElse(msisdn, false))
  }

  private final class InMemoryUrlVerdictCacheRepository extends UrlVerdictCacheRepository {
    private var state: Map[String, CachedVerdict] = Map.empty

    override def findValid(normalizedUrl: String, now: Instant): IO[Option[CachedVerdict]] =
      IO.pure(state.get(normalizedUrl).filter(_.expiresAt.isAfter(now)))

    override def upsert(
        normalizedUrl: String,
        verdict: UrlVerdict,
        checkedAt: Instant,
        expiresAt: Instant
    ): IO[Unit] = IO { state = state.updated(normalizedUrl, CachedVerdict(normalizedUrl, verdict, checkedAt, expiresAt))}
  }

  private final class StubWebRiskClient(results: Map[String, UrlVerdict]) extends WebRiskClient {
    override def evaluate(url: String): IO[UrlVerdict] =
      IO.pure(results.getOrElse(url, UrlVerdict.Safe))
  }
}

