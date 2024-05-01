package uk.gov.hmrc.tradergoodsprofiles.services

import cats.data.EitherT
import com.google.inject.{ImplementedBy, Inject}
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, CONFLICT}
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tradergoodsprofiles.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofiles.models.errors.PresentationError.StandardError
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{RouterError}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

@ImplementedBy(classOf[RouterServiceImpl])
trait RouterService {

  def send(eori: String, recordId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, RouterError, "ViaTGPRouter"]
}
class RouterServiceImpl @Inject() (routerConnector: RouterConnector) extends RouterService with Logging {

  override def send(eori: String, recordId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, RouterError, "ViaTGPRouter"] =
    EitherT(
      routerConnector
        .get(eori, recordId)
        .map(result => Right(result))
        .recover {
          case UpstreamErrorResponse(message, BAD_REQUEST, _, _) => Left(determineError(message))
          case NonFatal(e)                                       =>
            logger.error(s"Unable to send to EIS : ${e.getMessage}", e)
            Left(RouterError.UnexpectedError(thr = Some(e)))
        }
    )

  private def determineError(message: String): RouterError =
    Try(Json.parse(message))
      .map(_.validate[StandardError])
      .map {
        case JsSuccess(value: StandardError, _) => RouterError.GetFailedTGPError(value.message, value.code)
        case _                                  => RouterError.UnexpectedError()
      }
      .getOrElse(RouterError.UnexpectedError())
}
