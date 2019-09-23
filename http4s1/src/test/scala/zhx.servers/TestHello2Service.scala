package zhx.servers

import org.http4s._
import zhx.auth.{AuthenticationHeaders, Authenticator}
import zio._
import org.http4s.implicits._
import zio.interop.catz._
import Middlewares._
import zio.test._
import zio.test.Assertion._
import org.http4s.server.Router
object TestHello2Service extends  DefaultRunnableSpec(

  suite("routes suite") (


    testM("root request returns forbidden") {
      val io = hello2Service.run(Request[withMiddleware.AppTask](Method.GET, uri"/"))
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
      assertM(io.map(_.status),
        equalTo(Status.Forbidden)) // will fail if nothing there
    },

    testM("root request body returns hello!") {
      val req = Request[withMiddleware.AppTask](Method.GET, uri"/")
      val io = hello2Service.run(req)
      val iop = (for {
        request <- io
        body <- request.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
        _ = println(s"got body $body")
      } yield body)
        .provide(new Authenticator {
          override val authenticatorService = Authenticator.friendlyAuthenticator
        })
      assertM(iop, equalTo("hello! tim"))
    },
/*
    testM("root request with authentication returns ok") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, uri"/")
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "friend")
      val io = hello2Service.run(req)
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
      assertM(io.map(_.status), equalTo(Status.Ok)) // will fail if nothing there
    }
    ,
    testM("unmapped request returns not found") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, uri"/a")
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "friend")
      val io = hello2Service.run(req)
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
      assertM(io.map(_.status), equalTo(Status.NotFound))
    }
    ,
    testM("root request body returns hello!") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, uri"/")
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "friend")
      val io = hello2Service.run(req)
      val iop = (for {
        request <- io
        body <- request.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
      } yield body)
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
      assertM(iop, equalTo("hello! tim"))
    }
    ,
    testM("bad password gives forbidden") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, uri"/")
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "frond")
      val io = hello2Service.run(req)
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
      assertM(io.map(_.status), equalTo(Status.Forbidden))
    }
*/

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
