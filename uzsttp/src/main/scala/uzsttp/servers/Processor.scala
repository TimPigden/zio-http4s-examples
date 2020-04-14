package uzsttp.servers

import uzhttp.Request.{Method, WebsocketRequest}
import uzhttp.{HTTPError, Request, Response}
import zio._
import uzsttp.auth.Authorizer._
import uzsttp.servers.Processor.HRequest
import cats.implicits._
import Utils._
import cats.data.NonEmptyList
import sttp.model.Uri
import sttp.model.Uri.PathSegment
import uzhttp.HTTPError.{BadRequest, Forbidden, NotFound, Unauthorized}
import uzsttp.auth.{auth, authorizer}
import zio.stream._
import hrequest._
import sttp.client.HttpError
import uzhttp.websocket._

object Processor {
  type HRequest = Has[Request]

  type Processor[R <: HRequest] = ZIO[R, Option[HTTPError], Response]

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

  def orNotFound[R <: HRequest](p: Processor[R]): ZIO[R, HTTPError, Response] =
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

  def noAuthHandler(p: Processor[HRequest]): Request => IO[HTTPError, Response] =
    { req: Request =>
        orNotFound(p).provideLayer(ZLayer.succeed(req))
    }

  def authHandler(p: Processor[HRequest with Auth]): ZIO[Authorizer, HTTPError, Request => IO[HTTPError, Response]] =
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

  def combineRoutes[R <: HRequest](h: Processor[R], t: Processor[R]*): Processor[R] =
    t.tail.foldLeft(h)((acc, it) =>
      acc orElseOptional it
    )
}

package object hrequest {
  def request = ZIO.access[HRequest](_.get)
  def uri = request.map{r =>
    r.uri.getPath.split("/").toList.filterNot(_ == "") }
  def method = request.map(_.method)
  def stringBody = for {
    req <- request
    s <- req.body match {
      case Some(value) =>
        value.run(ZSink.utf8DecodeChunk)
      case None => ZIO.fail(BadRequest("Missing body"))
    }
  } yield s

  def webSocket: ZIO[HRequest, HTTPError, WebsocketRequest] =
    for {
      r <- request
      ws <- r match {
        case wr: WebsocketRequest => IO.succeed(wr)
        case x => IO.fail(BadRequest("not a websocket"))
      }
    } yield ws

  def handleWebsocketFrame(textHandler: String => IO[HTTPError, Frame])
                          (frame: Frame): UIO[Stream[HTTPError, Take[Nothing, Frame]]] = frame match {
    case frame@Binary(data, _)       => UIO.succeed(Stream.empty)
    case frame@Text(data, _)         => textHandler(data)
        .either.map {
          case Left(err) => Stream.fail(err)
          case Right(f) => Stream(Take.Value(f))
    }
    case frame@Continuation(data, _) => UIO.succeed(Stream.empty)
    case Ping => UIO(Stream(Take.Value(Pong)))
    case Pong => UIO(Stream.empty)
    case Close => UIO(Stream(Take.Value(Close), Take.End))
  }
}
