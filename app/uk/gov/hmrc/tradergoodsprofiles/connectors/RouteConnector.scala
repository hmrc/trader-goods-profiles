package uk.gov.hmrc.tradergoodsprofiles.connectors

import play.api.mvc.Result
import uk.gov.hmrc.tradergoodsprofiles.models.GetRecordResponse

import scala.concurrent.Future

class RouteConnector {
  def get(): Future[Either[Result, GetRecordResponse]] = ???

}
