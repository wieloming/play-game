package pl.play.phishingfilter.repository

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import java.time.Instant
import pl.play.phishingfilter.model.{CachedVerdict, UrlVerdict}

trait UrlVerdictCacheRepository {
  def findValid(normalizedUrl: String, now: Instant): IO[Option[CachedVerdict]]
  def upsert(normalizedUrl: String, verdict: UrlVerdict, checkedAt: Instant, expiresAt: Instant): IO[Unit]
}

final class DoobieUrlVerdictCacheRepository(xa: Transactor[IO]) extends UrlVerdictCacheRepository {
  override def findValid(normalizedUrl: String, now: Instant): IO[Option[CachedVerdict]] =
    sql"""
      SELECT normalized_url, verdict, checked_at, expires_at
      FROM url_verdict_cache
      WHERE normalized_url = $normalizedUrl
        AND expires_at > ${now.toEpochMilli}
    """.query[(String, String, Long, Long)]
      .option
      .transact(xa)
      .map(_.map { case (url, verdict, checkedAt, expiresAt) =>
        CachedVerdict(
          normalizedUrl = url,
          verdict = UrlVerdict.fromString(verdict),
          checkedAt = Instant.ofEpochMilli(checkedAt),
          expiresAt = Instant.ofEpochMilli(expiresAt)
        )
      })

  override def upsert(normalizedUrl: String, verdict: UrlVerdict, checkedAt: Instant, expiresAt: Instant): IO[Unit] = {
    //MERGE INTO -> INSERT ... ON CONFLICT (...) DO UPDATE
    sql"""
      MERGE INTO url_verdict_cache (normalized_url, verdict, checked_at, expires_at)
      KEY(normalized_url)
      VALUES ($normalizedUrl, ${verdict.value}, ${checkedAt.toEpochMilli}, ${expiresAt.toEpochMilli})
    """.update.run.transact(xa).void
  }
}
