/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import utils.SpecBase
import viewmodels.Paginated

import scala.collection.JavaConverters._

object PaginatorElement {
  def apply(e: Element):List[PaginatorElement] = {
    e.select("li").asScala.map( li => {
      val url = li.select("a").asScala.toList match {
        case Nil => ""
        case head::tail => head.attr("href")
      }
      PaginatorElement(url, li.text())
    }).toList
  }
}

object PaginatorParser {
  def apply(e: Element):PaginatorParser = {
    val description = e.select(".pager-summary").text()
    PaginatorParser(description, PaginatorElement(e))
  }
}

case class PaginatorElement(url:String, linkText:String)
case class PaginatorParser(description:String, links: Seq[PaginatorElement])

case class ExamplePaginatedViewModel(allItems: Seq[Int],
                                     itemsPerPage: Int,
                                     requestedPage: Int,
                                     itemsDescription: String,
                                     urlForPage: Int => String) extends Paginated[Int]

class PaginatedSpec extends SpecBase {

  val app = application().build()
  val messagesApi = app.injector.instanceOf[MessagesApi]

  implicit val messages = messagesApi.preferred(FakeRequest("GET", "/"))

  private def getUriForPage(page: Int) = s"/foo/bar?foo=bar&page=$page"

  private val someDescription = "approved accounts"

  // scalastyle:off magic.number

  def somePaginatedViewModel(numberOfItems: Int, requestedPage: Int): ExamplePaginatedViewModel = {
    val allItems = List.range(1, numberOfItems + 1)
    val itemsPerPage = 25
    ExamplePaginatedViewModel(allItems, itemsPerPage, requestedPage, someDescription, getUriForPage)
  }

