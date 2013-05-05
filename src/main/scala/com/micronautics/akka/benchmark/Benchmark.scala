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

import scala.concurrent.{ Await, ExecutionContext, Future }
import akka.actor.ActorSystem
import scala.concurrent.duration.Duration
import java.util.concurrent.{ ExecutorService, Executor }
import com.micronautics.akka.DefaultLoads
import Model.ecNameMap
import collection.parallel.ForkJoinTasks
import Numeric._
import grizzled.math.stats.{ arithmeticMean, popStdDev, populationVariance }
import scala.collection.parallel.immutable.ParSeq

/**
  * Exercises the ExecutorBenchmark loads
  * @author Mike Slinn
  *
  * @param load no-args functor that can return any type of result
  * @param showResult determines if the result should be displayed on the console or not
  */
class Benchmark(val load: () => Any, val showResult: Boolean) {
  private val gui = new Gui(this)
  private implicit var dispatcher: ExecutionContext = null
  private var forkJoinTaskSupport : scala.collection.parallel.ForkJoinTaskSupport = _

  /** Swing view */
  def showGui { gui.startup(null) }

  def stop() { /* not implemented */ }

  def run() {
    reset
    if (Benchmark.consoleOutput)
      println()
    ecNameMap.keys.foreach { ec: Any =>
      val ecName: String = ecNameMap.get(ec.asInstanceOf[AnyRef]).get
      ec match {
        case system: ActorSystem =>
          dispatcher = system.dispatcher
          runAkkaFutureLoads(ec, ecName)
          system.shutdown()
        case parallelism: Int =>
          runParallelLoads(parallelism, ecName)
        case jucExecutor: Executor => // j.u.c.Executor assumed
          dispatcher = ExecutionContext.fromExecutor(jucExecutor)
          runAkkaFutureLoads(ec, ecName)
          jucExecutor.asInstanceOf[ExecutorService].shutdown()
        case unknown =>
          println("Unknown test type: " + unknown)
          return
      }
      gui.removeCategorySpaces
    }
    gui.resize
  }

  def reset {
    ExecutorBenchmark.reset
    Model.reset
  }

  /**
    * Exercise load numInterations times using Akka Future; if load is idempotent then each result will be identical.
    * The load needs to execute long enough that the overhead of the for-comprehension and Future.sequence in this method is not noticeable.
    * @return TimedResult containing total time and list of results
    */
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

  /**
    * Run loads at least twice; once to warm up Hotspot using the desired Executor,
    * and again at least once to time using the warmed up Hotspot. If a standard deviation is desired then the load
    * should be invoked at least 10 times, perhaps 100 times.
    */
  def runAkkaFutureLoads(executor: Any, executorName: String) {
    if (Benchmark.consoleOutput)
      println("Warming up hotspot for executor " + executorName)
    val newTest1 = Model.addTest(executor, executorName, runAkkaFutureLoad, true)
    if (Benchmark.showWarmUpTimes) {
      val test1StdDev = 0 // we only warm up once
      gui.addValue(MeanResult(newTest1.test, newTest1.testName, newTest1.millis, test1StdDev), true)
    }
    if (Benchmark.consoleOutput)
      println("\nRunning " + Benchmark.numRuns + " timed loads on " + executorName)
    val results = for (
      i <- 0 until Benchmark.numRuns;
      result = Model.addTest(executor, executorName, runAkkaFutureLoad, false)
    ) yield TestResult(newTest1.test, executorName, result.millis, result)
    val millisMean: Long = arithmeticMean(results.map(_.millis): _*).asInstanceOf[Long]
    val stdDev: Long = popStdDev(results.map(_.millis): _*).asInstanceOf[Long]
    // std deviation is +/- so subtract from mean and double it to show uncertainty range
    // midpoint of uncertainty is therefore the mean
    gui.addValue(MeanResult(runAkkaFutureLoad, executorName, stdDev * 2L, millisMean - stdDev), false)
    if (Benchmark.consoleOutput)
      println("\n---------------------------------------------------\n")
  }

  /**
    * Exercise load numInterations times using Scala parallel collection; if load is idempotent then each result will be identical.
    * @return TimedResult containing total time and list of results
    */
  def runParallelLoad: TimedResult[Seq[Any]] = {
    System.gc(); System.gc(); System.gc()
    val timedResult = time {
      val array = (1 to Benchmark.numInterations).par
      // {@see http://stackoverflow.com/questions/5424496/scala-parallel-collections-degree-of-parallelism/5425354#5425354}
      array.tasksupport = forkJoinTaskSupport
      (array.par.map { x => load() })
    }("Parallel collection elapsed time").asInstanceOf[TimedResult[Seq[Any]]]
    if (Benchmark.consoleOutput && showResult)
      println("Result in " + timedResult.millis + " using Scala parallel collections: " + timedResult.results)
    timedResult
  }

  /**
    * Run loads at least twice; once to warm up Hotspot using the desired Executor,
    * and again at least once to time using the warmed up Hotspot. If a standard deviation is desired then the load
    * should be invoked at least 10 times, perhaps 100 times.
    */
  def runParallelLoads(nProcessors: Int, executorName: String) {
    //ForkJoinTasks.defaultForkJoinPool.setParallelism(nProcessors)
    forkJoinTaskSupport = new scala.collection.parallel.ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(nProcessors))
    // coming in Scala 2.10 according to Aleksandar Prokopec:
    //scala.collection.parallel.mutable.ParArray(1, 2, 3).tasksupport = new scala.collection.parallel.ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(2))

    if (Benchmark.consoleOutput)
      println("Warming up hotspot for " + executorName)
    val newTest1 = Model.addTest(nProcessors, executorName, runParallelLoad, true)
    if (Benchmark.showWarmUpTimes) {
      val test1StdDev = 0 // // we only warm up once
      gui.addValue(MeanResult(newTest1.test, newTest1.testName, newTest1.millis, test1StdDev), true)
    }
    val results = for (
      i <- 0 until Benchmark.numRuns;
      result = Model.addTest(nProcessors, executorName, runParallelLoad, false)
    ) yield TestResult(newTest1.test, executorName, result.millis, result)
    val millisMean: Long = arithmeticMean(results.map(_.millis): _*).asInstanceOf[Long]
    val stdDev: Long = popStdDev(results.map(_.millis): _*).asInstanceOf[Long]
    // std deviation is +/- so subtract from mean and double it to show uncertainty range
    // midpoint of uncertainty is therefore the mean
    gui.addValue(MeanResult(newTest1.test, executorName, stdDev * 2L, millisMean - stdDev), false)
    if (Benchmark.consoleOutput)
      println("\n---------------------------------------------------\n")
  }

  def time(block: => Any)(msg: String = "Elapsed time"): TimedResult[Any] = {
    val t0 = System.nanoTime()
    val result: Any = block
    val elapsedMs = (System.nanoTime() - t0) / 1000000
    if (Benchmark.consoleOutput)
      println(msg + ": " + elapsedMs + "ms")
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

  def apply(load: () => Any = DefaultLoads.cpuIntensive, showResult: Boolean = false) = {
    new Benchmark(load, showResult)
  }
}
