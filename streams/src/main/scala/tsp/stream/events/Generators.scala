package tsp.stream.events

import java.time.Instant

import tsp.stream.events.Events.{ChillEvent, Temperature}
import zio._
import zio.clock.Clock
import zio.clock.Clock.Live
import zio.random.Random

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
    def generate(s: S): ZIO[ZEnv, Nothing, (ChillEvent, S)]
  }

  def generateOpt[S](eg: EventGenerator[S])(s: S): ZIO[ZEnv, Nothing, Option[(ChillEvent, S)]] =
    eg.generate(s).map { res =>
    println(s"generated $res")
      Some(res)
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

  def update(s: SimpleEventState, newTemp: Temperature) =
    now.map { currentTime =>
      val newEvent = s.chillEvent.copy(temperature = s.chillEvent.temperature + newTemp,
        gps = s.chillEvent.gps.copy(at = currentTime))
      val newState = s.copy(chillEvent = newEvent)
      (newEvent, newState)
    }

  /**
   * Temperature is a random walk where each step moves us up or down randomly within range
   * +/- variation
   */
  def randomWalkGenerator(variation: Double): EventGenerator[SimpleEventState] = { s =>

    for {
      random <- getRandom
      d <- random.nextDouble
      r = (d * 2 - 1) * variation
      updated <- update(s, r)

    } yield updated
  }

  def centeringRandomWalkGenerator(variation: Double, centre: Double, by: Double): EventGenerator[SimpleEventState] = { s =>
    for {
      random <- getRandom
      d1a <- random.nextDouble
      d1 = d1a * 2 - 1
      d2 <- random.nextDouble
      rawAmount = if (d2 < by) {
        // we want to move towards centre
        val direction = if (s.chillEvent.temperature > centre) -1 else 1
        Math.abs(d1) * direction
      } else d1
      //_ = println(s"d1 $d1 d2 $d2 rawAmount $rawAmount")
      adjustedAmount = rawAmount * variation
      updated <- update(s, adjustedAmount)
    } yield updated

  }

}
