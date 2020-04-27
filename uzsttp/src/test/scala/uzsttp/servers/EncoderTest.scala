package uzsttp.servers
import sttp.client._
import sttp.model.StatusCode
import sttp.client.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import zio._
import zio.test._
import Assertion._
import TestUtil._
import zio.test.environment._
import uzsttp.auth.Authorizer
import zio.blocking.Blocking
import zio.clock.Clock
import uzsttp.encoding.Encoders._
import Person._
import TestAspect._

object EncoderTest extends DefaultRunnableSpec {

  def hasDonald = testM("we have a president") {
    for {
      _ <- serverUp
      response <- SttpClient.send(basicRequest.get(uri"http://localhost:8080/president"))
      body = response.body
      goodBody <- body match {
        case Left(errs) => IO.fail(new Throwable(s"bad body $errs"))
        case Right(bdy) => parseXmlString[Person](bdy)
      }
    } yield assert(goodBody)(equalTo(donald))
  }

  def isJoe = testM("joe's name comes back") {
    for {
      _ <- serverUp
      response <- SttpClient.send(basicRequest.post(uri"http://localhost:8080/whatIsMyName")
      .body(writeXmlString(joe)))
    } yield assert(response.body)(equalTo(Right(joe.name)))
  }

  def badBodyJoe = testM("badRequest") {
    for {
      _ <- serverUp
      response <- SttpClient.send(basicRequest.post(uri"http://localhost:8080/whatIsMyName")
        .body("joe was the vp"))
    } yield assert(response.code)(equalTo(StatusCode.BadRequest))
  }

  val withPartialFunction = suite("all tests using partial function")(
    hasDonald,
    isJoe,
    badBodyJoe
  ).provideSomeLayerShared[TestEnvironment with SttpClient](serverLayer(XmlRoutes.routes))


  override def spec = withPartialFunction.provideCustomLayer(AsyncHttpClientZioBackend.layer()).mapError(TestFailure.fail)


}
