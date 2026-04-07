package pl.play.phishingfilter.repository

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import java.time.Instant

trait SubscriptionRepository {
  def setEnabled(sender: String, enabled: Boolean, now: Instant): IO[Unit]
  def isEnabled(sender: String): IO[Boolean]
}

final class DoobieSubscriptionRepository(xa: Transactor[IO]) extends SubscriptionRepository {
  override def setEnabled(sender: String, enabled: Boolean, now: Instant): IO[Unit] =
    //MERGE INTO -> INSERT ... ON CONFLICT (...) DO UPDATE
    sql"""
      MERGE INTO subscriptions (sender, enabled, updated_at)
      KEY(sender)
      VALUES ($sender, $enabled, ${now.toEpochMilli})
    """.update.run.transact(xa).void

  override def isEnabled(sender: String): IO[Boolean] =
    sql"""
      SELECT enabled
      FROM subscriptions
      WHERE sender = $sender
    """.query[Boolean].option.transact(xa).map(_.getOrElse(false))
}
