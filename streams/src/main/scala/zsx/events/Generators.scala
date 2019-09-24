package zsx.events

import java.time.Instant

import zio.{IO, ZIO}
import zio.clock.Clock
import zio.random.Random
import zsx.events.Events.ChillEvent

/**
 * Collection of things to generate values for the chill event. Used in streams.
 * State contains whatever is necessary to retain for generating the next value
 */
object Generators {
  /**
   * convenience extractor from the ZIO environment R
   */
  def getClock: ZIO[Clock, Nothing, Clock.Service[Any]] =
    ZIO.access(_.clock)

  /**
   * convenience extractor from the ZIO environment R
   */
  def getRandom: ZIO[Random, Nothing, Random.Service[Any]] =
    ZIO.access(_.random)

  /**
   * typeclass for generating a state S and ChillEvent from a previous state
   */
  trait EventGenerator[S] {
    def generate(s: S): ZIO[Clock with Random, Nothing, Option[(ChillEvent, S)]]
  }

  /**
   * gets current instant from zio clock (which could be mocked)
   */
  def now: ZIO[Clock, Nothing, Instant] =
    for {
      clock <- getClock
      offs <- clock.currentDateTime
    } yield offs.toInstant


  /**
   * for when the only state we need is actually the previous ChillEvent
   */
  case class SimpleEventState(chillEvent: ChillEvent)

  /**
   * Temperature is a random walk where each step moves us up or down randomly within range
   * +/- variation
   */
  def randomWalkGenerator(variation: Double): EventGenerator[SimpleEventState] = { s =>
    import s._
    for {
      newInst <- now
      random <- getRandom
      d <- random.nextDouble
    } yield {
      val r = (d * 2 - 1) * variation
      val newEvent = s.chillEvent.copy(temperature = s.chillEvent.temperature + r,
        gps = s.chillEvent.gps.copy(time = newInst))
      val newState = s.copy(chillEvent = newEvent)
      Some(( newEvent, newState))
    }
  }

}
