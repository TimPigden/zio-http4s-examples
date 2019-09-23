package zhx.servers
import cats.data.{Kleisli, OptionT}
import fs2.Stream.Compiler._
import org.http4s.{Request, Response}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import zhx.auth.Authenticator
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console._
import zio.interop.catz._


object Hello2 extends App with AuthenticationMiddleware {

  type AppEnvironment = Clock with Console with Authenticator with Blocking

  val hello2Service = new Hello2Service[AppEnvironment] {}

  val authenticatedService = authenticationMiddleware(hello2Service.service)

  val secApp = Router[AppTask](
    "" -> authenticatedService
  ).orNotFound

  val server1 = ZIO.runtime[AppEnvironment]
    .flatMap {
      implicit rts =>
        BlazeServerBuilder[AppTask]
          .bindHttp(8080, "localhost")
          .withHttpApp(secApp)
          .serve
          .compile
          .drain
    }

  val server = server1
    .provideSome[Environment] { base =>
      new Clock with Console with Blocking with Authenticator {
        override val clock: Clock.Service[Any] = base.clock
        override val console: Console.Service[Any] = base.console
        override val blocking: Blocking.Service[Any] = base.blocking

        override def authenticatorService: Authenticator.Service = Authenticator.friendlyAuthenticator
      }
    }

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    server.foldM(err => putStrLn(s"execution failed with $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))

}
