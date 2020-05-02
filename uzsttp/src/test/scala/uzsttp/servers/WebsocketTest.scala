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
import uzsttp.websocket.PersonStream
import uzsttp.websocket.PersonStream._
import zio.clock.Clock
import zio.stream.{Take, ZStream}

object WebsocketTest extends DefaultRunnableSpec {

  def sendPerson(person: Person, ws: WebSocket[Task]) = {
    println(s"sending person $person")
    ws.send(WebSocketFrame.text(writeXmlString(person)))
  }

  def next(ws: WebSocket[Task]): Task[Option[Person]] = {
    for {
      et <- ws.receiveText()
      personOpt <- et match {
        case Right(t) => parseXmlString(t).map(Some(_))
        case _ => IO.succeed(None)
      }
    } yield personOpt
  }

  val ageOneAtATime = testM("test age people"){

    for {
      _ <- serverUp
      response <- SttpClient.openWebsocket(basicRequest.get(uri"ws://localhost:8080/wsPersonOneByOne"))
      _ = println(s"response is $response")
      ws = response.result
      sent <- sendPerson(joe, ws)
      joeOlder <- next(ws)
    } yield assert(joeOlder)(equalTo(Some(older(joe))))
  }


  def asStream(ws: WebSocket[Task]): Task[ZStream[Any, Nothing, Take[Nothing, Person]]] = {

    def processQueue(q: Queue[Take[Nothing, Person]]): Task[Boolean] =
      for {
        ws <- next(ws)
        ended <- ws match {
          case None =>
            for {
              _ <- q.offer(Take.End)
              _ <- UIO(println(s"got end of stream"))
            } yield true
          case Some(person) =>
            for {
              _ <- UIO(println(s"got person $person"))
              _ <- q.offer(Take.Value(person))
            } yield false
        }
      } yield ended

    for {
      q <- Queue.unbounded[Take[Nothing,Person]]
      f = ZStream.fromQueueWithShutdown(q)
      _ <- processQueue(q).repeat(Schedule.doUntil(bool => bool)).fork
    } yield f
  }

  val emptyStream = testM("calls, expecting an empty stream") {
    for {
      _ <- serverUp
      response <- SttpClient.openWebsocket(basicRequest.get(uri"ws://localhost:8080/wsPerson"))
      _ = println(s"response is $response")
      ws = response.result
      sent <- sendPerson(joe.copy(age = 101), ws)
      agingPeople <- asStream(ws).map(_.unTake).map(_.takeWhile(_ != DEATH))
      allPeople <- agingPeople.runCollect
      _ <- ws.close
    } yield assert(allPeople.size)(equalTo(0))
  }

  val nonEmptyStream = testM("calls, expecting a non-empty stream") {
    for {
      _ <- serverUp
      response <- SttpClient.openWebsocket(basicRequest.get(uri"ws://localhost:8080/wsPerson"))
      _ = println(s"response is $response")
      ws = response.result
      sent <- sendPerson(joe.copy(age = 78), ws)
      agingPeople <- asStream(ws).map(_.unTake).map(_.takeWhile(_ != DEATH))
      _ <- ws.close
      allPeople <- agingPeople.runCollect
      //_ <- UIO(println(s"all people ${allPeople.mkString("\n")}"))
    } yield assert(allPeople.size)(equalTo(23))
  }

  val cutShortInfiniteStream = testM("cut short infinite stream") {
    for {
      _ <- serverUp
      response <- SttpClient.openWebsocket(basicRequest.get(uri"ws://localhost:8080/wsPerson"))
      _ = println(s"response is $response")
      ws = response.result
      sent <- sendPerson(joe.copy(age = 1), ws)
      agingPeople <- asStream(ws).map(_.unTake).map(_.take(200))
      _ <- ws.close
      allPeople <- agingPeople.runCollect
    } yield assert(allPeople.size)(equalTo(200))
  }



  def endPoints = ZIO.access[Clock](_.get).map { clk =>
    val ps = PersonStream(clk)
    EndPoint.combineRoutes(ps.agePerson, agePersonByOne)
  }


  val streamTests = suite("test with client")(
    ageOneAtATime,
    emptyStream,
    nonEmptyStream,
    cutShortInfiniteStream,
  ).provideCustomLayerShared(AsyncHttpClientZioBackend.layer() ++ Clock.live ++ serverLayer2M(endPoints)).mapError(TestFailure.fail)

  override def spec = streamTests

}
