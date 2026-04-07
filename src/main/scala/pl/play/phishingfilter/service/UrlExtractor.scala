package pl.play.phishingfilter.service

import java.net.URI
import scala.util.Try

object UrlExtractor {
  private val UrlRegex = """https?://[^\s]+""".r

  def extract(text: String): List[String] =
    UrlRegex.findAllIn(text).toList

  def normalize(url: String): Option[String] =
    Try(new URI(url.trim)).toOption.flatMap { uri =>
      //if (uri.getScheme == null) None
      //else Some(uri.getScheme)
      val scheme = Option(uri.getScheme).map(_.toLowerCase)
      val host   = Option(uri.getHost).map(_.toLowerCase)
      val path   = Option(uri.getRawPath).filter(_.nonEmpty).getOrElse("")
      val query  = Option(uri.getRawQuery).filter(_.nonEmpty).map(q => s"?$q").getOrElse("")

      for {
        s <- scheme
        h <- host
      } yield s"$s://$h$path$query".stripSuffix("/")
    }

   def extractAndNormalize(text: String): List[String] =
    extract(text).flatMap(normalize).distinct
}
