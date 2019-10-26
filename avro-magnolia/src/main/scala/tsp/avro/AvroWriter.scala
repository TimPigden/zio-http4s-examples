package tsp.avro

import magnolia._
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

import scala.collection.JavaConverters._
import scala.language.experimental.macros
trait AvroWriter[A]  {
  def write(schema: Schema, value: A): AnyRef
}

object AvroWriterDerivation {
  type Typeclass[T] = AvroWriter[T]

  def combine[T](ctx: CaseClass[AvroWriter, T]): AvroWriter[T] =
    new AvroWriter[T] {
      override def write(schema: Schema, value: T): AnyRef = {
        val record = new GenericData.Record(schema)
        ctx.parameters.foreach { param =>
          val thisSchema = schema.getField(param.label).schema()
          val fieldVal = param.dereference(value)
          val res = param.typeclass.write(thisSchema, fieldVal)
          record.put(param.label, res)
        }
        record
      }
    }

  def dispatch[T](ctx: SealedTrait[AvroWriter, T]): AvroWriter[T] = { (schema, value) =>
    ctx.dispatch(value) { sub =>
      val thisSchema = schema.getTypes.asScala.find(_.getName == sub.typeName.short).get
      sub.typeclass.write(thisSchema, sub.cast(value))
    }
  }

  implicit def avroWriter[T]: AvroWriter[T] = macro Magnolia.gen[T]

}
