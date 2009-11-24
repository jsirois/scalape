package com.siroisdesign.pe

import org.apache.log4j.{Logger}
import org.scalatest._

/**
 * @author john@siroisdesign.com
 */
class ProcessEngineTest extends FunSuite {
  val LOG:Logger = Logger.getLogger(this.getClass)

  test("assembly") {
    val processEngine = new ProcessEngine(new Executor {
      def <~[O](producer: => O) = this <= producer
    })
    
    val result =
        ((prefix:String, suffix:String) => {
          LOG.info("executing user-supplied function with prefix: " + prefix + " suffix:" + suffix)
          prefix + suffix
        }) ~>: processEngine <~ {
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

    expect(0, "Shouldn't spawn any threads using package-private constructor") {
      ProcessEngine.executor.getPoolSize
    }
  }
}