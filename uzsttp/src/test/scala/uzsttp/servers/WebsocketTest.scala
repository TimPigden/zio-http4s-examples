package uzsttp.servers

import zio.test._
import sttp.client._
import sttp.model.StatusCode
import sttp.model.ws.WebSocketFrame
import sttp.client.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient, ZioWebSocketHandler}
import sttp.client.ws.WebSocket
import zio._
import uzsttp.encoding.Encoders._
import Person._
import Assertion._
import TestUtil._
import uzsttp.websocket.PersonStream._
import zio.clock.Clock

object WebsocketTest extends DefaultRunnableSpec {

  def sendPerson(person: Person, ws: WebSocket[Task]) = {
    println(s"sending person $person")
    ws.send(WebSocketFrame.text(writeXmlString(person)))
  }

  def next(ws: WebSocket[Task]): Task[Option[Person]] =
    for {
      et <- ws.receiveText()
      personOpt <- et match {
        case Right(t) => parseXmlString(t).map(Some(_))
        case _ => IO.succeed(None)
      }
    } yield personOpt

  val peopleAge = testM("test age people"){

    for {
      _ <- serverUp
      response <- SttpClient.openWebsocket(basicRequest.get(uri"ws://localhost:8080/wsIn"))
      _ = println(s"response is $response")
      ws = response.result
      sent <- sendPerson(joe, ws)
      joeOlder <- next(ws)
    } yield assert(joeOlder)(equalTo(Some(older(joe))))

  }

  val streamTests = suite("test with client")(
    peopleAge
  ).provideCustomLayerShared(AsyncHttpClientZioBackend.layer() ++ Clock.live ++ serverLayer2(agePeople)).mapError(TestFailure.fail)

  override def spec = streamTests

}
