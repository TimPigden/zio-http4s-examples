package zsx.events

import java.time.Instant

object Events {

  case class LongLat(longitude: Double, latitude: Double)
  case class GPS(time: Instant, longLat: LongLat, bearing: Double)
  case class ChillEvent(vehicleId: String, temperature: Double, gps: GPS)

  case class ReceivedEvent(chillEvent: ChillEvent, receivedAt: Instant)
}
