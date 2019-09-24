package zhx.auth

import zhx.auth.Authenticator.AuthenticationError
import zio.{IO, Task, ZIO}

// first test of https middleware
object Authenticator {

  case class AuthToken(tok: String)

  trait AuthenticationError extends Throwable

  val authenticationError: AuthenticationError = new AuthenticationError {
    override def getMessage: String = "Authentication Error"
  }

  trait Service {
    def authenticate(userName: String, password: String): Task[AuthToken]
  }

  val friendlyAuthenticator: Service = { (userName, password) =>
    password match {
      case "friend" => IO.succeed(AuthToken(userName)) // rather trivial implementation but does allow us to inject variety
      case _ => IO.fail(authenticationError)
    }
  }
}

trait Authenticator { def authenticatorService: Authenticator.Service }

package object authenticator {
  def authenticatorService: ZIO[Authenticator, AuthenticationError, Authenticator.Service] = ZIO.accessM(x => ZIO.succeed(x.authenticatorService))
}


