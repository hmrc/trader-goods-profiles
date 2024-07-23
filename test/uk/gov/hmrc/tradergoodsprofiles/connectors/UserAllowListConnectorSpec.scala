package uk.gov.hmrc.tradergoodsprofiles.connectors

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tradergoodsprofiles.connectors.UserAllowListConnector.UnexpectedResponseException
import uk.gov.hmrc.tradergoodsprofiles.support.BaseConnectorSpec

import scala.concurrent.Future

private class UserAllowListConnectorSpec
    extends BaseConnectorSpec
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach {

  private val baseUrl: String = "http://localhost:12345"
  private val connector       = new UserAllowListConnector(httpClient, appConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig)
    when(appConfig.userAllowListBaseUrl).thenReturn(baseUrl)
    when(httpClient.post(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any[Object])(any, any, any))
      .thenReturn(requestBuilder)
  }

  "check allow list" should {
    "return true when the given EORi is found" in {
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.successful(HttpResponse.apply(200, "")))

      val result = await(connector.check("private-beta", "12345"))

      result mustBe true
    }

    "return false when the given EORi is not found" in {
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.successful(HttpResponse.apply(404, "")))

      val result = await(connector.check("private-beta", "67890"))

      result mustBe false
    }

    "return a UnexpectedResponseException when user-allow-list throws an error" in {
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.successful(HttpResponse.apply(400, "")))

      assertThrows[UnexpectedResponseException](await(connector.check("private-beta", "12345")))
    }

  }
}
