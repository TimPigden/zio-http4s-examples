package uzsttp.servers
import uzhttp._
import zio._
import Request._
import uzsttp.auth.Authorizer.{Auth, AuthInfo}
import cats.implicits._
import Utils._
import uzhttp.HTTPError.{Forbidden, Unauthorized}
import uzsttp.encoding.Encoders.writeXmlBody
import uzsttp.servers.Processor.{HRequest, Processor, startsWith, uriMethod}
import Processor._

object AuthorizedRoutes2 {

  val authorized: Processor[HRequest with Auth] =
    for {
      _ <- uriMethod(endsWith("authorized"), Method.GET)
      _ <- authStatus("Vetted")
    } yield Response.plain("OK")

}
