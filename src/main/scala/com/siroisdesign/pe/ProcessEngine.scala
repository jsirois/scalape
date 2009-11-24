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
  private class ScalaCallable[T](callable:() => T) extends Callable[T] {
    override def call() = callable()
  }

  // TODO(jsirois): must be a way to define execute signature to accept by name directly - clean this up
  implicit def byName2byValue[T](function: => T) = () => function

  def callable[T] (supplier:() => T) : Callable[T] = new ScalaCallable(supplier)
}

trait Executor {
  
  /**
   * Synchronously calculates and assigns a value.
   */
  sealed def <=[O](producer: => O) = () => producer

  /**
   * Schedules asynchronously calculation of a value and returns a function that will block on the result.
   */
  def <~[O](producer: => O) : () => O
}

class PE(executor:Executor) {
  def this() = this(PE)

  class Binder[S, O](function:(() => S) => () => O) {
    def <=(producer: => S) = {
      function(executor <= producer)
    }

    def <~(producer: => S) = {
      function(executor <~ producer)
    }
  }

  class Binder2[S, T, O](function:(() => S) => (() => T) => () => O) {
    def <=(producer: => S) = {
      new Binder[T, O](function(executor <= producer))
    }

    def <~(producer: => S) = {
      new Binder[T, O](function(executor <~ producer))
    }
  }

  def ~>[I1, I2, O](function:(I1, I2) => O) = {
    def delayedFunction(input1:() => I1) = {
      (input2:() => I2) => () => function(input1(), input2())
    }
    new Binder2[I1, I2, O](delayedFunction)
  }
}

object PE extends Executor {
  import ProcessEngine.byName2byValue // enable implicit conversion

  lazy val executor:ThreadPoolExecutor = Executors.newCachedThreadPool match {
    case t:ThreadPoolExecutor => t
    case _ => throw new IllegalStateException
  }

  def <~[O](producer: => O) = {
    val future = executor.submit(ProcessEngine.callable(producer))
    () => future.get
  }
}