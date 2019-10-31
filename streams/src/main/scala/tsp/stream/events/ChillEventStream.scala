package tsp.stream.events

import tsp.stream.events.Generators._
import zio.Schedule
import zio.duration.Duration
import zio.stream.ZStream
import scala.concurrent.duration.{Duration => ScalaDuration, _}


object ChillEventStream {
  // final def unfoldM[R, E, A, S](s: S)(f0: S => ZIO[R, E, Option[(A, S)]]): ZStream[R, E, A] =

  def generatedStream[S](initialState: S, generator: EventGenerator[S]) =
    ZStream.unfoldM(initialState)(generateOpt(generator))
    .schedule(Schedule.spaced(Duration.fromScala(1.seconds)))

}
