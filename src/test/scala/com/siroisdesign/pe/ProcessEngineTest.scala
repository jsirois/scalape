package com.siroisdesign.pe

import java.util.logging.{SimpleFormatter, ConsoleHandler, Logger}
import org.scalatest._
import scala.{Math}
import ProcessEngine._

/**
 * @author john@siroisdesign.com
 */
class ProcessEngineTest extends FunSuite {
  test("assembly") {
    val processEngine = new ProcessEngine
    val result =
        processEngine.execute((prefix:String, times:Int, suffix:String) => prefix + times + suffix)
            .apply("jake")
            .apply {
              processEngine.execute((n:Int, p:Int) => Math.pow(n.toDouble, p.toDouble).toInt)(2) {
                processEngine.execute((n:Int) => n * 2).apply(2)
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

  val LOG:Logger = Logger.getLogger(this.getClass.getName)
  
  test("pe") {
    val pe = new PE
    val result =
        pe ~> ((prefix:String, suffix:String) => {
          LOG.info("executing user-supplied function with prefix: " + prefix + " suffix:" + suffix)
          prefix + suffix
        }) <= {
          LOG.info("Retuning joe immediately")
          "joe"
        } <~ {
          LOG.info("About to sleep before calculating jake...")
          Thread.sleep(100)
          LOG.info("...woke up to return jake")
          "jake"
        }

    expect("joejake") {
      result()
    }

    expect(1) {
      PE.executor.getPoolSize
    }
  }
}