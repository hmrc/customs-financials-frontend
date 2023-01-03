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

package viewmodels

/**
  * This Paginated trait should be mixed into any ViewModels that require pagination.
  * Views that require pagination can then simply include
  *   '@pager(viewModel)'
  * to render the paginator controls, and use viewModel.visibleItems to reference the items visible on the current page.
  *
  * It will always display FixedWidth number of links plus Previous and Next buttons if they are applicable
  * -if you are on the 1st page, you will see pages: 1,2,3,4,5,Next   (here you can jump 5 pages ahead)
  * -if you are on the 6th page, you will see pages: Prev,4,5,6,7,8,Next  (here you can jump just 2 pages back or forward)
  * -if you are on the last page (e.g. 9) you will see: Prev,5,6,7,8,9  (here you can jump 5 pages back)
  *
  * @param T the type of items to be paginated
  * @param items the sequence of items to be paginated
  * @param itemsPerPage the number of items to display per page
  * @param requestedPage the page number requested by the user (via query param)
  * @param urlForPage a function which takes a target page number and returns a URL to navigate to that page
  * @param itemsDescription a description of what the items are - eg. accounts, transactions, etc.
  */

trait Paginated[T] {

  val allItems: Seq[T]
  val itemsPerPage: Int
  val requestedPage: Int
  val urlForPage: Int => String
  val itemsDescription: String

  private val FirstPage = 1
  private val FixedWidth = 5
  private val lookAhead = FixedWidth / 2

  private lazy val totalNumberOfItems: Int = allItems.length

  private lazy val lastPage = totalNumberOfItems % itemsPerPage match {
    case 0 => totalNumberOfItems / itemsPerPage
    case _ => totalNumberOfItems / itemsPerPage + 1  //We need an additional page for the remainder items
  }

  lazy val currentPage: Int = requestedPage.max(FirstPage).min(lastPage)

  lazy val isFirstPage: Boolean = currentPage == FirstPage
  lazy val isLastPage: Boolean = currentPage == lastPage

  lazy val dataFitsOnOnePage: Boolean = totalNumberOfItems <= itemsPerPage

  lazy val firstItemOnPage: Int = (currentPage - 1) * itemsPerPage
  lazy val lastItemOnPage: Int = totalNumberOfItems.min(currentPage * itemsPerPage)

  lazy val visibleItems: Seq[T] = allItems.slice(firstItemOnPage, lastItemOnPage)

  lazy val pageRange: IndexedSeq[Int] = {
    val range = if (currentPage <= lookAhead) {
      FirstPage to FixedWidth
    } else if (currentPage + lookAhead >= lastPage) {
      lastPage + 1 - FixedWidth to lastPage
    } else {
      currentPage - lookAhead to currentPage + lookAhead
    }
    range.filter(n => n >= FirstPage && n <= lastPage)
  }

}
