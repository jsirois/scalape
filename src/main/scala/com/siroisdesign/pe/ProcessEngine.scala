package com.siroisdesign;

package pe {

  private[pe] trait Executor {

    /**
     * Synchronously calculates and assigns a value.
     */
    sealed def <=[O](producer: => O) = () => producer

    /**
     * Schedules asynchronously calculation of a value and returns a function that will block on the result.
     */
    private[pe] def <~[O](producer: => O) : () => O
  }

  class ProcessEngine(private[pe] val executor:Executor) {
    def this() = this(ProcessEngine)

    class Binder[S, O](private[ProcessEngine] val function:(() => S) => () => O) {
      def <=(producer: => S) = {
        function(executor <= producer)
      }

      def <~(producer: => S) = {
        function(executor <~ producer)
      }
    }

    class Binder2[S, T, O](private[ProcessEngine] val function:(() => S) => (() => T) => () => O) {
      def <=(producer: => S) = {
        new Binder[T, O](function(executor <= producer))
      }

      def <~(producer: => S) = {
        new Binder[T, O](function(executor <~ producer))
      }
    }

    def ~>: [I1, I2, O](function:(I1, I2) => O) = this ~> function

    def ~>[I1, I2, O](function:(I1, I2) => O) = {
      def delayedFunction(input1:() => I1) = {
        (input2:() => I2) => () => function(input1(), input2())
      }
      new Binder2[I1, I2, O](delayedFunction)
    }
  }

  object ProcessEngine extends Executor {
    import java.util.concurrent._

    private[pe] lazy val executor:ThreadPoolExecutor = Executors.newCachedThreadPool match {
      case t:ThreadPoolExecutor => t
      case _ => throw new IllegalStateException
    }

    private class ScalaCallable[T](callable:() => T) extends Callable[T] {
      override def call() = callable()
    }
    
    private[pe] def <~[O](producer: => O) = {
      val future = executor.submit(new ScalaCallable(() => producer))
      () => future.get
    }
  }
}