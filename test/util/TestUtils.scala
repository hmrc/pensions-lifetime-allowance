/*
 * Copyright 2024 HM Revenue & Customs
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

package util

import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.charset.Charset
import java.util.Random
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.duration._

object TestUtils extends TestUtils

trait TestUtils {

  implicit val defaultTimeout: FiniteDuration = 5.seconds

  def extractAwait[A](future: Future[A]): A = await[A](future)

  def status(of: Result): Int = of.header.status

  def status(of: Future[Result])(implicit timeout: Duration): Int = status(Await.result(of, timeout))

  def jsonBodyOf(result: Result)(implicit mat: Materializer): JsValue =
    Json.parse(bodyOf(result))

  def jsonBodyOf(
      resultF: Future[Result]
  )(implicit mat: Materializer, executionContext: ExecutionContext): Future[JsValue] =
    resultF.map(jsonBodyOf)

  def bodyOf(result: Result)(implicit mat: Materializer): String = {
    val bodyBytes: ByteString = await(result.body.consumeData)
    // We use the default charset to preserve the behaviour of a previous
    // version of this code, which used new String(Array[Byte]).
    // If the fact that the previous version used the default charset was an
    // accident then it may be better to decode in UTF-8 or the charset
    // specified by the result's headers.
    bodyBytes.decodeString(Charset.defaultCharset().name)
  }

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  val rand               = new Random()
  val ninoGenerator      = new Generator(rand)
  def randomNino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  val testNino: String           = randomNino
  val (testNinoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(testNino)

  implicit val hc: HeaderCarrier = HeaderCarrier()
}
