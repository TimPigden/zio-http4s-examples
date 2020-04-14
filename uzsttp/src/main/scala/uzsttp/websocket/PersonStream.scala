package uzsttp.websocket
import uzhttp.HTTPError.BadRequest
import uzhttp.Request.Method
import uzhttp.{HTTPError, Response}
import uzhttp.websocket.{Frame, Text}
import uzsttp.servers.Person._
import uzsttp.encoding.Encoders._
import uzsttp.servers.Person
import zio.stream._
import uzsttp.servers.hrequest._
import uzsttp.servers.Processor._
import zio._
object PersonStream {

  def streamCrowd = {
    val l1 = crowd.map{ person =>
      Text(writeXmlString(person), true)
    }
    ZStream.fromIterable(l1)
  }

  def agePerson(text: String): IO[HTTPError, Text] =
    parseXmlString[Person](text).map { person =>
      println(s"aging $person")
      val aged = Text(writeXmlString(older(person)), true)
      println(s"to $aged")
      aged
    }.mapError(e => BadRequest(e.getMessage))


  val agePeople: Processor[HRequest] =
    for {
      req <- webSocket.mapError(e => Some(e))
      _ = println(s"got req $req")
      _ <- uriMethod(endsWith("wsIn"), Method.GET)
      _ = println(s"uri and method ok")
      streamOut = Stream.flatten(req.frames.mapM(handleWebsocketFrame(agePerson))).unTake
      _ = println(s"streamOut is $streamOut")
      response <- Response.websocket(req, streamOut).mapError(e => Some(e))
    } yield response

}
