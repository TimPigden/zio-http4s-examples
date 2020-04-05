package zhx.servers

import org.http4s.{AuthedRequest, AuthedRoutes}
import org.http4s.dsl.Http4sDsl
import zhx.auth.Authenticator
import zhx.auth.Authenticator._
import zio.RIO
import zio.interop.catz._
import zhx.encoding.Encoders._

/**
 * Example combining Encoding and Authentication
 */
class Hello4Service[R <: Authenticator] {

  type AuthenticatorTask[T] = RIO[R, T]
  private val dsl = Http4sDsl[AuthenticatorTask]
  import dsl._

  val service = AuthedRoutes.of[AuthToken, AuthenticatorTask] {
    case GET -> Root as authToken => Ok("hello4!")
    case GET -> Root / "president" as authToken => Ok(Person.donald) // uses implicit encoder
    case AuthedRequest(authToken, req @ POST -> Root / "ageOf") =>
      req.decode[Person] { m => Ok(m.age.toString)}
  }

}
