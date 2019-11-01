package tsp.stream.events

import tsp.stream.events.Events.SimpleEvent
import zio._
import zio.stream.{Sink, ZStream}
import zio.test._
import zio.test.Assertion._

import scala.concurrent.duration.{Duration => ScalaDuration, _}
import PlayWithTimeBits._
import zio.clock.Clock
import zio.duration.Duration
import scala.concurrent.duration.{Duration => ScalaDuration, _}

import zio.test.environment.{Live, TestClock, TestEnvironment}
import zio.test.mock.MockClock

object PlayWithTime extends DefaultRunnableSpec(
  suite("timings")
/*
    (testM("first attempt, does nothing"){
      val stream = myStream.take(30)
      val sink = Sink.collectAll[SimpleEvent]
      for {
        runner <- stream.run(sink)
      } yield assert(runner.size, equalTo(20))
    }
  )
*/

  (testM("sepaate ticker"){
    val stream = myStream.take(30)
    val sink = Sink.collectAll[SimpleEvent]
    for {
      ticker <- Live.withLive(TestClock.adjust(Duration.fromScala(1.seconds)))(_.repeat(Schedule.spaced(Duration.fromScala(10.millis)))).fork
      runner <- stream.run(sink)
    } yield assert(runner.size, equalTo(30))
  }
  )

)

object PlayWithTimeBits
{
  def myStream = ZStream.repeatEffect(
    for {
      at <- Generators.now
      _ <- Live.live(console.putStrLn(s"at $at"))
      } yield SimpleEvent(at)
    )
    .schedule(Schedule.spaced(Duration.fromScala(10.seconds)))

  def ticks(testClock: TestClock.Test, millisPerSecond: Int) =
    testClock.adjust(Duration.fromScala(1.seconds)).repeat(Schedule.spaced(Duration.fromScala(millisPerSecond.millis)))

}



