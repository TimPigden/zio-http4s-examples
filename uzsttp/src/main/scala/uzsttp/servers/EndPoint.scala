package uzsttp.servers

import uzhttp.Request.{Method, WebsocketRequest}
import uzhttp.{HTTPError, Request, Response}
import zio._
import uzsttp.auth.Authorizer._
import uzsttp.servers.EndPoint.HRequest
import cats.implicits._
import Utils._
import cats.data.NonEmptyList
import uzhttp.HTTPError.{BadRequest, Forbidden, NotFound, Unauthorized}
import uzsttp.auth.auth
import zio.stream._
import hrequest._
import uzhttp.websocket._
import uzsttp.encoding.Encoders.{StringParser, StringWriter}
import zio.stream.ZStream.Take

object EndPoint {
  type HRequest = Has[Request]

  type EndPoint[R <: HRequest] = ZIO[R, Option[HTTPError], Response]

  def endsWith(ss: NonEmptyList[String])(in: Seq[String]): Boolean =
    in.endsWith(ss.toList)

  def endsWith(s: String)(in: Seq[String]): Boolean =
    in.endsWith(List(s))

  def startsWith(ss: NonEmptyList[String])(in: Seq[String]): Boolean =
    in.startsWith(ss.toList)

  def startsWith(s: String)(in: Seq[String]): Boolean = {
    println(s"compere $s to : $in")
    in.startsWith(List(s))
  }

  def uriMethod(pMatch: Seq[String] => Boolean, expectedMethod: Method): ZIO[HRequest, Option[HTTPError], Unit] = {
    for {
      pth <- uri
      mtd <- method
      matched <- if (pMatch(pth) && (mtd === expectedMethod))
        IO.unit else IO.fail(None)
    } yield matched
  }

  def orNotFound[R <: HRequest](p: EndPoint[R]): ZIO[R, HTTPError, Response] =
    for {
      r <- request
      pp <- p.mapError {
        case Some(err) => err
        case None => NotFound(r.uri.getPath)
      }
    } yield pp

  def authStatus(s: String): ZIO[Auth, Option[HTTPError], Unit] =
    for {
      stat <- auth.status
      _ = println(s"exected $s got $stat")
      res <- if (stat === s) IO.unit
      else IO.fail(Some(Forbidden("go get permission")))
    } yield res

  def noAuthHandler(p: EndPoint[HRequest]): Request => IO[HTTPError, Response] =
    { req: Request =>
        orNotFound(p).provideLayer(ZLayer.succeed(req))
    }

  def authHandler(p: EndPoint[HRequest with Auth]): ZIO[Authorizer, HTTPError, Request => IO[HTTPError, Response]] =
    ZIO.access[Authorizer](_.get).map { aut => { req: Request =>
      (for {
        authInfo <- getAuthorization(req).provideLayer(ZLayer.succeed(aut))
        res <- orNotFound(p).provideLayer(ZLayer.succeed(req) ++ ZLayer.succeed(authInfo))
      } yield res).mapError { th =>
        th match {
          case herr: HTTPError => herr
          case th => Unauthorized(th.getMessage)
        }
      }}
    }

  def combineRoutes[R <: HRequest](h: EndPoint[R], t: EndPoint[R]*): EndPoint[R] =
    t.foldLeft(h)((acc, it) =>
      acc catchSome { case None => it }
    )

  def requestStringBody(req: Request): IO[HTTPError, String] =
    req.body match {
      case Some(value) =>
        value.transduce(ZTransducer.utf8Decode).runHead.someOrFail(BadRequest("Missing body"))
      case None => ZIO.fail(BadRequest("Missing body"))
    }
}

package object hrequest {
  def request = ZIO.access[HRequest](_.get)

  def stringBody =
    for {
      req <- request
      s <- EndPoint.requestStringBody(req)
    } yield s

  def uri = request.map { r =>
    r.uri.getPath.split("/").toList.filterNot(_ == "")
  }

  def method = request.map(_.method)


  def webSocket: ZIO[HRequest, HTTPError, WebsocketRequest] =
    for {
      r <- request
      ws <- r match {
        case wr: WebsocketRequest => IO.succeed(wr)
        case x => IO.fail(BadRequest("not a websocket"))
      }
    } yield ws

  def handleWebsocketFrame(textHandler: String => Stream[HTTPError, Take[Nothing, Frame]])
                          (frame: Frame): Stream[HTTPError, Take[Nothing, Frame]] = frame match {
    case frame@Binary(data, _) => Stream.empty
    case frame@Text(data, _) => textHandler(data)
    case frame@Continuation(data, _) => Stream.empty
    case Ping => Stream(Exit.succeed(Chunk(Pong)))
    case Pong => Stream.empty
    case Close => Stream(Exit.succeed(Chunk(Close)), Take.End)
  }


}
