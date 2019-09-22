package zsx.events

import zio.{IO, ZIO}
import zio.clock.Clock
import Events.ChillEvent
import zio.random.Random
import zio.stream.ZStream
import zsx.events.Generators.EventGenerator


object ChillEventStream {
  // final def unfoldM[R, E, A, S](s: S)(f0: S => ZIO[R, E, Option[(A, S)]]): ZStream[R, E, A] =

  def generatedStream[S](initialState: S, generator: EventGenerator[S])
  : ZIO[Clock with Random, Nothing, ZStream[Clock with Random, Nothing, ChillEvent]] = {
    IO.succeed(ZStream.unfoldM(initialState)(generator.generate))
  }


}
