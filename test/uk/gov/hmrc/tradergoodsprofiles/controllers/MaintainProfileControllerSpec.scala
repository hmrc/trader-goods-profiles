package uk.gov.hmrc.tradergoodsprofiles.controllers

import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.MockitoSugar.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.InternalServerError
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.ValidateHeaderAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.{FakeSuccessAuthAction, FakeUnauthorizedAuthAction}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.UpdateProfileRequest
import uk.gov.hmrc.tradergoodsprofiles.models.responses.UpdateProfileResponse
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}

import scala.concurrent.{ExecutionContext, Future}

class MaintainProfileControllerSpec extends PlaySpec with AuthTestSupport with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val requestHeaders = Seq(
    "Accept"       -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    "X-Client-ID"  -> "some client ID"
  )

  private val eori          = "GB123456789012"
  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  private val uuidService   = mock[UuidService]
  private val routerService = mock[RouterService]

  private val updateProfileRequest = UpdateProfileRequest(
    actorId = "GB987654321098",
    ukimsNumber = "XIUKIM47699357400020231115081800",
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345")
  )

  private val updateProfileResponse = UpdateProfileResponse(
    eori,
    "GB987654321098",
    "XIUKIM47699357400020231115081800",
    "RMS-GB-123456",
    "6 S12345"
  )

  private val sut = new MaintainProfileController(
    new FakeSuccessAuthAction(),
    new ValidateHeaderAction(uuidService),
    uuidService,
    routerService,
    stubControllerComponents()
  )

  private val unauthorizedSut = new MaintainProfileController(
    new FakeUnauthorizedAuthAction(correlationId),
    new ValidateHeaderAction(uuidService),
    uuidService,
    routerService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(uuidService, routerService)
    when(uuidService.uuid).thenReturn(correlationId)
    when(routerService.updateProfile(mockEq(eori), any[UpdateProfileRequest])(any()))
      .thenReturn(Future.successful(Right(updateProfileResponse)))
  }

  "MaintainProfileController" should {

    "return 200 OK when the profile update is successful" in {
      val request: FakeRequest[JsValue] = FakeRequest()
        .withHeaders(requestHeaders: _*)
        .withBody(Json.toJson(updateProfileRequest))

      val result = sut.updateProfile(eori)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(updateProfileResponse)
    }

    "return 400 Bad Request when the JSON body is invalid" in {
      val invalidJson = Json.parse("""{"invalid": "json"}""")

      val request = FakeRequest()
        .withHeaders(requestHeaders: _*)
        .withBody(invalidJson)

      val result = sut.updateProfile(eori)(request)

      status(result) mustBe BAD_REQUEST
      //TODO: Assert actual error response
    }

    "return 401 Unauthorized when the user is not authorized" in {
      val expectedJson = Json.obj(
        "timestamp" -> correlationId,
        "code"      -> "UNAUTHORIZED",
        "message"   -> "Unauthorized"
      )

      val request = FakeRequest()
        .withHeaders(requestHeaders: _*)
        .withBody(Json.toJson(updateProfileRequest))

      val result = unauthorizedSut.updateProfile(eori)(request)

      status(result) mustBe UNAUTHORIZED
      val jsonResponse = contentAsJson(result)
      jsonResponse mustBe expectedJson
    }

    "return 500 Internal Server Error when the service layer fails" in {
      withAuthorizedTrader()

      val expectedJson = Json.obj(
        "timestamp" -> correlationId,
        "code"      -> "INTERNAL_SERVER_ERROR",
        "message"   -> "Internal Server Error"
      )

      when(routerService.updateProfile(any, any)(any))
        .thenReturn(Future.successful(Left(InternalServerError(expectedJson))))

      val request = FakeRequest()
        .withHeaders(requestHeaders: _*)
        .withBody(Json.toJson(updateProfileRequest))

      val result = sut.updateProfile(eori)(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      val jsonResponse = contentAsJson(result)
      jsonResponse mustBe expectedJson
    }
  }
}
