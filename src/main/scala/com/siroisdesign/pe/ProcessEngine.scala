package com.siroisdesign.pe


import java.util.concurrent._

/**
 * @author john@siroisdesign.com
 */
class ProcessEngine {
  private val executor:ThreadPoolExecutor = Executors.newCachedThreadPool match {
    case t:ThreadPoolExecutor => t
    case _ => throw new IllegalStateException
  }

  def execute[O, I](function:I => O) : (() => I) => (() => O) = {
    (input:() => I) => () => {
      function(input())
    }
  }

  def execute[O, I1, I2](function:(I1, I2) => O) : (() => I1) => (() => I2) => (() => O) = {
    (input1:() => I1) => (input2:() => I2) => () => {
      val result1:Future[I1] = executor.submit(ProcessEngine.callable(input1))
      val result2:Future[I2] = executor.submit(ProcessEngine.callable(input2))
      function(result1.get, result2.get)
    }
  }

  def execute[O, I1, I2, I3](function:(I1, I2, I3) => O) : (() => I1) => (() => I2) => (() => I3) => (() => O) = {
    (input1:() => I1) => (input2:() => I2) => (input3:() => I3) => () => {
      val result1:Future[I1] = executor.submit(ProcessEngine.callable(input1))
      val result2:Future[I2] = executor.submit(ProcessEngine.callable(input2))
      val result3:Future[I3] = executor.submit(ProcessEngine.callable(input3))
      function(result1.get, result2.get, result3.get)
    }
  }

  def getThreadCount() : Int = {
    executor.getPoolSize
  }
}

object ProcessEngine {
  private class ScalaCallable[T](callable:(() => T)) extends Callable[T] {
    override def call() : T = {
      callable.apply  
    }
  }

  // TODO(jsirois): must be a way to define execute signature to accept by name directly - clean this up
  implicit def byName2byValue[T](function: => T) = () => function
  
  def callable[T] (callable:() => T) : Callable[T] = {
    new ScalaCallable(callable)
  }
}