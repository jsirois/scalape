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

    type delayed1[I, O] = (() => I) => () => O
    
    class Binder[I, O](private[ProcessEngine] val function:delayed1[I, O]) {
      def <=(producer: => I) = function(executor <= producer)
      def <~(producer: => I) = function(executor <~ producer)
    }

    type delayed2[I1, I2, O] = (() => I1) => delayed1[I2, O]

    class Binder2[I1, I2, O](private[ProcessEngine] val function:delayed2[I1, I2, O]) {
      def <=(producer: => I1) = new Binder[I2, O](function(executor <= producer))
      def <~(producer: => I1) = new Binder[I2, O](function(executor <~ producer))
    }

    /**
     * Schedules execution of {@code function} with arguments bound by synchronously or asynchronously using the
     * returned {@link Binder2}.
     */
    def ~>:[I1, I2, O](function:(I1, I2) => O) = {
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

    private[pe] def <~[O](producer: => O) = {
      val future = executor.submit(new Callable[O] {
        def call = producer
      })
      () => future.get
    }
  }
}