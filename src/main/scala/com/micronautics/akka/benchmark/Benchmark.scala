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
import java.util.concurrent.{ExecutorService, Executor}
import akka.actor.ActorSystem
import Model.ecNameMap
import com.micronautics.akka.DefaultLoad
import akka.util.Duration

/**
  * Does the heavy lifting for ExecutorBenchmark
  * @author Mike Slinn
  */
class Benchmark (var load: () => Any, var showResult: Boolean) {
  var consoleOutput: Boolean = true
  val NumInterations: Int = 1000
  implicit var dispatcher: ExecutionContext = null


  /** Swing view */
  def showGui {
    val gui = new Gui(this)
    gui.startup(null)
  }

  def stop() { /* not implemented */ }

  def run() {
    if (consoleOutput)
      println()
    ecNameMap.keys.foreach {
      e: Any =>
        if (e.isInstanceOf[ActorSystem]) {
          dispatcher = e.asInstanceOf[ActorSystem].dispatcher
          doit(e, ecNameMap.get(e.asInstanceOf[AnyRef]).get)
          e.asInstanceOf[ActorSystem].shutdown()
        } else {
          dispatcher = ExecutionContext.fromExecutor(e.asInstanceOf[Executor])
          doit(e, ecNameMap.get(e.asInstanceOf[AnyRef]).get)
          e.asInstanceOf[ExecutorService].shutdown()
        }
    }
  }

  def doit(test: Object, executorName: String) {
    if (consoleOutput)
      println("Warming up hotspot to test " + executorName)
    addTest(test, executorName, parallelTest, true)
    addTest(test, executorName, futureTest, true)
    if (consoleOutput)
      println("\nRunning tests on " + executorName)
    addTest(test, executorName, parallelTest, false)
    addTest(test, executorName, futureTest, false)
    if (consoleOutput)
      println("\n---------------------------------------------------\n")
  }

  def futureTest: Seq[Any] = {
    val t0 = System.nanoTime()
    val trFuture = time {
      for (i <- 1 to NumInterations) yield Future { load() }
    }("Futures creation time").asInstanceOf[TimedResult[Seq[Future[Any]]]]

    val f2 = Future sequence trFuture.result andThen {
      case f =>
        val t1 = System.nanoTime()
        println("Total time for Akka future version: " + (t1 - t0) / 1000000 + "ms")
        f match {
          case Right(timedResult: TimedResult[Any]) =>
            if (showResult)
              println("Result using Akka future version: " + timedResult.result)
            timedResult
          case Left(exception) =>
            println(exception.getMessage)
            TimedResult(0, null)
          case _ =>
            TimedResult(0, null)
        }
    }
    Await.result(f2, Duration.Inf).asInstanceOf[Seq[Any]]
  }

  def parallelTest: TimedResult[Any] = {
    val timedResult = time {
      (1 to NumInterations).par.map { x => load() }
    }("Parallel collection elapsed time")
    if (showResult)
      println("Result using Scala parallel collections: " + timedResult.result)
    timedResult
  }

  def time(block: => Any)(msg: String="Elapsed time"): TimedResult[Any] = {
    val t0 = System.nanoTime()
    val result: Any = block
    val t1 = System.nanoTime()
    if (consoleOutput)
      println(msg + ": "+ (t1 - t0)/1000000 + "ms")
    TimedResult((t1 - t0)/1000000, result)
  }
}

object Benchmark {
  def apply(load: () => Any = DefaultLoad.run, showResult: Boolean=false) = {
    new Benchmark(load, showResult)
  }
}