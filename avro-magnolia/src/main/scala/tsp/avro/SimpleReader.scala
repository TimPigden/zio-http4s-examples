package tsp.avro

import java.nio.ByteBuffer
import java.time._

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.util.Utf8

import scala.collection.JavaConverters._
import SimpleSchemaGenerator._
import tsp.avro.Utils.ZoneInstant

trait SimpleReader extends SimpleReader1 {

  implicit val byteArrayAvroReader: AvroReader[Array[Byte]] = { (_, ref) =>
    ref match {
      case bb: ByteBuffer =>
        bb.array()
    }
  }

  implicit val floatArrayAvroReader: AvroReader[Array[Float]] = { (_, ref) => ref match {
    case bb: ByteBuffer =>
      val flts = bb.asFloatBuffer()
      val lim = flts.limit()
      val out = Array.ofDim[Float](lim)
      0.until(lim).foreach( i => out(i) = flts.get(i))
      out
    }
  }
  implicit val intArrayAvroReader: AvroReader[Array[Int]] = { (_, ref) => ref match {
    case bb: ByteBuffer =>
      val flts = bb.asIntBuffer()
      val lim = flts.limit()
      val out = Array.ofDim[Int](lim)
      0.until(lim).foreach( i => out(i) = flts.get(i))
      out
  }
  }
  implicit val doubleArrayAvroReader: AvroReader[Array[Double]] = { (_, ref) => ref match {
    case bb: ByteBuffer =>
      val flts = bb.asDoubleBuffer()
      val lim = flts.limit()
      val out = Array.ofDim[Double](lim)
      0.until(lim).foreach( i => out(i) = flts.get(i))
      out
  }
  }
  implicit val longArrayAvroReader: AvroReader[Array[Long]] = { (_, ref) => ref match {
    case bb: ByteBuffer =>
      val flts = bb.asLongBuffer()
      val lim = flts.limit()
      val out = Array.ofDim[Long](lim)
      0.until(lim).foreach( i => out(i) = flts.get(i))
      out
  }
  }

}

trait SimpleReader1 {
  implicit val intAvroReader: AvroReader[Int] = { (_, ref) => ref match {
    case v: Integer => v.intValue
  }}

  implicit val longAvroReader: AvroReader[Long] = { (_, ref) => ref match {
    case v: java.lang.Long => v
  }
  }

  implicit val doubleAvroReader: AvroReader[Double] = { (_, ref) => ref match {
    case v: java.lang.Double => v
  }
  }

  implicit val floatAvroReader: AvroReader[Float] = { (_, ref) => ref match {
    case v: java.lang.Float => v
  }
  }

  implicit val stringAvroReader: AvroReader[String] = { (_, ref) => ref match {
    case utf8: Utf8 => utf8.toString
    case v: String => v
  }
  }

  implicit val booleanAvroReader: AvroReader[Boolean] = { (_, ref) => ref match {
    case v: java.lang.Boolean => v
  }
  }

  def iterableAvroReader[T](implicit tReader: AvroReader[T]): AvroReader[Iterable[T]] = { (schema, ref) =>
    val elType = schema.getElementType
    ref match {
      case ar: GenericData.Array[_] =>
        ar.asScala.map {
          case ref: AnyRef => tReader.read(elType, ref)
        }
    }
  }

  implicit def listAvroReader[T](implicit tReader: AvroReader[T]): AvroReader[List[T]] = new AvroReader[List[T]] {
    private val iterableReader = iterableAvroReader[T]
    override def read(schema: Schema, data: AnyRef): List[T] = iterableReader.read(schema, data).toList
  }

  implicit def setAvroReader[T](implicit tReader: AvroReader[T]): AvroReader[Set[T]] = new AvroReader[Set[T]] {
    private val iterableReader = iterableAvroReader[T]
    override def read(schema: Schema, data: AnyRef): Set[T] = iterableReader.read(schema, data).toSet
  }

  def stringKMapAvroReader[K <: String, T](implicit tReader: AvroReader[T], toK: String => K): AvroReader[Map[K, T]] = { (schema, ref) =>
    val elType = schema.getValueType
    ref match {
      case ar: java.util.Map[_, _] =>
        ar.asScala.map { p =>
          val t = p._2 match {
            case ref: AnyRef => tReader.read(elType, ref)
          }
          val k = p._1 match {
            case s: String => toK(s)
          }
          k -> t
        }.toMap
    }
  }

  def stringMapAvroReader[T](implicit tReader: AvroReader[T]): AvroReader[Map[String, T]] = new AvroReader[Map[String, T]] {
    private val internal = stringKMapAvroReader[String, T](tReader, x => x)

    override def read(schema: Schema, data: AnyRef): Map[String, T] = internal.read(schema, data)
  }

  def kvMapAvroReader[K, T](implicit kReader: AvroReader[K], tReader: AvroReader[T]): AvroReader[Map[K, T]] = new AvroReader[Map[K, T]] {
    private val tupReader = AvroReaderDerivation.avroReader[(K, T)]
    private val itReader = iterableAvroReader(tupReader)

    override def read(schema: Schema, ref: AnyRef): Map[K, T] = itReader.read(schema, ref).toMap
  }

  implicit def optionReader[T](implicit tReader: AvroReader[T]): AvroReader[Option[T]] = { (schema, data) =>
    Option(data).map { d => tReader.read(schema, d) }
  }
  
  implicit val instantReader: AvroReader[Instant] = { (schema, data) =>
    val instantL = longAvroReader.read(schema, data)
    Instant.ofEpochMilli(instantL)
  }

  implicit val localDateReader: AvroReader[LocalDate] = { (schema, data) =>
    val localDateL = longAvroReader.read(schema, data)
    LocalDate.ofEpochDay(localDateL)
  }
  implicit val localDateTimeReader: AvroReader[LocalDateTime] = { (schema, data) =>
    val instantL = longAvroReader.read(schema, data)
    val inst = Instant.ofEpochMilli(instantL)
    LocalDateTime.ofInstant(inst, Utils.utcId)
  }
  implicit val localTimeReader: AvroReader[LocalTime] = { (schema, data) =>
    val localTimeL = longAvroReader.read(schema, data)
    LocalTime.ofNanoOfDay(localTimeL * 1000000L)
  }

  implicit val zonedDateTimeReader: AvroReader[ZonedDateTime] = new AvroReader[ZonedDateTime] {
    private val zoneInstantReader = AvroReaderDerivation.avroReader[ZoneInstant]

    override def read(schema: Schema, data: AnyRef): ZonedDateTime =
      zoneInstantReader.read(schema, data).toZonedDateTime
  }

}

object SimpleReader extends SimpleReader