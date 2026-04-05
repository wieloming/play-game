package pl.play.phishingfilter.db

import cats.effect.IO
import doobie.Transactor
import pl.play.phishingfilter.config.DatabaseConfig

object Database {
  def transactor(config: DatabaseConfig): Transactor[IO] =
    Transactor.fromDriverManager[IO](
      driver = config.driver,
      url = config.url,
      user = config.user,
      password = config.password,
      logHandler = None
    )
}
