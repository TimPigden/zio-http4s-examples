package tsp.stream.events

import java.time.Instant

import zio._
import zio.stream.{Sink, ZStream}
import zio.test._
import zio.test.Assertion._

import scala.concurrent.duration.{Duration => ScalaDuration, _}
import SpeedingUpTimeHelper._
import zio.clock.Clock
import zio.duration.Duration
import zio.test.environment.{Live, TestClock, TestEnvironment}

object SpeedingUpTime extends DefaultRunnableSpec(
  suite("timings")(
/*    testM("sepaate ticker"){
      val stream = myStream.take(30)
      val sink = Sink.collectAll[SimpleEvent]
      for {
        runner <- stream.run(sink)
      } yield assert(runner.size, equalTo(30))
    }
  ),*/

    testM("sepaate ticker"){
      val stream = myStream.take(30)
      val sink = Sink.collectAll[SimpleEvent]
      for {
        _ <- Live.withLive(TestClock.adjust(Duration.fromScala(1.seconds)))(
          _.repeat(ZSchedule.spaced(Duration.fromScala(10.millis)))).fork
//        _ <- TestClock.adjust(Duration.fromScala(1.seconds))
        //        .repeat(Schedule.recurs(300)).fork
        runner <- stream.run(sink)
      } yield assert(runner.size, equalTo(30))
    }
  )
)

object SpeedingUpTimeHelper
{
  case class SimpleEvent(at: Instant)

  def myStream = ZStream.repeatEffect(
    for {
      at <- ZIO.accessM[Clock](_.clock.currentDateTime)
      evt = SimpleEvent(at.toInstant)
      _ <- Live.live(console.putStrLn(s"at $evt"))
    } yield evt
  )
  .schedule(ZSchedule.spaced(Duration.fromScala(10.seconds)))

}



