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
import uzsttp.servers.EndPoint._
import zio._
object PersonStream {

  def agePerson(text: String): IO[HTTPError, Text] =
    parseXmlString[Person](text).map { person =>
      Text(writeXmlString(older(person)), true)
    }.mapError(e => BadRequest(e.getMessage))


  val agePeople: EndPoint[HRequest] =
    for {
      req <- webSocket.mapError(e => Some(e))
      _ <- uriMethod(endsWith("wsIn"), Method.GET)
      streamOut = Stream.flatten(req.frames.mapM(handleWebsocketFrame(agePerson))).unTake
      response <- Response.websocket(req, streamOut).mapError(e => Some(e))
    } yield response

}
