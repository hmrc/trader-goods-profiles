package uk.gov.hmrc.tradergoodsprofiles.controllers.actions

import cats.data.EitherT
import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc.{ActionFilter, Request, Result}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.InvalidHeaderErrorResponse
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidateHeaderAction @Inject() (datetimeService: DateTimeService)(implicit ec: ExecutionContext)
    extends ActionFilter[Request] {

  override val executionContext: ExecutionContext = ec

  override def filter[A](request: Request[A]): Future[Option[Result]] = {

    val pattern = """^application/vnd[.]{1}hmrc[.]{1}1{1}[.]0[+]{1}json$""".r

    val result = for {
      acceptHeader             <- validateAcceptHeader(request)
      contentTypeHeader        <- validateContentTypeHeader(request)
      isValidAcceptHeaderFormat = pattern.matches(acceptHeader)
      isValidContentTypeHeader  = contentTypeHeader.equals(MimeTypes.JSON)

    } yield (isValidAcceptHeaderFormat, isValidContentTypeHeader) match {
      case (true, true) => None
      case _            => Some(InvalidHeaderErrorResponse(datetimeService.timestamp, "Invalid Header").toResult)
    }

    result.merge
  }

  private def validateAcceptHeader(request: Request[_]): EitherT[Future, Option[Result], String] =
    EitherT.fromOption(
      request.headers.get(HeaderNames.ACCEPT),
      Some(InvalidHeaderErrorResponse(datetimeService.timestamp, "The accept header is missing").toResult)
    )

  private def validateContentTypeHeader(request: Request[_]): EitherT[Future, Option[Result], String] =
    EitherT.fromOption(
      request.headers.get(HeaderNames.CONTENT_TYPE),
      Some(InvalidHeaderErrorResponse(datetimeService.timestamp, "The Content-Type header is missing").toResult)
    )

}
