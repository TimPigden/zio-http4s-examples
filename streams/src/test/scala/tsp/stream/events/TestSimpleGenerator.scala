package tsp.stream.events

import zio.test._
import zio.test.Assertion._

object TestSimpleGenerator extends DefaultRunnableSpec(
  suite("timings")
  (testM(assert(true, equalTo(true))))
)
{

}



