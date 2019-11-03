package tsp.stream.events

import java.time.Instant

object Events {

  type Temperature = Double

  trait Event {
    def at: Instant
  }

  case class ReceivedEvent[Evt](event: Evt, receivedAt: Instant) extends Event {
    override def at = receivedAt
  }

  case class ChillEvent(vehicleId: String, temperature: Temperature, at: Instant) extends Event

}
