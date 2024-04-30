package uk.gov.hmrc.tradergoodsprofiles.connectors

import org.scalatestplus.play.PlaySpec

class RouteConnectorSpec extends PlaySpec {

  private val sut = new RouteConnector()

  "get" should {
    "return 200" in {
      val result = sut.get()


    }
  }
}
