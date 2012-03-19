package com.micronautics.akka.benchmark

/* Copyright 1012 Micronautics Research Corporation

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Subject to the additional condition that the attribution code in Gui.scala
   remains untouched and displays each time the program runs.

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

import akka.dispatch.{Await, ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.util.Duration
import java.util.concurrent.{ExecutorService, Executor}
import com.micronautics.akka.DefaultLoad
import Model.ecNameMap
import collection.parallel.ForkJoinTasks
import Numeric._
import grizzled.math.stats.{arithmeticMean, popStdDev, populationVariance}

/**
  * Exercises the ExecutorBenchmark loads
  * @author Mike Slinn
  */
class Benchmark (var load: () => Any, var showResult: Boolean) {
  val gui = new Gui(this)
  implicit var dispatcher: ExecutionContext = null


  /** Swing view */
  def showGui { gui.startup(null) }

  def stop() { /* not implemented */ }

  def run() {
    reset
    if (Benchmark.consoleOutput)
      println()
    ecNameMap.keys.foreach {
      ec: Any =>
        val ecName = ecNameMap.get(ec.asInstanceOf[AnyRef]).get
        if (ec.isInstanceOf[ActorSystem]) {
          val system = ec.asInstanceOf[ActorSystem]
          dispatcher = system.dispatcher
          runAkkaFutureLoads(ec, ecName)
          system.shutdown()
        } else if (ec.isInstanceOf[Int]) {
          runParallelLoads(ec.asInstanceOf[Int], ecName)
        } else { // j.u.c.Executor
          dispatcher = ExecutionContext.fromExecutor(ec.asInstanceOf[Executor])
          runAkkaFutureLoads(ec, ecName)
          ec.asInstanceOf[ExecutorService].shutdown()
        }
        gui.removeCategorySpaces
    }
  }

  def reset {
    ExecutorBenchmark.reset
    Model.reset
  }

  /** Exercise load numInterations times using Akka Future; if load is idempotent then each result will be identical.
   * The load needs to execute long enough that the overhead of the for-comprehension and Future.sequence in this method is not noticable.
    * @return TimedResult containing total time and list of results */
  def runAkkaFutureLoad: TimedResult[Seq[Any]] = {
    System.gc(); System.gc(); System.gc()
    val t0 = System.nanoTime()
    val trFuture = time {
      for (i <- 1 to Benchmark.numInterations) yield Future { load() }
    }("Futures creation time").asInstanceOf[TimedResult[Seq[Future[Any]]]]

    val f2 = Future.sequence(trFuture.results).map { results: Seq[Any] =>
        val elapsedMs: Long = (System.nanoTime() - t0) / 1000000
        if (Benchmark.consoleOutput) {
          println("Total time for Akka future version: " + elapsedMs + "ms")
          if (showResult)
            println("Result in " + trFuture.millis + " using Akka future version: " + results)
        }
        TimedResult(elapsedMs, results)
    }
    val r = Await.result(f2, Duration.Inf)
    r.asInstanceOf[TimedResult[Seq[Any]]]
  }

