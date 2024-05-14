package uk.gov.hmrc.tradergoodsprofiles.controllers.actions

import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService

import java.time.Instant
import scala.concurrent.ExecutionContext

class ValidateHeaderActionSpec extends PlaySpec {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "Validate Header Action" should {
    "return None" when {
      "accept header is valid" in {

        val dateTimeService = mock[DateTimeService]
        when(dateTimeService.timestamp).thenReturn(Instant.parse("2024-05-09T12:12:12.5678985Z"))

        val sut = new ValidateHeaderAction(dateTimeService)

        val request = FakeRequest().withHeaders(
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        val result  = await(sut.filter(request))

        result mustBe None
      }
    }
  }
}
