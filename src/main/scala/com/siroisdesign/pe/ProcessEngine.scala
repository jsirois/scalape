package com.siroisdesign.pe;

private[pe] trait Executor {

  /**
   * Synchronously calculates and assigns a value.
   */
  sealed def <=[O](producer: => O) = () => producer

  /**
   * Schedules asynchronously calculation of a value and returns a function that will block on the result.
   */
  private[pe] def <~[O](producer: => O): () => O
}

/**
 * A class that provides a DSL of sorts for describing a function call graph with edges that can be either synchronous
 * or asynchronous.  The resulting model can then be executed and parallelism will be exploited to execute the maximum
 * number of asynchonous edge calls concurrently as possible during reduction of the graph to a final value.  Arguments
 * are bound with chained builder objects that uniformly bind synchronously with {@code <=} and asynchronously with
 * {@code <~}.  Currently the class only handles functions of 2 or 3 arguments.
 */
sealed class ProcessEngine(private[pe] val executor:Executor) {
  def this() = this(ProcessEngine)

  private[ProcessEngine] type delayed[T] = () => T

  private[ProcessEngine] type delayed1[I, O] = delayed[I] => delayed[O]

  class Binder[I, O](private[ProcessEngine] val function: delayed1[I, O]) {
    def <=(producer: => I) = function(executor <= producer)
    def <~(producer: => I) = function(executor <~ producer)
  }

  private[ProcessEngine] type delayed2[I1, I2, O] = delayed[I1] => delayed1[I2, O]

  class Binder2[I1, I2, O](private[ProcessEngine] val function: delayed2[I1, I2, O]) {
    def <=(producer: => I1) = new Binder(function(executor <= producer))
    def <~(producer: => I1) = new Binder(function(executor <~ producer))
  }

  /**
   * Schedules execution of {@code function} with 2 arguments bound by synchronously or asynchronously using the
   * returned {@link Binder2}.
   */
  def ~>:[I1, I2, O](function: (I1, I2) => O) =
    new Binder2((input1: delayed[I1]) =>
                (input2: delayed[I2]) =>
                        () => function(input1(), input2()))

  private[ProcessEngine] type delayed3[I1, I2, I3, O] = delayed[I1] => delayed2[I2, I3, O]

  class Binder3[I1, I2, I3, O](private[ProcessEngine] val function: delayed3[I1, I2, I3, O]) {
    def <=(producer: => I1) = new Binder2(function(executor <= producer))
    def <~(producer: => I1) = new Binder2(function(executor <~ producer))
  }

  /**
   * Schedules execution of {@code function} with 3 arguments bound by synchronously or asynchronously using the
   * returned {@link Binder3}.
   */
  def ~>:[I1, I2, I3, O](function: (I1, I2, I3) => O) =
    new Binder3((input1: delayed[I1]) =>
                (input2: delayed[I2]) =>
                (input3: delayed[I3]) =>
                        () => function(input1(), input2(), input3()))

  // TODO(jsirois): ditto...
}

object ProcessEngine extends Executor {
  import java.util.concurrent._

  private[pe] lazy val executor: ThreadPoolExecutor = Executors.newCachedThreadPool match {
    case t: ThreadPoolExecutor => t
    case _ => throw new IllegalStateException
  }

  private[pe] def <~[O](producer: => O) = {
    val future = executor.submit(new Callable[O] {
      def call = producer
    })
    () => future.get
  }
}