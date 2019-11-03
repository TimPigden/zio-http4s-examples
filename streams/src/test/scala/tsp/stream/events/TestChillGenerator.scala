package tsp.stream.events

import tsp.stream.events.Events.{ChillEvent, ReceivedEvent}
import tsp.stream.events.Generators.{Delay, EventGenerator}
import zio.test.Assertion._
import zio.test._
import zio._
import zio.duration.Duration
import zio.stream.Sink
import zio.test.environment.{Live, TestClock, TestEnvironment}
import java.time.{Duration => JDuration}

import Support._

object TestChillGenerator extends DefaultRunnableSpec (

  suite("test emitting stream")(
/*    testM("random walk"){
      for {
        initialState <- Support.initialiseState
        _ <- fastTime(JDuration.ofSeconds(10), JDuration.ofMillis(20))
        randomWalker = Generators.centeringRandomWalkGenerator(1, -10, 0.1)
        stream = ChillEventStream.generatedStream(initialState, randomWalker, JDuration.ofMinutes(1)).take(20)
        sink = Sink.collectAll[ChillEvent]
        runner <- stream.run(sink)
        _ <- Live.live(console.putStrLn(s"${runner.mkString("\n")}"))
      } yield {
        assert(runner.size, equalTo(20))
      }
    }
  ,
  testM("stream received works"){
    for {
      initialState <- Support.initialiseState
      _ <- fastTime(JDuration.ofSeconds(10), JDuration.ofMillis(20))

      stream1 = ChillEventStream.generatedStream(initialState, Support.randomWalker, JDuration.ofSeconds(60)).take(20)
      stream = ChillEventStream.randomEventDelayStream(stream1)
      sink = Sink.collectAll[ReceivedEvent[ChillEvent]]
      runner <- stream.run(sink)
      _ <- Live.live(console.putStrLn(s"${runner.mkString("\n")}"))
    } yield {
      assert(runner.size, equalTo(20))
    }
  },*/
/*
    testM("run delay"){
      for {
        _ <- fastTime(JDuration.ofSeconds(10), JDuration.ofMillis(20))
        initialState = Delay(JDuration.ofMillis(0))
        delayer = Generators.delayGenerator(howOften = JDuration.ofMinutes(10),
          variation = JDuration.ofSeconds(30),
          standardDelay = JDuration.ofMillis(10),
          sampleFrequency = JDuration.ofSeconds(20)
        )
        stream = EventStreams.generatedStream(initialState, delayer, JDuration.ofMinutes(1)).take(20)
        sink = Sink.collectAll[Delay]
        runner <- stream.run(sink)
        _ <- Live.live(console.putStrLn(s"${runner.mkString("\n")}"))
      } yield {
        assert(runner.size, equalTo(20))
      }
    },
*/

    testM("run delayed chills"){
      for {
        _ <- fastTime(JDuration.ofSeconds(10), JDuration.ofMillis(20))
        initialDelay = Delay(JDuration.ofMillis(0))
        initialChill <- Support.initialiseState
        delayer = Generators.delayGenerator(howOften = JDuration.ofMinutes(10),
          variation = JDuration.ofSeconds(30),
          standardDelay = JDuration.ofMillis(10),
          sampleFrequency = JDuration.ofSeconds(20)
        )
        randomWalker = Generators.centeringRandomWalkGenerator(1, -10, 0.1)
        delayStream = EventStreams.generatedStream(initialDelay, delayer, JDuration.ofMinutes(1))
        chillStream = EventStreams.generatedStream(initialChill, randomWalker, JDuration.ofMinutes(1))
        receivedEvents = delayStream.zip(chillStream).map { pair =>
          ReceivedEvent(pair._2, pair._2.at.plus(pair._1.amount))
        }.take(40)
        sink = Sink.collectAll[ReceivedEvent[ChillEvent]]
        runner <- receivedEvents.run(sink)
        _ <- Live.live(console.putStrLn(s"${runner.mkString("\n")}"))
      } yield {
        assert(runner.size, equalTo(20))
      }
    }
  )
)

object Support {
  val randomWalker: EventGenerator[ChillEvent, ChillEvent] = Generators.centeringRandomWalkGenerator(1, -10, 0.1)

  def initialiseState =
    Generators.now.map { nw =>
      ChillEvent("vehicle1", -18.0, nw) }

  def fastTime(testIntervals: JDuration, liveIntervals: JDuration) =
    Live.withLive(TestClock.adjust(Duration.fromJava(testIntervals)))(
    _.repeat(Schedule.spaced(Duration.fromJava(liveIntervals)))).fork


}
