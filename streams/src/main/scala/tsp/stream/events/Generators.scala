package tsp.stream.events

import java.time.Instant

import tsp.stream.events.Events.{ChillEvent, Event, Temperature}
import zio._
import zio.clock.Clock
import zio.clock.Clock.Live
import zio.duration.Duration
import zio.random.Random
import zio.stream.ZStream
import java.time.{Duration => JDuration}
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

  def randomDouble =
    for {
      r <- getRandom
      f <- r.nextDouble
    } yield f

  def randomDuration(lower: JDuration, upper: JDuration) =
    randomDouble.map { d =>
      val diff = upper.minus(lower).toMillis
      val adjusted = (diff * d).toLong
      lower.plusMillis(adjusted)
    }

  /**
   * typeclass for generating a state S and an event from a previous state
   */
  trait EventGenerator[Evt, S] {
    def generate(s: S): ZIO[ZEnv, Nothing, Option[(Evt, S)]]
  }

  def generatedStream[Evt, S](initialState: S, generator: EventGenerator[Evt, S], every: Duration) =
    ZStream.unfoldM(initialState)(generator.generate)
      .schedule(ZSchedule.spaced(every))

  /**
   * gets current instant from zio clock (which could be mocked)
   */
  def now: ZIO[Clock, Nothing, Instant] =
    for {
      clock <- getClock
      offs <- clock.currentDateTime
    } yield offs.toInstant

  /**
   * creates an EventGenerator implementing biased random walk. Note this is not
   * intended to emulate real refrigeration units - just give us some numbers to play with
   * @param variation maximum we should change at each tick
   * @param centre target temperature to which we are biased
   * @param bias number in range 0 - 1 representing the proportion
   *             of ticks at which we attempt to move towards the centre
   */
  def centeringRandomWalkGenerator(variation: Double, centre: Double, bias: Double): EventGenerator[ChillEvent, ChillEvent] = { s =>
    for {
      random <- getRandom
      d1a <- random.nextDouble
      d1 = d1a * 2 - 1
      d2 <- random.nextDouble
      rawAmount = if (d2 < bias) {
        // we want to move towards centre
        val direction = if (s.temperature > centre) -1 else 1
        Math.abs(d1) * direction
      } else d1
      adjustedAmount = rawAmount * variation + s.temperature
      nw <- now // gets the current time from, the clock
      newEvent = s.copy(temperature = adjustedAmount, at = nw)
    } yield Some((newEvent, newEvent))
  }

  case class Delay(amount: JDuration)

  /**
   * arbitrarily generate delays or reduce arrays already there
   * @param howOften how often do we actually get delays
   * @param variation max delay we want to create
   * @param standardDelay all messages are delayed by a few milliseconds
   * @param sampleFrequency how frequently our source main stream set is generating events
   */
  def delayGenerator(howOften: JDuration,
                     variation: JDuration,
                     standardDelay: JDuration,
                     sampleFrequency: JDuration): EventGenerator[Delay, Delay] =
    new EventGenerator[Delay, Delay] {
      private val sampleFrequencyMillis = sampleFrequency.toMillis
      private val howOftenDbl = sampleFrequency.toMillis.toDouble / howOften.toMillis // we will use this to get probability of introducing new delay
      private val standardDelayMillis = standardDelay.toMillis

      override def generate(s: Delay): ZIO[zio.ZEnv, Nothing, Option[(Delay, Delay)]] = {
        val sMillis = s.amount.toMillis
        val newMillis = if (sMillis > sampleFrequency.toMillis)
          IO.succeed(JDuration.ofMillis(sMillis - sampleFrequency.toMillis))
        else if (sMillis > standardDelayMillis)
          IO.succeed(standardDelay)
        else for {
          r <- randomDouble
          newDelay <- if (r > howOftenDbl) // we don't want a new one
            IO.succeed(standardDelay)
          else randomDuration(standardDelay, variation)
        } yield newDelay
        newMillis.map { nd =>
          Some((Delay(nd), Delay(nd)))
        }
      }
    }
}
