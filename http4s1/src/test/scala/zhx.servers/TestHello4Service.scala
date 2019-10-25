package zhx.servers

import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import zhx.auth.{AuthenticationHeaders, Authenticator}
import zhx.encoding.Encoders._
import zhx.servers.Middlewares.withMiddleware
import zio._
import zio.interop.catz._
import zio.test._
import zio.test.Assertion._
import MoreMiddlewares._

object TestHello4Service extends DefaultRunnableSpec(
  suite("routes suite")(
    testM("president returns donald") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, uri"/president")
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "friend")
      val io = (for{
        response <- hello4Service.run(req)
        body <- response.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
        parsed <- parseIO(body)
      }yield parsed)
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
      assertM(io, equalTo(Person.donald))
    },
    testM("joe is 76") {
      val req1 = Request[withMiddleware.AppTask](Method.POST, uri"/ageOf")
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "friend")
        .withEntity(Person.joe)
      val io = (for{
        response <- hello4Service.run(req)
        body <- response.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
      }yield body)
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
      assertM(io, equalTo("76"))
    }

  ))


object MoreMiddlewares {
  val hello4Service1 = new Hello4Service[Authenticator]
  val hello4Service = Router[withMiddleware.AppTask](
    "" -> withMiddleware.authenticationMiddleware(hello4Service1.service))
    .orNotFound

}
