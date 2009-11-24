package com.siroisdesign.pe

import org.scalatest._
import scala.{Math}
import ProcessEngine._

/**
 * @author john@siroisdesign.com
 */
class ProcessEngineTest extends FunSuite {
  test("assembly") {
    val processEngine = new ProcessEngine
    val result:() => String =
            processEngine
                    .execute((prefix:String, times:Int, suffix:String) => prefix + times + suffix)
                    .apply("jake")
                    .apply {
                        processEngine
                                .execute((n:Int, p:Int) => Math.pow(n.toDouble, p.toDouble).toInt)(2) {
                                    processEngine
                                            .execute((n:Int) => n * 2)
                                            .apply(2)
                                }
                    }.apply("joe");

    expect(0) {
      processEngine.getThreadCount
    }

    expect("jake16joe") {
      result()
    }

    assert(processEngine.getThreadCount > 0)
  }
}