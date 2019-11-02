package tsp.stream.events

import java.time.Instant

object Events {

  type Temperature = Double

  trait Event {
    def at: Instant
  }
  case class ReceivedEvent[Evt](event: Evt, receivedAt: Instant)

  case class ChillEvent(vehicleId: String, temperature: Temperature, at: Instant)

}
