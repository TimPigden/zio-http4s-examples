package tsp.stream.events

import tsp.stream.events.Events.{ChillEvent, ReceivedEvent}
import tsp.stream.events.Generators.SimpleEventState
import zio.test.Assertion._
import zio.test._
import zio._
import zio.duration.Duration
import zio.stream.Sink
import zio.test.environment.{Live, TestClock, TestEnvironment}

import scala.concurrent.duration.{Duration => ScalaDuration, _}

object TestChillGenerator extends DefaultRunnableSpec (

  suite("test emititing stream")(
/*    testM("fold works") {
      val nItems = 200
      for {
        initialState <- Support.initialiseState
        folded <- Support.foldIt(initialState, nItems)
        _ = println(folded.map(_.toString).mkString("\n"))
      } yield {
        assert(folded.size, equalTo(nItems))
      }
      },*/
/*    testM("stream works"){
      for {
        initialState <- Support.initialiseState
        _ <- Live.withLive(TestClock.adjust(Duration.fromScala(10.seconds)))(
          _.repeat(Schedule.spaced(Duration.fromScala(10.millis)))).fork

        stream = ChillEventStream.generatedStream(initialState, Support.randomWalker).take(20)
        sink = Sink.collectAll[ChillEvent]
        runner <- stream.run(sink)
      } yield {
        assert(runner.size, equalTo(20))
      }
    }

    ),*/
  testM("stream received works"){
    for {
      initialState <- Support.initialiseState
      _ <- Live.withLive(TestClock.adjust(Duration.fromScala(10.seconds)))(
        _.repeat(Schedule.spaced(Duration.fromScala(10.millis)))).fork

      stream1 = ChillEventStream.generatedStream(initialState, Support.randomWalker).take(20)
      stream = ChillEventStream.receivedStream(stream1)
      sink = Sink.collectAll[ReceivedEvent[ChillEvent]]
      runner <- stream.run(sink)
    } yield {
      assert(runner.size, equalTo(20))
    }
  }

)




)

object Support {
  val randomWalker = Generators.centeringRandomWalkGenerator(1, -10, 0.1)

  def initialiseState =
    Generators.now.map { nw =>
      SimpleEventState(ChillEvent("vehicle1", -18.0, nw)) }

}
