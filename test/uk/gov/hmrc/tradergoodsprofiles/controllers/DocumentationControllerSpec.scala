package uk.gov.hmrc.tradergoodsprofiles.controllers

import org.apache.pekko.stream.Materializer
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.test.Helpers.{route, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig

import scala.concurrent.Future

class DocumentationControllerSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with MockitoSugar {

  val mockAppConfig: AppConfig = mock[AppConfig]

  implicit lazy val materializer: Materializer = app.materializer

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[AppConfig].to(mockAppConfig))
    .build()

  override def beforeEach(): Unit =
    reset(mockAppConfig)

  "DocumentationController" when {
    "definition" should {
      "return definition.json file" in {
        val result = doGet("/api/definition", Map.empty)
        status(result) shouldBe OK
        val jsonResult = contentAsJson(result)
        (jsonResult \ "api").asOpt[JsValue] should not be empty
      }
    }

    "specification" should {
      "return application.yaml without withdraw advice endpoint when withdrawAdviceEnabled is false" in {
        when(mockAppConfig.withdrawAdviceEnabled).thenReturn(false)
        val result: Future[Result] = doGet("/api/conf/1.0/application.yaml", Map.empty)

        status(result) shouldBe OK
        val stringResult = Helpers.contentAsString(result)

        stringResult should include("---")
        stringResult should not include "summary: Withdraw your request for advice from HMRC"
      }

      "return application.yaml with withdraw advice endpoint when withdrawAdviceEnabled is true" in {
        when(mockAppConfig.withdrawAdviceEnabled).thenReturn(true)
        val result: Future[Result] = doGet("/api/conf/1.0/application.yaml", Map.empty)

        status(result) shouldBe OK
        val stringResult = Helpers.contentAsString(result)

        stringResult should include("---")
        stringResult should include("summary: Withdraw your request for advice from HMRC")
      }

    }
  }

  def doGet(uri: String, headers: Map[String, String]): Future[Result] = {
    val fakeRequest = FakeRequest(GET, uri).withHeaders(headers.toSeq: _*)
    route(app, fakeRequest).get
  }
}
