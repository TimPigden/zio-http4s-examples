package tsp.avro

import java.time.{Instant, ZoneId, ZonedDateTime}

object Utils {

  case class ZoneInstant(zoneIdS: String, instant: Instant) {
    def toZonedDateTime: ZonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of(zoneIdS))
  }

  lazy val utcId = ZoneId.of("UTC")

}
