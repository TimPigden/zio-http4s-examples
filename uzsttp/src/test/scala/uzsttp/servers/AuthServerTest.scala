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
import zio.test.environment.TestEnvironment

object AuthServerTest extends DefaultRunnableSpec {

  override def spec = suite("all tests")(
    testAuth,
    testAuthProcessor
  ).provideCustomLayer(AsyncHttpClientZioBackend.layer()).mapError(TestFailure.fail)

  val noAuthentication = testM("root request with no authentication returns Unauthorized") {
    for {
      _ <- serverUp
      response <- SttpClient.send(basicRequest.get(uri"http://localhost:8080/authorized/"))
    } yield assert(response.code)(equalTo(StatusCode.Unauthorized))
  }

  val noAuthorization = testM("root request with authentication but no authorization returns") {
    for {
      _ <- serverUp
      response <- SttpClient.send(
        basicRequest.get(uri"http://localhost:8080/authorized/")
          .header(Authorizer.Authorization, "anybody")
      )

    } yield assert(response.code)(equalTo(StatusCode.Unauthorized))
  }

  val insufficientAuthorization = testM("root request with authentication and low level authorisation") {
    for {
      _ <- serverUp
      response <- SttpClient.send(
        basicRequest.get(uri"http://localhost:8080/authorized/")
          .header(Authorizer.Authorization, "acquaintance")
      )

    } yield assert(response.code)(equalTo(StatusCode.Forbidden))
  }

  val sufficientAuthorization = testM("root request with authentication and high level authorisation") {
    for {
      _ <- serverUp
      response <- SttpClient.send(
        basicRequest.get(uri"http://localhost:8080/authorized/")
          .header(Authorizer.Authorization, "friend")
      )
    } yield assert(response.code)(equalTo(StatusCode.Ok))
  }

  val notFoundTrumpsNoAuthentication = testM("no auth, wrong page gives not found but not really what we want") {
    // the original formulation returns not found. However, we actually prefer unauthorized if not logged in
    for {
      _ <- serverUp
      response <- SttpClient.send(
        basicRequest.get(uri"http://localhost:8080/authorized/a")
      )
    } yield assert(response.code)(equalTo(StatusCode.NotFound))
  }

  val noAuthenticationTrumpsNotFound = testM("no auth, wrong page gives no auth") {
    for {
      _ <- serverUp
      response <- SttpClient.send(
        basicRequest.get(uri"http://localhost:8080/authorized/a")
      )
    } yield assert(response.code)(equalTo(StatusCode.Unauthorized))
  }


  val notFoundTrumpsAuthentication = testM("good auth, wrong page gives not found") {
    for {
      _ <- serverUp
      response <- SttpClient.send(
        basicRequest.get(uri"http://localhost:8080/authorized/a")
          .header(Authorizer.Authorization, "friend")
      )
    } yield assert(response.code)(equalTo(StatusCode.NotFound))
  }

  val testAuth = suite("test authorization sttp client")(
    noAuthentication,
    noAuthorization,
    insufficientAuthorization,
    sufficientAuthorization,
    notFoundTrumpsNoAuthentication,
    notFoundTrumpsAuthentication,
  ).provideSomeLayerShared[TestEnvironment with SttpClient]((Blocking.live ++ Clock.live ++ Authorizer.friendlyAuthorizerLive) >>> authLayer(AuthorizedRoutes.routes))

  val testAuthProcessor = suite("test authorization sttp client")(
    noAuthentication,
    noAuthorization,
    insufficientAuthorization,
    sufficientAuthorization,
    noAuthenticationTrumpsNotFound,
    notFoundTrumpsAuthentication,
  ).provideSomeLayerShared[TestEnvironment with SttpClient]((Blocking.live ++
    Clock.live ++ Authorizer.friendlyAuthorizerLive) >>> authLayer2(AuthorizedRoutes2.authorized))

}
