package zhx.servers

import org.http4s._
import zhx.auth.{AuthenticationHeaders, Authenticator}
import zio._
import org.http4s.implicits._
import zio.interop.catz._
import zio.test.{DefaultRunnableSpec, Predicate, assert, fail, suite, testM}
import Middlewares._
import org.http4s.server.Router
object TestHello2Service extends  DefaultRunnableSpec(

  suite("routes suite") (

    testM("root request returns forbidden") {
      val io = hello2Service.run(Request[withMiddleware.AppTask](Method.GET, Uri.uri("/")))
          .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
      io.fold(
        e => fail(Cause.fail(e)),
        s => assert(s.status, Predicate.equals(Status.Forbidden))) // will fail if nothing there
    },

    testM("root request with authentication returns ok") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, Uri.uri("/"))
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "friend")
      val io = hello2Service.run(req)
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
      io.fold(
        e => fail(Cause.fail(e)),
        s => assert(s.status, Predicate.equals(Status.Ok))) // will fail if nothing there
    }
    ,
    testM("unmapped request returns not found") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, Uri.uri("/a"))
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "friend")
      val io = hello2Service.run(req)
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
      io.fold(
        e => fail(Cause.fail(e)),
        s => assert(s.status, Predicate.equals(Status.NotFound))
      )
    }
    ,
    testM("root request body returns hello!") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, Uri.uri("/"))
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "friend")
      val io = hello2Service.run(req)
      (for {
        request <- io
        body <- request.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
      } yield body)
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
        .fold(
          e => fail(Cause.fail(e)),
          s => assert(s, Predicate.equals("hello! tim")))
    }
    ,
    testM("bad password gives forbidden") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, Uri.uri("/"))
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "frond")
      val io = hello2Service.run(req)
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
      io.fold(
        e => fail(Cause.fail(e)),
        s => assert(s.status, Predicate.equals(Status.Forbidden))
      )
    }

  )
)
object Middlewares {
  val withMiddleware = new AuthenticationMiddleware {
    override type AppEnvironment = Authenticator
  }

  val hello2Service1 = new Hello2Service[Authenticator]
  // todo - is there a better way to add isNotFound to the middleware service?
  val hello2Service = Router[withMiddleware.AppTask](
  ("" -> withMiddleware.authenticationMiddleware(hello2Service1.service)))
    .orNotFound
}
