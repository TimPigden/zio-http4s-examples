package tsp.stream.events

import tsp.stream.events.Events.{ChillEvent, ReceivedEvent}
import tsp.stream.events.Generators.{Delay, EventGenerator}
import zio.test.Assertion._
import zio.test._
import zio._
import zio.duration.Duration
import zio.stream.{Sink, ZStream}
import zio.test.environment.{Live, TestClock, TestEnvironment}
import java.time.{Instant, Duration => JDuration}

import Support._

object TestChillGenerator extends DefaultRunnableSpec (

  suite("test emitting stream")(
    testM("random walk"){
      for {
        nw <- Generators.now // utility method gets current time as instant from Clock
        initialState = ChillEvent("vehicle1", -18.0, nw)
        _ <- fastTime(JDuration.ofSeconds(60), JDuration.ofMillis(10))
        randomWalker = Generators.centeringRandomWalkGenerator(1, -10, 0.1)
        stream = EventStreams.generatedStream(initialState, randomWalker, JDuration.ofMinutes(1)).take(200)
        sink = Sink.collectAll[ChillEvent]
        runner <- stream.run(sink)
        _ <- Live.live(console.putStrLn(s"${runner.mkString("\n")}"))
      } yield {
        assert(runner.size, equalTo(200))
      }
    }
  ,
  testM("stream received works"){
    for {
      initialState <- Support.initialiseVehicle(1)
      _ <- fastTime(JDuration.ofSeconds(10), JDuration.ofMillis(20))

      stream1 = EventStreams.generatedStream(initialState, Support.randomWalker, JDuration.ofSeconds(60)).take(20)
      stream = EventStreams.randomEventDelayStream(stream1)
      sink = Sink.collectAll[ReceivedEvent[ChillEvent]]
      runner <- stream.run(sink)
      _ <- Live.live(console.putStrLn(s"${runner.mkString("\n")}"))
    } yield {
      assert(runner.size, equalTo(20))
    }
  },

    testM("run delay"){
      val initialDelay = Delay(JDuration.ofMillis(0))
      val delayer = Generators.delayGenerator(howOften = JDuration.ofMinutes(10),
        variation = JDuration.ofSeconds(300),
        standardDelay = JDuration.ofMillis(10),
        sampleFrequency = JDuration.ofSeconds(20)
      )
      val delays = ZStream.unfoldM(initialDelay)(delayer.generate).take(200)
      val sink = Sink.collectAll[Delay]
      for {
        runner <- delays.run(sink)
        _ <- Live.live(console.putStrLn(s"${runner.mkString("\n")}"))
      } yield {
        assert(runner.size, equalTo(200))
      }
    },
    testM("run delayed chills"){
      val initialDelay = Delay(JDuration.ofMillis(0))
      val delayer = Generators.delayGenerator(howOften = JDuration.ofMinutes(10),
        variation = JDuration.ofSeconds(300),
        standardDelay = JDuration.ofMillis(10),
        sampleFrequency = JDuration.ofSeconds(20)
      )
      val delays = ZStream.unfoldM(initialDelay)(delayer.generate)
      for {
        _ <- fastTime(JDuration.ofSeconds(10), JDuration.ofMillis(5))
        initialChill <- Support.initialiseVehicle(1)
        randomWalker = Generators.centeringRandomWalkGenerator(1, -10, 0.1)
        chillStream = EventStreams.generatedStream(initialChill, randomWalker, JDuration.ofSeconds(20))
        receivedEvents = delays.zip(chillStream).map { pair =>
          ReceivedEvent(pair._2, pair._2.at.plus(pair._1.amount))
        }.take(200)
        sink = Sink.collectAll[ReceivedEvent[ChillEvent]]
        runner <- receivedEvents.run(sink)
        _ <- Live.live(console.putStrLn(s"${runner.mkString("\n")}"))
      } yield {
        assert(runner.size, equalTo(200))
      }
    },

    testM("multiple vehicles"){
      val initialDelay = Delay(JDuration.ofMillis(0))
      val delayer = Generators.delayGenerator(howOften = JDuration.ofMinutes(10),
        variation = JDuration.ofSeconds(300),
        standardDelay = JDuration.ofMillis(10),
        sampleFrequency = JDuration.ofSeconds(20)
      )
      val randomWalker = Generators.centeringRandomWalkGenerator(1, -10, 0.1)

      def vehicleStream(i: Int, startAt: Instant) = {
        val initialChill = ChillEvent(s"v-$i", -18.0, startAt)
        val chillStream = EventStreams.generatedStream(initialChill, randomWalker, JDuration.ofSeconds(20))
        val delays = ZStream.unfoldM(initialDelay)(delayer.generate)
        delays.zip(chillStream).map { pair =>
            ReceivedEvent(pair._2, pair._2.at.plus(pair._1.amount))
          }
        }

      for {
        _ <- fastTime(JDuration.ofSeconds(10), JDuration.ofMillis(5))
        nw <- Generators.now
        streams = 1.to(20).map { v => vehicleStream(v, nw)}
        combined = ZStream.mergeAllUnbounded()(streams:_*)
        sink = Sink.collectAll[ReceivedEvent[ChillEvent]]
        runner <- combined.take(2000).run(sink)
        _ <- Live.live(console.putStrLn(s"${runner.mkString("\n")}"))
      } yield {
        assert(runner.size, equalTo(2000))
      }
    }
  )
)

object Support {
  val randomWalker: EventGenerator[ChillEvent, ChillEvent] = Generators.centeringRandomWalkGenerator(1, -10, 0.1)

  def initialiseVehicle(i: Int) =
    Generators.now.map { nw =>
      ChillEvent(s"v-$i", -18.0, nw) }

  def fastTime(testIntervals: JDuration, liveIntervals: JDuration) =
    Live.withLive(TestClock.adjust(Duration.fromJava(testIntervals)))(
    _.repeat(ZSchedule.spaced(Duration.fromJava(liveIntervals)))).fork


}
