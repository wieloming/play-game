package pl.play.phishingfilter.db

import cats.effect.IO
import cats.implicits.catsSyntaxApplyOps
import doobie.Transactor
import doobie.implicits._

object Migrations {
  def run(xa: Transactor[IO]): IO[Unit] = {
    val createSubscriptions =
      sql"""
        CREATE TABLE IF NOT EXISTS subscriptions (
          sender VARCHAR PRIMARY KEY,
          enabled BOOLEAN NOT NULL,
          updated_at BIGINT NOT NULL
        )
      """.update.run

    val createUrlVerdictCache =
      sql"""
        CREATE TABLE IF NOT EXISTS url_verdict_cache (
          normalized_url VARCHAR PRIMARY KEY,
          verdict VARCHAR NOT NULL,
          checked_at BIGINT NOT NULL,
          expires_at BIGINT NOT NULL
        )
      """.update.run

    // Run the two table-creation actions sequentially in a single DB transaction.
    // Ignore the intermediate/final result and return IO[Unit].
    (createSubscriptions *> createUrlVerdictCache).transact(xa).void
  }
}
