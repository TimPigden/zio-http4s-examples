package tsp.avro

import java.io.{ByteArrayOutputStream, File}
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZonedDateTime}
import java.util

import org.scalatest.{Assertion, Matchers, WordSpecLike}
import AvroSchemaDerivation._
import AvroReaderDerivation._
import AvroWriterDerivation._
import SimpleSchemaGenerator._
import SimpleWriter._
import SimpleReader._
import org.json4s.jackson.JsonMethods._
import TestSchema._


class TestSchema extends WordSpecLike with Matchers  {
  
  def writeReadBackList[T](protos: List[T])
                      (implicit schemaGenerator: SchemaGenerator[T],
                       reader: AvroReader[T],
                       writer: AvroWriter[T]): Assertion = {
    val asJson = schemaGenerator.generate
    println(pretty(asJson))
    val schema = AvroCompiler.compile(asJson)
    val andBack = schema.toString(false)
    println(s"schema\n$andBack")
    val records = protos.map{ p =>
      writer.write(schema, p)
    }
    val res = records.map { r => reader.read(schema, r ) }
    println(s"read is $res")
    res should === (protos)
  }

  def writeReadBackFile[T](protos: List[T],
                           tFile: File)
                          (implicit schemaGenerator: SchemaGenerator[T],
                           reader: AvroReader[T],
                           writer: AvroWriter[T]): Assertion = {
    AvroSerialize.writeBinaryFile[T](protos, tFile)
    val readBack = AvroSerialize.readBinaryFile[T](tFile)
    readBack should === (protos)
  }


  def writeReadBack[T](proto: T)
                      (implicit schemaGenerator: SchemaGenerator[T],
                       reader: AvroReader[T],
                       writer: AvroWriter[T]): Assertion =
    writeReadBackList(List(proto))

  "simple" in {
    writeReadBack(simpleProto)
  }

  "wrappedSimple" in {
    writeReadBack(wrappedSimple)
  }

  "double Array" in {
    writeReadBack(wdaProto)
  }

  "byte Array" in {
    writeReadBack(wbProto)
  }


  "stuff" in {
    writeReadBackList(List(stuffProto, stuffProto2))
  }

  "list" in {
    writeReadBackList(List(iList1, iList0))
  }

  "set" in {
    writeReadBackList(List(iSet1, iSet0))
  }

  "stuff list" in {
    writeReadBack(StuffList(stuffList))
  }

  "naked stuff list" in {
    writeReadBack(stuffList)
  }


  "stuff map" in {
    implicit val schema = stringMapSchema[String, Stuff]
    implicit val writer = stringMapAvroWriter[String, Stuff]
    implicit val reader = stringMapAvroReader[Stuff]

    writeReadBack(stuffMap)
  }
  "stuff double map" in {
    implicit val schema = kvMapSchema[Stuff, Double]
    implicit val writer = kvMapAvroWriter[Stuff, Double]
    implicit val reader = kvMapAvroReader[Stuff, Double]

    writeReadBack(stuffDoubleMap)
  }
  "options" in {
    writeReadBackList(optList)
  }

  "instant" in {
    writeReadBack(Instant.now)
  }

  "localDate" in {
    writeReadBack(LocalDate.now)
  }

  "localDateTime" in {
    writeReadBack(LocalDateTime.now)
  }

  "localTime" in {
    writeReadBack(LocalTime.now)
  }

  "zonedDateTime" in {
    writeReadBack(ZonedDateTime.now())
  }

  "as file" in {
    writeReadBackFile(List(iSet1, iSet0), new File("data/avro/testout.avro"))
  }

  "s1 as file" in  {
    writeReadBackFile(List(sms), new File("data/avro/testout.avro"))
  }
  "s2 file" in  {
    writeReadBackFile(List(s2), new File("data/avro/testout.avro"))
  }


}

object TestSchema {
  case class Simple(i: Int, d: Double)
  val simpleProto = Simple(1, 2.0)

  case class WrappedSimple(simple: Simple)
  val wrappedSimple = WrappedSimple(simpleProto)

  case class WithDoubleArray(i: Int, b: Array[Double]) {
    // I know this should have a hash too but not necessary here
    override def equals(obj: Any): Boolean = {
      obj match {
        case wda: WithDoubleArray =>
          wda.i == i && util.Arrays.equals(wda.b, b)
        case _ => false
      }
    }
  }
  val wdaProto = WithDoubleArray(3, Array(1.2, 2.5))

  case class WithBytes(i: Int, bytes: Array[Byte])
  val wbProto = WithBytes(1, Array(12, 13, 14))

  sealed trait Stuff
  case object AStuff extends Stuff
  case object BStuff extends Stuff
  case class CStuff(j: Int) extends Stuff

  case class WithStuff(i: Int, stuff1: Stuff)
  val stuffProto = WithStuff(1, AStuff)
  val stuffProto2 = WithStuff(1, CStuff(2))

  val stuffList: List[Stuff] = List(AStuff, CStuff(2))

  case class IntList(l: List[Int])
  val iList1 = IntList(List(1,2))
  val iList0 = IntList(Nil)

  type NakedList = List[Stuff]

  case class StuffList(l: NakedList)


  case class IntSet(l: Set[Int])
  val iSet1 = IntSet(Set(1,2))
  val iSet0 = IntSet(Set.empty[Int])

  type StuffMap = Map[String, Stuff]

  val stuffMap: StuffMap = Map( "one" -> AStuff, "two" -> CStuff(2))

  type StuffDoubleMap = Map[Stuff, Double]

  val stuffDoubleMap: StuffDoubleMap = Map(AStuff -> 3.2, CStuff(2) -> 4.0, BStuff -> 5)

  val optList: List[Option[Int]] = List(None, Some(1), Some(3))

  case class SlicedEntry(distance: Float,
                         averageDuration: Float,
                         minDuration: Float,
                         maxDuration: Float,
                         size: Int,
                         start: Int,
                        )

  type SiteCode = String

  case class LabelledMatrixEntry(from: String, to: String, entry: SlicedEntry)

  val sle1 = SlicedEntry(1.0f, 1.1f, 1.2f, 1.3f, 3, 2)
  val sle2 = SlicedEntry(2.0f, 2.1f, 2.2f, 2.3f, 4, 3)

  val lme1 =LabelledMatrixEntry("hi", "there", sle1)
  val lme2 =LabelledMatrixEntry("ho", "thor", sle2)

  case class StaticMapService(rawData: Array[Float], available: List[LabelledMatrixEntry]) {
    override def equals(obj: Any): Boolean = {
      obj match {
        case sms: StaticMapService =>
          val rawEquals = util.Arrays.equals(rawData, sms.rawData)
          val availEquals = available == sms.available
          rawEquals && availEquals
        case _ => false
      }

    }

    override def hashCode(): Int = super.hashCode()
  }
  case class S1(rawData: Array[Float])
  case class S2(available: List[LabelledMatrixEntry])

  val sms = StaticMapService(Array(1.1f, 1.2f), List(lme1, lme2))
  val s1 = S1(Array(1.1f, 1.2f))
  val s2 = S2(List(lme1, lme2))
}
