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

/**
  * Does the heavy lifting for ExecutorBenchmark
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
      e: Any =>
        if (e.isInstanceOf[ActorSystem]) {
          val system = e.asInstanceOf[ActorSystem]
          dispatcher = system.dispatcher
          doit(e, ecNameMap.get(e.asInstanceOf[AnyRef]).get)
          system.shutdown()
        } else {
          dispatcher = ExecutionContext.fromExecutor(e.asInstanceOf[Executor])
          doit(e, ecNameMap.get(e.asInstanceOf[AnyRef]).get)
          e.asInstanceOf[ExecutorService].shutdown()
        }
    }
  }

  def reset {
    ExecutorBenchmark.reset
    Model.reset
  }
  
  def doit(test: Any, executorName: String) {
    if (Benchmark.consoleOutput)
      println("Warming up hotspot to test " + executorName)
    if (Benchmark.doParallelCollections) {
      val newTest = Model.addTest(test, "Parallel collection w/ " + executorName, parallelTest, true)
      if (Benchmark.showWarmUpTimes)
        gui.addValue(newTest, true)
    }
    if (Benchmark.doFutures) {
      val newTest = Model.addTest(test, "Akka Futures w/ "  + executorName, futureTest, true)
      if (Benchmark.showWarmUpTimes)
        gui.addValue(newTest, true)
    }
    if (Benchmark.consoleOutput)
      println("\nRunning tests on " + executorName)
    if (Benchmark.doParallelCollections) {
      val newTest = Model.addTest(test, "Parallel collection w/ " + executorName, parallelTest, false)
      gui.addValue(newTest, false)
    }
    if (Benchmark.doFutures) {
      val newTest = Model.addTest(test, "Akka Futures w/ "  + executorName, futureTest, false)
      gui.addValue(newTest, false)
    }
    gui.removeCategorySpaces
    if (Benchmark.consoleOutput)
      println("\n---------------------------------------------------\n")
  }

  def futureTest: TimedResult[Seq[Any]] = {
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

  def parallelTest: TimedResult[Seq[Any]] = {
    val timedResult = time {
      ((1 to Benchmark.numInterations).par.map { x => load() })
    }("Parallel collection elapsed time").asInstanceOf[TimedResult[Seq[Any]]]
    if (Benchmark.consoleOutput && showResult)
      println("Result in " + timedResult.millis + " using Scala parallel collections: " + timedResult.results)
    timedResult
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
  var doParallelCollections: Boolean = true
  var doFutures: Boolean = true
  var showWarmUpTimes: Boolean = false


  def apply(load: () => Any = DefaultLoad.run, showResult: Boolean=false) = {
    new Benchmark(load, showResult)
  }
}