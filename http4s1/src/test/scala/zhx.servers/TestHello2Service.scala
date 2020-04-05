package zhx.servers

import org.http4s._
import zhx.auth.{AuthenticationHeaders, Authenticator}
import org.http4s.implicits._
import zio.interop.catz._
import Middlewares._
import zio.test._
import zio.test.environment._
import cats.implicits._
import zio.test.Assertion._
import org.http4s.server.Router
import zhx.auth.Authenticator.Authenticator
import zio.IO
object TestHello2Service extends  DefaultRunnableSpec {

  override def spec = suite("routes suite")(

    testM("root request returns forbidden") {
      val io = hello2Service.run(Request[withMiddleware.AppTask](Method.GET, uri"/"))
      assertM(io.map(_.status))(
        equalTo(Status.Forbidden)) // will fail if nothing there
    },

    testM("root request with authentication returns ok") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, uri"/")
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "friend")
      val io = hello2Service.run(req).provideCustomLayer(Authenticator.friendly)
      assertM(io.map(_.status))(equalTo(Status.Ok)) // will fail if nothing there
    }
    ,
    testM("unmapped request returns not found") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, uri"/a")
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "friend")
      val io = hello2Service.run(req)
      assertM(io.map(_.status))(equalTo(Status.NotFound))
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
      assertM(iop)(equalTo("hello! tim"))
    }
    ,
    testM("bad password gives forbidden") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, uri"/")
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "frond")
      val io = hello2Service.run(req).provideCustomLayer(Authenticator.friendly)
      assertM(io.map(_.status))(equalTo(Status.Forbidden))
    }

  ).provideCustomLayerShared(Authenticator.friendly)
}

object Middlewares {
  val withMiddleware = new AuthenticationMiddleware {
    override type AppEnvironment = Authenticator
  }

  val hello2Service1 = new Hello2Service[Authenticator]

  val hello2Service = Router[withMiddleware.AppTask](
  ("" -> withMiddleware.authenticationMiddleware(hello2Service1.service)))
    .orNotFound
}
