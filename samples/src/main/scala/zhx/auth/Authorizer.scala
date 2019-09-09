package zhx.auth

import zhx.auth.Authenticator.AuthToken
import zio.{IO, Task, ZIO}

object Authorizer {

  // we implement a trivial authorization level to demonstration the separation of
  // responsibility between authorization and authentication

  sealed trait Role
  case object Reader extends Role
  case object Writer extends Role
  case object Owner extends Role

  type RoleSet = Set[Role]

  trait Service {
    def roles(authToken: AuthToken): Task[RoleSet]
  }

  val simpleAuthorizer : Service = { authToken =>
    IO.succeed(authToken.tok match {
      case "john" => Set(Reader, Writer, Owner)
      case "tim" => Set(Reader, Writer)
      case "tom" => Set(Reader)
      case "" => Set.empty
    }
    )
  }
}

trait Authorizer { def authorizerService: Authorizer.Service }

package object authorizer {
  def authorizerService: ZIO[Authorizer, Throwable, Authorizer.Service] = ZIO.accessM(x => ZIO.succeed(x.authorizerService))
}

