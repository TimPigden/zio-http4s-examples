package zhx.servers

import org.http4s.server.blaze._
import zhx.auth.Authenticator
import zio._
import zio.clock.Clock
import zio.console.Console
import zio.interop.catz._
import zio.interop.catz.implicits._

object Hello2 extends App with AuthenticationMiddleware {

  type AppEnvironment = Clock with Console with Authenticator

  def run(args: List[String]) =
    server.fold(_ => 1, _ => 0)

  val hello2Service = new Hello2Service[Authenticator]

  val authenticatedService = authenticationMiddleware(hello2Service.service)

  val server = ZIO.runtime[AppEnvironment]
    .flatMap {
      implicit rts =>
        BlazeServerBuilder[Task]
          .bindHttp(8080, "localhost")
          .withHttpApp(authenticatedService)
          .serve
          .compile
          .drain
    }

}
