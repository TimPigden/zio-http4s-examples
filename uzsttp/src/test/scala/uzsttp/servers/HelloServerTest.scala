package uzsttp.servers
import sttp.client._
import sttp.model.StatusCode
import sttp.client.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import zio._
import zio.test._
import Assertion._
import TestUtil._
import uzsttp.auth.Authorizer
import zio.blocking.Blocking
import zio.clock.Clock

object HelloServerTest extends DefaultRunnableSpec {

  def hasRoot = testM("service has root") {
    for {
      _ <- serverUp
      response <- SttpClient.send(basicRequest.get(uri"http://localhost:8080/"))
    } yield assert(response.code)(equalTo(StatusCode.Ok))
  }

  def hasBody = testM("service has body") {
    for {
      _ <- serverUp
      response <- SttpClient.send(basicRequest.get(uri"http://localhost:8080/"))
    } yield {
      assert(response.body)(equalTo(Right("OK")))
    }
  }

  override def spec = suite("all tests")(
    testHello1
  )

  val testHello1 = suite("test hello1 with sttp client")(
    hasRoot,
    hasBody,
  ).provideCustomLayerShared(AsyncHttpClientZioBackend.layer() ++ serverLayer(Hello1Routes.routes)).mapError(TestFailure.fail)

}
