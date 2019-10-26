package tsp.avro

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneId, ZoneOffset, ZonedDateTime}

import org.json4s.JsonAST.{JArray, JDouble, JInt, JString, JValue}
import org.json4s.JsonDSL._
import tsp.avro.Utils.ZoneInstant

trait SimpleSchemaGenerator {
  implicit def intAvroSchema: SchemaGenerator[Int] = new SchemaGenerator[Int] {
    override def generate = JString("int")
  }

  implicit def longAvroSchema: SchemaGenerator[Long] = new SchemaGenerator[Long] {
    override def generate: JValue = JString("long")
  }

  implicit def doubleAvroSchema: SchemaGenerator[Double] = new SchemaGenerator[Double] {
    override def generate: JValue = JString("double")
  }

  implicit def floatAvroSchema: SchemaGenerator[Float] = new SchemaGenerator[Float] {
    override def generate: JValue = JString("float")
  }

  implicit def stringAvroSchema: SchemaGenerator[String] = new SchemaGenerator[String] {
    override def generate: JValue = JString("string")
  }

  implicit val bytes: SchemaGenerator[Iterable[Byte]] = new SchemaGenerator[Iterable[Byte]] {
    override def generate: JValue = JString("bytes")
  }

  implicit def boolean: SchemaGenerator[Boolean] = new SchemaGenerator[Boolean] {
    override def generate: JValue = JString("boolean")
  }

  /**
   * explicit array ops for fast primitives
   */
  implicit def doubleArraySchema[T <: Double]: SchemaGenerator[Array[T]] = new SchemaGenerator[Array[T]] {
    override def generate: JValue = JString("bytes")
  }

  implicit def floatArraySchema[T <: Float]: SchemaGenerator[Array[T]] = new SchemaGenerator[Array[T]] {
    override def generate: JValue = JString("bytes")
  }

  implicit def intArraySchema[T <: Int]: SchemaGenerator[Array[T]] = new SchemaGenerator[Array[T]] {
    override def generate: JValue = JString("bytes")
  }

  implicit def longArraySchema[T <: Long]: SchemaGenerator[Array[T]] = new SchemaGenerator[Array[T]] {
    override def generate: JValue = JString("bytes")
  }

  implicit def byteArraySchema[T <: Byte]: SchemaGenerator[Array[T]] = new SchemaGenerator[Array[T]] {
    override def generate: JValue = JString("bytes")
  }

  def iterableSchema[T](implicit tGenerator: SchemaGenerator[T]): SchemaGenerator[Iterable[T]] = new SchemaGenerator[Iterable[T]] {
    override def generate: JValue =
      ("type" -> "array") ~
        ("items" -> tGenerator.generate)
  }

  implicit def listSchema[T](implicit tGenerator: SchemaGenerator[T]): SchemaGenerator[List[T]] = new SchemaGenerator[List[T]] {
    override def generate: JValue = iterableSchema[T].generate
  }

  implicit def setSchema[T](implicit tGenerator: SchemaGenerator[T]): SchemaGenerator[Set[T]] = new SchemaGenerator[Set[T]] {
    override def generate: JValue = iterableSchema[T].generate
  }

  def stringMapSchema[K <: String, T](implicit tGenerator: SchemaGenerator[T]): SchemaGenerator[Map[K, T]] =
    new SchemaGenerator[Map[K, T]] {
      override def generate: JValue = ("type" -> "map") ~ ("values" -> tGenerator.generate)
    }

  def kvMapSchema[K, T](implicit kGenerator: SchemaGenerator[K], tGenerator: SchemaGenerator[T]): SchemaGenerator[Map[K, T]] =
    new SchemaGenerator[Map[K, T]] {
      private val tupleGenerator = AvroSchemaDerivation.avroSchema[(K, T)]
      private val tupleIterator = iterableSchema(tupleGenerator)

      override def generate: JValue = tupleIterator.generate
    }

  implicit def optionSchema[T](implicit tGenerator: SchemaGenerator[T]): SchemaGenerator[Option[T]] = new SchemaGenerator[Option[T]] {
    override def generate: JValue = JArray(List("null", tGenerator.generate))
  }

  implicit val instantSchema: SchemaGenerator[Instant] = new SchemaGenerator[Instant] {
    override def generate: JValue = longAvroSchema.generate
  }

  implicit val localDateSchema: SchemaGenerator[LocalDate] = new SchemaGenerator[LocalDate] {
    override def generate: JValue = longAvroSchema.generate
  }

  implicit val localDateTimeSchema: SchemaGenerator[LocalDateTime] = new SchemaGenerator[LocalDateTime] {
    override def generate: JValue = longAvroSchema.generate
  }

  implicit val localTimeSchema: SchemaGenerator[LocalTime] = new SchemaGenerator[LocalTime] {
    override def generate: JValue = longAvroSchema.generate
  }
  

  implicit val zonedDateTimeSchema: SchemaGenerator[ZonedDateTime] = new SchemaGenerator[ZonedDateTime] {
    private val zoneInstantSchema = AvroSchemaDerivation.avroSchema[ZoneInstant]
    override def generate: JValue = zoneInstantSchema.generate
  }

}

object SimpleSchemaGenerator extends SimpleSchemaGenerator