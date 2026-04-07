package pl.play.phishingfilter.service

import munit.FunSuite

class UrlExtractorSuite extends FunSuite {

  test("extract should return all URLs from message") {
    val text = "Test for me https://example.com and http://test.local/path?x=1"

    val urls = UrlExtractor.extract(text)

    assertEquals(urls, List("https://example.com", "http://test.local/path?x=1"))
  }

  test("normalize should lowercase scheme and host and remove trailing slash") {
    val normalized =
      UrlExtractor.normalize("HTTPS://Example.com/path/")

    assertEquals(normalized, Some("https://example.com/path"))
  }
}
