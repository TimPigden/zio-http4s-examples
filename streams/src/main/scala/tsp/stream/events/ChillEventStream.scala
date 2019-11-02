package tsp.stream.events

import tsp.stream.events.Generators._
import zio._
import zio.clock.Clock
import zio.duration.Duration
import zio.stream.ZStream
import Events._
import scala.concurrent.duration.{Duration => ScalaDuration, _}


object ChillEventStream {
  // final def unfoldM[R, E, A, S](s: S)(f0: S => ZIO[R, E, Option[(A, S)]]): ZStream[R, E, A] =

  def generatedStream[S](initialState: S, generator: EventGenerator[S]) =
    ZStream.unfoldM(initialState)(generateOpt(generator))
    .schedule(Schedule.spaced(Duration.fromScala(60.seconds)))

  def receivedStream[E](inStream: ZStream[ZEnv with Clock, Nothing, ChillEvent]) =
    inStream.mapM { ev =>
      now.map { nw =>
        val re = ReceivedEvent(ev, nw)
        println(s"outstream event $re")
        re
      }
    }


}
