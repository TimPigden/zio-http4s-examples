package zhx.servers
import cats.Monad
import cats.effect._
import fs2.Stream.Compiler._
import org.http4s.headers.{Connection, `Content-Length`}
import org.http4s.{Headers, MessageFailure, Response, Status}
import org.http4s.implicits._
import org.http4s.server.{AuthMiddleware, Router, ServiceErrorHandler}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import zhx.auth.Authenticator
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console._
import zio.interop.catz._


object Hello2 extends App with AuthenticationMiddleware {

  type AppEnvironment = Clock with Console with Authenticator with Blocking
  import dsl._

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    server.foldM(err => putStrLn(s"execution failed with $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))

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
}
