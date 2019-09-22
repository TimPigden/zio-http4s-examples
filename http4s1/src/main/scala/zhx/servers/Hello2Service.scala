package zhx.servers

import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl
import zhx.auth.Authenticator
import zhx.auth.Authenticator.AuthToken
import zio.RIO
import zio.interop.catz._

/**
 * simplest possible service with a single get as string
 */
class Hello2Service[R <: Authenticator] {

  type AuthenticatorTask[T] = RIO[R, T]
  private val dsl = Http4sDsl[AuthenticatorTask]
  import dsl._

  val service = AuthedRoutes.of[AuthToken, AuthenticatorTask] {
    case GET -> Root as authToken => Ok(s"hello! ${authToken.tok}")
  }

}
