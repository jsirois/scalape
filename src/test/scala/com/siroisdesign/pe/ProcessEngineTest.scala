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
        ((prefix:String, message:String, suffix:String) => {
          LOG.info("executing user-supplied function with prefix: " + prefix + " suffix:" + suffix)
          prefix + message + suffix
        }) ~>: processEngine <~ {
          LOG.info("Retuning joe immediately")
          "joe"
        } <= "Hello World!" <~ {
          LOG.info("About to sleep before calculating jake...")
          Thread.sleep(100)
          LOG.info("...woke up to return jake")
          "jake"
        }

    expect("joeHello World!jake") {
      result()
    }

    expect(0, "Shouldn't spawn any threads using package-private constructor") {
      ProcessEngine.executor.getPoolSize
    }
  }
}