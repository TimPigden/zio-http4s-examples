package uzsttp.servers
import sttp.client.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import zio.test._
import TestUtil._
import zio.test.environment._
import EncoderTest._

object WithEndPoint extends DefaultRunnableSpec {

  val withEndPoint = suite("all tests using processor")(
    hasDonald,
    isJoe,
    badBodyJoe
  ).provideSomeLayerShared[TestEnvironment with SttpClient](serverLayer2(XmlRoutes2.routes))

  override def spec = withEndPoint.provideCustomLayer(AsyncHttpClientZioBackend.layer()).mapError(TestFailure.fail)


}