  "The paginator" should {
    "Be empty if less than one page of items are available" in {
      val model = somePaginatedViewModel(20, 1)
      val html = views.html.components.pager(model).toString()
      html.trim must be("")
    }

    "In case of 2 pages, and we are on the 1st, display: 1,2,Next" in {
      val model = somePaginatedViewModel(40, 1)
      model.visibleItems mustBe (1 to 25)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val paginatorParser = PaginatorParser(html)
      paginatorParser.description mustBe "Showing 1 – 25 of 40 approved accounts"
      paginatorParser.links.length mustBe 3
      paginatorParser.links(0).linkText mustBe "1"
      paginatorParser.links(0).url mustBe ""
      paginatorParser.links(1).linkText mustBe "2"
      paginatorParser.links(1).url mustBe getUriForPage(2)
      paginatorParser.links(2).linkText mustBe "Next"
      paginatorParser.links(2).url mustBe getUriForPage(2)
    }

    "In case of 2 pages, and we are on the 2nd, display: Prev,1,2" in {
      val model = somePaginatedViewModel(45, 2)
      model.visibleItems mustBe (26 to 45)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val parsed = PaginatorParser(html)
      parsed.description mustBe "Showing 26 – 45 of 45 approved accounts"
      parsed.links.length mustBe 3
      parsed.links(0).linkText mustBe "Prev"
      parsed.links(0).url mustBe getUriForPage(1)
      parsed.links(1).linkText mustBe "1"
      parsed.links(1).url mustBe getUriForPage(1)
      parsed.links(2).linkText mustBe "2"
      parsed.links(2).url mustBe ""
    }

    "In case of 10 pages, and we are on the 1st, display: 1,2,3,4,5,Next" in {
      val model = somePaginatedViewModel(245, 1)
      model.visibleItems mustBe (1 to 25)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val parsed = PaginatorParser(html)
      parsed.description mustBe "Showing 1 – 25 of 245 approved accounts"
      parsed.links.length mustBe 6
      parsed.links(0).linkText mustBe "1"
      parsed.links(0).url mustBe ""
      parsed.links(1).linkText mustBe "2"
      parsed.links(1).url mustBe getUriForPage(2)
      parsed.links(2).linkText mustBe "3"
      parsed.links(2).url mustBe getUriForPage(3)
      parsed.links(3).linkText mustBe "4"
      parsed.links(3).url mustBe getUriForPage(4)
      parsed.links(4).linkText mustBe "5"
      parsed.links(4).url mustBe getUriForPage(5)
      parsed.links(5).linkText mustBe "Next"
      parsed.links(5).url mustBe getUriForPage(2)
    }

    "In case of 10 pages, and we are on the 2nd, display: Prev,1,2,3,4,Next" in {
      val model = somePaginatedViewModel(245, 2)
      model.visibleItems mustBe (26 to 50)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val parsed = PaginatorParser(html)
      parsed.description mustBe "Showing 26 – 50 of 245 approved accounts"
      parsed.links.length mustBe 7
      parsed.links(0).linkText mustBe "Prev"
      parsed.links(0).url mustBe getUriForPage(1)
      parsed.links(1).linkText mustBe "1"
      parsed.links(1).url mustBe getUriForPage(1)
      parsed.links(2).linkText mustBe "2"
      parsed.links(2).url mustBe ""
      parsed.links(3).linkText mustBe "3"
      parsed.links(3).url mustBe getUriForPage(3)
      parsed.links(4).linkText mustBe "4"
      parsed.links(4).url mustBe getUriForPage(4)
      parsed.links(5).linkText mustBe "5"
      parsed.links(5).url mustBe getUriForPage(5)
      parsed.links(6).linkText mustBe "Next"
      parsed.links(6).url mustBe getUriForPage(3)
    }

    "In case of 10 pages, and we are on the 3rd, display: Prev,1,2,3,4,5,Next" in {
      val model = somePaginatedViewModel(245, 3)
      model.visibleItems mustBe (51 to 75)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val parsed = PaginatorParser(html)
      parsed.description mustBe "Showing 51 – 75 of 245 approved accounts"
      parsed.links.length mustBe 7
      parsed.links(0).linkText mustBe "Prev"
      parsed.links(0).url mustBe getUriForPage(2)
      parsed.links(1).linkText mustBe "1"
      parsed.links(1).url mustBe getUriForPage(1)
      parsed.links(2).linkText mustBe "2"
      parsed.links(2).url mustBe getUriForPage(2)
      parsed.links(3).linkText mustBe "3"
      parsed.links(3).url mustBe ""
      parsed.links(4).linkText mustBe "4"
      parsed.links(4).url mustBe getUriForPage(4)
      parsed.links(5).linkText mustBe "5"
      parsed.links(5).url mustBe getUriForPage(5)
      parsed.links(6).linkText mustBe "Next"
      parsed.links(6).url mustBe getUriForPage(4)
    }

    "In case of 10 pages, and we are on the 5th, display: Prev,3,4,5,6,7,Next" in {
      val model = somePaginatedViewModel(245, 5)
      model.visibleItems mustBe (101 to 125)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val parsed = PaginatorParser(html)
      parsed.description mustBe "Showing 101 – 125 of 245 approved accounts"
      parsed.links.length mustBe 7
      parsed.links(0).linkText mustBe "Prev"
      parsed.links(0).url mustBe getUriForPage(4)
      parsed.links(1).linkText mustBe "3"
      parsed.links(1).url mustBe getUriForPage(3)
      parsed.links(2).linkText mustBe "4"
      parsed.links(2).url mustBe getUriForPage(4)
      parsed.links(3).linkText mustBe "5"
      parsed.links(3).url mustBe ""
      parsed.links(4).linkText mustBe "6"
      parsed.links(4).url mustBe getUriForPage(6)
      parsed.links(5).linkText mustBe "7"
      parsed.links(5).url mustBe getUriForPage(7)
      parsed.links(6).linkText mustBe "Next"
      parsed.links(6).url mustBe getUriForPage(6)
    }

    "In case of 10 pages, and we are on the 8th, display: Prev,6,7,8,9,10,Next" in {
      val model = somePaginatedViewModel(245, 8)
      model.visibleItems mustBe (176 to 200)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val parsed = PaginatorParser(html)
      parsed.description mustBe "Showing 176 – 200 of 245 approved accounts"
      parsed.links.length mustBe 7
      parsed.links(0).linkText mustBe "Prev"
      parsed.links(0).url mustBe getUriForPage(7)
      parsed.links(1).linkText mustBe "6"
      parsed.links(1).url mustBe getUriForPage(6)
      parsed.links(2).linkText mustBe "7"
      parsed.links(2).url mustBe getUriForPage(7)
      parsed.links(3).linkText mustBe "8"
      parsed.links(3).url mustBe ""
      parsed.links(4).linkText mustBe "9"
      parsed.links(4).url mustBe getUriForPage(9)
      parsed.links(5).linkText mustBe "10"
      parsed.links(5).url mustBe getUriForPage(10)
      parsed.links(6).linkText mustBe "Next"
      parsed.links(6).url mustBe getUriForPage(9)
    }

    "In case of 10 pages, and we are on the 9th, display: Prev6,7,8,9,10,Next" in {
      val model = somePaginatedViewModel(245, 9)
      model.visibleItems mustBe (201 to 225)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val parsed = PaginatorParser(html)
      parsed.description mustBe "Showing 201 – 225 of 245 approved accounts"
      parsed.links.length mustBe 7
      parsed.links(0).linkText mustBe "Prev"
      parsed.links(0).url mustBe getUriForPage(8)
      parsed.links(1).linkText mustBe "6"
      parsed.links(1).url mustBe getUriForPage(6)
      parsed.links(2).linkText mustBe "7"
      parsed.links(2).url mustBe getUriForPage(7)
      parsed.links(3).linkText mustBe "8"
      parsed.links(3).url mustBe getUriForPage(8)
      parsed.links(4).linkText mustBe "9"
      parsed.links(4).url mustBe ""
      parsed.links(5).linkText mustBe "10"
      parsed.links(5).url mustBe getUriForPage(10)
      parsed.links(6).linkText mustBe "Next"
      parsed.links(6).url mustBe getUriForPage(10)
    }

    "In case of 10 pages, and we are on the 10th, display: Prev,6,7,8,9,10" in {
      val model = somePaginatedViewModel(245, 10)
      model.visibleItems mustBe (226 to 245)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val parsed = PaginatorParser(html)
      parsed.description mustBe "Showing 226 – 245 of 245 approved accounts"
      parsed.links.length mustBe 6
      parsed.links(0).linkText mustBe "Prev"
      parsed.links(0).url mustBe getUriForPage(9)
      parsed.links(1).linkText mustBe "6"
      parsed.links(1).url mustBe getUriForPage(6)
      parsed.links(2).linkText mustBe "7"
      parsed.links(2).url mustBe getUriForPage(7)
      parsed.links(3).linkText mustBe "8"
      parsed.links(3).url mustBe getUriForPage(8)
      parsed.links(4).linkText mustBe "9"
      parsed.links(4).url mustBe getUriForPage(9)
      parsed.links(5).linkText mustBe "10"
      parsed.links(5).url mustBe ""
    }

    "In case of 10 pages and 250 elements, and we are on the 10th, display: Prev,6,7,8,9,10, no 11th page" in {
      val model = somePaginatedViewModel(250, 10)
      model.visibleItems mustBe (226 to 250)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val parsed = PaginatorParser(html)
      parsed.description mustBe "Showing 226 – 250 of 250 approved accounts"
      parsed.links.length mustBe 6
      parsed.links(0).linkText mustBe "Prev"
      parsed.links(0).url mustBe getUriForPage(9)
      parsed.links(1).linkText mustBe "6"
      parsed.links(1).url mustBe getUriForPage(6)
      parsed.links(2).linkText mustBe "7"
      parsed.links(2).url mustBe getUriForPage(7)
      parsed.links(3).linkText mustBe "8"
      parsed.links(3).url mustBe getUriForPage(8)
      parsed.links(4).linkText mustBe "9"
      parsed.links(4).url mustBe getUriForPage(9)
      parsed.links(5).linkText mustBe "10"
      parsed.links(5).url mustBe ""
    }

    "In case of 2 pages, and we erroneously open the 0th page, display: 1,2,Next" in {
      val model = somePaginatedViewModel(40, 0)
      model.visibleItems mustBe (1 to 25)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val parsed = PaginatorParser(html)
      parsed.links.length mustBe 3
      parsed.links(0).linkText mustBe "1"
      parsed.links(0).url mustBe ""
      parsed.links(1).linkText mustBe "2"
      parsed.links(1).url mustBe getUriForPage(2)
      parsed.links(2).linkText mustBe "Next"
      parsed.links(2).url mustBe getUriForPage(2)
    }

    "In case of 2 pages, and we erroneously open the -50th page, display: 1,2,Next" in {
      val model = somePaginatedViewModel(40, -50)
      model.visibleItems mustBe (1 to 25)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val parsed = PaginatorParser(html)
      parsed.links.length mustBe 3
      parsed.links(0).linkText mustBe "1"
      parsed.links(0).url mustBe ""
      parsed.links(1).linkText mustBe "2"
      parsed.links(1).url mustBe getUriForPage(2)
      parsed.links(2).linkText mustBe "Next"
      parsed.links(2).url mustBe getUriForPage(2)
    }

    "In case of 2 pages, and we erroneously open the 50th page, display: Prev,1,2" in {
      val model = somePaginatedViewModel(40, 50)
      model.visibleItems mustBe (26 to 40)
      val html = Jsoup.parse(views.html.components.pager(model).toString())
      val parsed = PaginatorParser(html)
      parsed.links.length mustBe 3
      parsed.links(0).linkText mustBe "Prev"
      parsed.links(0).url mustBe getUriForPage(1)
      parsed.links(1).linkText mustBe "1"
      parsed.links(1).url mustBe getUriForPage(1)
      parsed.links(2).linkText mustBe "2"
      parsed.links(2).url mustBe ""
    }

  }

  // scalastyle:on magic.number

}
