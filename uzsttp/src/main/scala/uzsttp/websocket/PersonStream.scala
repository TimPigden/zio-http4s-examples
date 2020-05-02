package uzsttp.websocket
import uzhttp.HTTPError.BadRequest
import uzhttp.Request.{Method, WebsocketRequest}
import uzhttp.{HTTPError, Response}
import uzhttp.websocket.{Frame, Text}
import uzsttp.servers.Person._
import uzsttp.encoding.Encoders._
import uzsttp.servers.Person
import zio.stream._
import uzsttp.servers.hrequest._
import uzsttp.servers.EndPoint._
import zio._
import zio.clock.Clock
import zio.duration._

object PersonStream {
  def agePerson(text: String): Stream[HTTPError, Take[Nothing, Text]] =
    ZStream.unwrap {
      parseXmlString[Person](text).bimap(err => BadRequest(err.getMessage),
        person => Stream(Take.Value(Text(writeXmlString(older(person)), true))))
    }

  val agePersonByOne: EndPoint[HRequest] =
    for {
      req <- webSocket.mapError(e => Some(e))
      _ <- uriMethod(endsWith("wsPersonOneByOne"), Method.GET)

      streamOut = Stream.flatten(req.frames.map(handleWebsocketFrame(agePerson))).unTake
      response <- Response.websocket(req, streamOut).mapError(e => Some(e))
    } yield response
}

case class PersonStream(clk: Clock.Service) {

  def autoAge(person: Person): Stream[Nothing, Person] = {
    println(s"calling autoAge with $person")
    val stream = if (person.age > 100)
        Stream(DEATH) // allows me to test an empty stream
      else if (person.age > 30) // limited length stream
        ZStream.fromIterable(person.age.to(100)).map{ i =>
          val p = person.copy(age = i)
          println(s"creating person $p")
          p
        }.concat(Stream(DEATH))
      else // will probably live forever!
        ZStream.iterate(person.age)(_ + 1).map(i => person.copy(age = i))
    stream
  }

  def autoAgeText(text: String): Stream[HTTPError, Take[Nothing, Frame]] =
    ZStream.unwrap {
      parseXmlString[Person](text).bimap(err => BadRequest(err.getMessage),
        person => autoAge(person).map { p => Take.Value(Text(writeXmlString(p)))})
    }

  val agePerson: EndPoint[HRequest] =
    for {
      req <- webSocket.mapError(e => Some(e))
      _ <- uriMethod(endsWith("wsPerson"), Method.GET)
      streamOut = Stream.flatten(req.frames.map(handleWebsocketFrame(autoAgeText))).unTake
      response <- Response.websocket(req, streamOut).mapError(e => Some(e))
    } yield response

}