  /** Run loads at least twice; once to warm up Hotspot using the desired Executor,
   * and again at least once to time using the warmed up Hotspot. If a standard deviation is desired then the load
   * should be invoked at least 10 times, perhaps 100 times. */
  def runAkkaFutureLoads(executor: Any, executorName: String) {
    if (Benchmark.consoleOutput)
      println("Warming up hotspot for executor " + executorName)
    val newTest1 = Model.addTest(executor, "Akka Futures w/ "  + executorName, runAkkaFutureLoad, true)
    if (Benchmark.showWarmUpTimes) {
      val test1StdDev = 0 // we only warm up once
      gui.addValue(TestResult2(newTest1.test, newTest1.testName, newTest1.millis, test1StdDev), true)
    }
    if (Benchmark.consoleOutput)
      println("\nRunning " + Benchmark.numRuns + " timed loads on " + executorName)
    val results = for (i <- 0 until  Benchmark.numRuns;
      val result = Model.addTest(executor, "Akka Futures w/ "  + executorName, runAkkaFutureLoad, false)
    ) yield TestResult(newTest1.test, "Akka Futures w/ "  + executorName, result.millis, result)
    val millisMean: Long = arithmeticMean(results.map(_.millis) : _*).asInstanceOf[Long]
    val stdDev: Long = popStdDev(results.map(_.millis) : _*).asInstanceOf[Long]
    // std deviation is +/- so subtract from mean and double it to show uncertainty range
    // midpoint of uncertainty is therefore the mean
    gui.addValue(TestResult2(runAkkaFutureLoad, "Akka Futures w/ "  + executorName, stdDev*2L, millisMean-stdDev), false)
    if (Benchmark.consoleOutput)
      println("\n---------------------------------------------------\n")
  }

/** Exercise load numInterations times using Scala parallel collection; if load is idempotent then each result will be identical.
 * @return TimedResult containing total time and list of results */
  def runParallelLoad: TimedResult[Seq[Any]] = {
    System.gc(); System.gc(); System.gc()
    val timedResult = time {
      ((1 to Benchmark.numInterations).par.map { x => load() })
    }("Parallel collection elapsed time").asInstanceOf[TimedResult[Seq[Any]]]
    if (Benchmark.consoleOutput && showResult)
      println("Result in " + timedResult.millis + " using Scala parallel collections: " + timedResult.results)
    timedResult
  }

  /** Run loads at least twice; once to warm up Hotspot using the desired Executor,
   * and again at least once to time using the warmed up Hotspot. If a standard deviation is desired then the load
   * should be invoked at least 10 times, perhaps 100 times. */
  def runParallelLoads(nProcessors: Int, executorName: String) {
    ForkJoinTasks.defaultForkJoinPool.setParallelism(nProcessors)
    // coming in Scala 2.10 according to Aleksandar Prokopec:
    //scala.collection.parallel.mutable.ParArray(1, 2, 3).tasksupport = new scala.collection.parallel.ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(2))

    val msg = "Scala parallel collections using %d processors".format(nProcessors)
    if (Benchmark.consoleOutput)
      println("Warming up hotspot for " + msg)
    val newTest1 = Model.addTest(nProcessors, msg, runParallelLoad, true)
    if (Benchmark.showWarmUpTimes) {
      val test1StdDev = 0 // // we only warm up once
      gui.addValue(TestResult2(newTest1.test, newTest1.testName, newTest1.millis, test1StdDev), true)
    }
    val results = for (i <- 0 until  Benchmark.numRuns;
      val result = Model.addTest(nProcessors, msg, runParallelLoad, false)
    ) yield TestResult(newTest1.test, msg, result.millis, result)
    val millisMean: Long = arithmeticMean(results.map(_.millis) : _*).asInstanceOf[Long]
    val stdDev: Long = popStdDev(results.map(_.millis) : _*).asInstanceOf[Long]
    // std deviation is +/- so subtract from mean and double it to show uncertainty range
    // midpoint of uncertainty is therefore the mean
    gui.addValue(TestResult2(newTest1.test, msg, stdDev*2L, millisMean-stdDev), false)
    if (Benchmark.consoleOutput)
      println("\n---------------------------------------------------\n")
  }

  def time(block: => Any)(msg: String="Elapsed time"): TimedResult[Any] = {
    val t0 = System.nanoTime()
    val result: Any = block
    val elapsedMs = (System.nanoTime() - t0)/1000000
    if (Benchmark.consoleOutput)
      println(msg + ": "+ elapsedMs + "ms")
    TimedResult(elapsedMs, result)
  }
}

object Benchmark {
  val strWarmup = "Warm-up"
  val strTimed = "Timed"

  var consoleOutput: Boolean = true
  var numInterations: Int = 1000
  var numRuns: Int = 10
  var showWarmUpTimes: Boolean = false


  def apply(load: () => Any = DefaultLoad.run, showResult: Boolean=false) = {
    new Benchmark(load, showResult)
  }
}