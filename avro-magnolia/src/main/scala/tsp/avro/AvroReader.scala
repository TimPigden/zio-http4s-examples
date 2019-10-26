package tsp.avro

import magnolia.{CaseClass, Magnolia, SealedTrait}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}

import scala.language.experimental.macros

trait AvroReader[A] {
  def read(schema: Schema, data: AnyRef): A
}

object AvroReaderDerivation {
  type Typeclass[T] = AvroReader[T]

  def combine[T](ctx: CaseClass[AvroReader, T]): AvroReader[T] = { (schema, data) =>
    if (ctx.isObject)
      ctx.rawConstruct(Seq.empty)
    else data match {
      case r: GenericRecord =>
        val fields = ctx.parameters.map { param =>
          val thisSchema = schema.getField(param.label).schema()
          val fieldObj = r.get(param.label)
          param.typeclass.read(thisSchema, fieldObj)
        }

        ctx.rawConstruct(fields)
      case x => throw new IllegalArgumentException(s"avro reader wrong type $x")
    }
  }

  def dispatch[T](ctx: SealedTrait[AvroReader, T]): AvroReader[T] = { (schema, data) =>
    data match {
      case gd: GenericData.Record =>
        val thisSchema = gd.getSchema
        val subtype = ctx.subtypes.find(_.typeName.short == thisSchema.getName).get
        subtype.typeclass.read(thisSchema, data)
    }

  }

  implicit def avroReader[T]: AvroReader[T] = macro Magnolia.gen[T]
}
