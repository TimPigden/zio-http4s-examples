package tsp.stream.events

import tsp.stream.events.Generators._
import zio._
import zio.clock.Clock
import zio.duration.Duration
import zio.stream.ZStream
import Events._
import java.time.{Duration => JDuration}

object EventStreams {
  // final def unfoldM[R, E, A, S](s: S)(f0: S => ZIO[R, E, Option[(A, S)]]): ZStream[R, E, A] =

  def generatedStream[Evt, S](initialState: S, generator: EventGenerator[Evt, S], timing: JDuration) =
    ZStream.unfoldM(initialState)(generator.generate)
    .schedule(ZSchedule.spaced(Duration.fromJava(timing)))

  def randomEventDelayStream[Evt <: Event](inStream: ZStream[ZEnv with Clock, Nothing, Evt]) =
    inStream.mapM { ev =>
      randomDuration(JDuration.ofMillis(10), JDuration.ofSeconds(10)).map { d =>
        val receivedTime = ev.at.plus(d)
        ReceivedEvent(ev, receivedTime)
      }
    }

}
