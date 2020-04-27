package uzsttp.servers
import uzhttp._
import zio._
import Request._
import uzsttp.auth.Authorizer.Auth
import uzsttp.servers.EndPoint.{HRequest, EndPoint, uriMethod}
import EndPoint._

object AuthorizedRoutes2 {

  val authorized: EndPoint[HRequest with Auth] =
    for {
      _ <- uriMethod(endsWith("authorized"), Method.GET)
      _ <- authStatus("Vetted")
    } yield Response.plain("OK")

}
