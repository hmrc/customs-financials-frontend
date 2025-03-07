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

package repositories

import com.mongodb.client.model.Indexes.ascending
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{IndexModel, IndexOptions, ReplaceOptions}
import play.api.Configuration
import play.api.libs.functional.syntax.{unlift, *}
import play.api.libs.json.{Format, OWrites, Reads, __}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.*

import java.time.{Instant, ZoneOffset}
import org.mongodb.scala.{SingleObservableFuture, ToSingleObservablePublisher}

@Singleton
class DefaultQueryCacheRepository @Inject() (mongoComponent: MongoComponent, config: Configuration)(implicit
  executionContext: ExecutionContext
) extends PlayMongoRepository[QueryStringMongo](
      collectionName = "customs-financials-query-cache",
      mongoComponent = mongoComponent,
      domainFormat = QueryStringMongo.format,
      indexes = Seq(
        IndexModel(
          ascending("lastUpdated"),
          IndexOptions()
            .name("customs-financials-query-cache-index")
            .unique(false)
            .expireAfter(config.get[Long]("mongodb.timeToLiveInSeconds"), TimeUnit.SECONDS)
        )
      )
    )
    with QueryCacheRepository {

  override def getQuery(sessionId: String): Future[Option[String]] =
    for {
      record <- collection.find(equal("_id", sessionId)).toSingle().toFutureOption()
      result  = record.map(_.query)
    } yield result

  override def clearAndInsertQuery(sessionId: String, query: String): Future[Boolean] = {
    val record = QueryStringMongo(query)
    for {
      writeSuccessful <- collection
                           .replaceOne(
                             equal("_id", sessionId),
                             record,
                             ReplaceOptions().upsert(true)
                           )
                           .toFuture()
                           .map(_.wasAcknowledged())
    } yield writeSuccessful
  }

  override def removeQuery(sessionId: String): Future[Boolean] =
    collection
      .deleteOne(equal("_id", sessionId))
      .toFuture()
      .map(_.wasAcknowledged())
}

trait QueryCacheRepository {

  def getQuery(sessionId: String): Future[Option[String]]

  def clearAndInsertQuery(sessionId: String, query: String): Future[Boolean]

  def removeQuery(sessionId: String): Future[Boolean]
}

case class QueryStringMongo(query: String, lastUpdated: LocalDateTime = LocalDateTime.now)

object QueryStringMongo {

  implicit lazy val writes: OWrites[QueryStringMongo] =
    (
      (__ \ "query").write[String] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.localDateTimeWrites)
    )(query => Tuple.fromProductTyped(query))

  implicit lazy val reads: Reads[QueryStringMongo] =
    (
      (__ \ "query").read[String] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.localDateTimeReads)
    )(QueryStringMongo.apply _)

  trait MongoJavatimeFormats {
    outer =>

    final val localDateTimeReads: Reads[LocalDateTime] =
      Reads
        .at[String](__ \ "$date" \ "$numberLong")
        .map(dateTime => Instant.ofEpochMilli(dateTime.toLong).atZone(ZoneOffset.UTC).toLocalDateTime)

    final val localDateTimeWrites: Writes[LocalDateTime] =
      Writes
        .at[String](__ \ "$date" \ "$numberLong")
        .contramap(_.toInstant(ZoneOffset.UTC).toEpochMilli.toString)
  }

  object MongoJavatimeFormats extends MongoJavatimeFormats

  implicit val format: Format[QueryStringMongo] = Format(reads, writes)
}
