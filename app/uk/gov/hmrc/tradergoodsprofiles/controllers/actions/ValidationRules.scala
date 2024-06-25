package uk.gov.hmrc.tradergoodsprofiles.controllers.actions

import play.api.http.HeaderNames
import play.api.mvc.{BaseController, Request, Result}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{BadRequestErrorResponse, Error}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants._

trait ValidationRules {
  this: BaseController =>

  def uuidService: UuidService

  protected def validateContentType(implicit request: Request[_]): Either[Error, _] =
    request.headers
      .get("Content-Type")
      .filter(_ == "application/json")
      .toRight(
        Error(InvalidHeaderParameter, InvalidHeaderContentTypeMessage, InvalidHeaderContentType)
      )

  protected def validateAcceptHeader(implicit request: Request[_]): Either[Error, String] = {
    val pattern = """^application/vnd[.]{1}hmrc[.]{1}1{1}[.]0[+]{1}json$""".r

    request.headers
      .get(HeaderNames.ACCEPT)
      .filter(pattern.matches(_))
      .toRight(Error(InvalidHeaderParameter, InvalidHeaderAcceptMessage, InvalidHeaderAccept))

  }

  protected def validateClientIdHeader(implicit request: Request[_]): Either[Error, String] =
    request.headers
      .get(XClientIdHeader)
      .toRight(Error(InvalidHeaderParameter, InvalidHeaderClientIdMessage, InvalidHeaderClientId))

  protected def validateAllHeader(implicit request: Request[_]): Either[Result, _] =
    (for {
      _ <- validateAcceptHeader
      _ <- validateContentType
      _ <- validateClientIdHeader
    } yield ()).left
      .map(e =>
        BadRequestErrorResponse(
          uuidService.uuid,
          e.code,
          e.message,
          e.errorNumber
        ).toResult
      )
  protected def validateAcceptAndClientIdHeader(implicit
    request: Request[_]
  ): Either[Result, _]                                                             =
    foldLeftBreak(List(validateAcceptHeader, validateClientIdHeader)).left
      .map(e =>
        BadRequestErrorResponse(
          uuidService.uuid,
          e.code,
          e.message,
          e.errorNumber
        ).toResult
      )

  private def foldLeftBreak(list: List[Either[Error, _]]): Either[Error, _] =
    //list => [Right(()), Left(E), Right(()) ]
    list match {
      case Nil          => Right(())
      case head :: tail =>
        head match {
          case l @ Left(_) => l
          case Right(_)    => foldLeftBreak(tail)
        }
    }

}
