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

import akka.util.Duration.Inf
import akka.dispatch.{Await, ExecutionContext, Future}
import java.util.concurrent.{ExecutorService, Executor}
import scala.collection.immutable.Map
import akka.actor.ActorSystem

/**
  * Does the heavy lifting for ExecutorBenchmark
  * @author Mike Slinn
  */
class Benchmark (val ecNameMap: Map[Object, String],
                 var load: () => Any,
                 var showResult: Boolean
                ) {
  val NumInterations = 1000
  implicit var dispatcher: ExecutionContext = null

  /** Swing view */
  def showGui {
    val gui = new Gui(this)
    gui.startup(null)
  }

  def stop() { /* not implemented */ }

  def run() {
    println()
    ecNameMap.keys.foreach {
      e: Any =>
        if (e.isInstanceOf[ActorSystem]) {
          dispatcher = e.asInstanceOf[ActorSystem].dispatcher
          doit(ecNameMap.get(e.asInstanceOf[AnyRef]).get)
          e.asInstanceOf[ActorSystem].shutdown()
        } else {
          dispatcher = ExecutionContext.fromExecutor(e.asInstanceOf[Executor])
          doit(ecNameMap.get(e.asInstanceOf[AnyRef]).get)
          e.asInstanceOf[ExecutorService].shutdown()
        }
    }
  }

  def doit(executorName: String) {
    println("Warming up hotspot to test " + executorName)
    parallelTest
    Await.ready(futureTest, Inf)
    println("\nRunning tests on " + executorName)
    parallelTest
    Await.ready(futureTest, Inf)
    println("\n---------------------------------------------------\n")
  }

  def futureTest = {
    val t0 = System.nanoTime()
    val futures = time {
      for (i <- 1 to NumInterations) yield Future { load() }
    }("Futures creation time")

    Future sequence futures andThen {
      case f =>
        val t1 = System.nanoTime()
        println("Total time for Akka future version: " + (t1 - t0) / 1000000 + "ms")
        f match {
          case Right(result) =>
            if (showResult)
              println("Result using Akka future version: " + result)
          case Left(exception) =>
            println(exception.getMessage)
        }
    }
  }

  def parallelTest {
    val parallelResult = time {
      (1 to NumInterations).par.map { x => load() }
    }("Parallel collection elapsed time")
    if (showResult)
      println("Result using Scala parallel collections: " + parallelResult)
    }

  def time[R](block: => R)(msg: String="Elapsed time"): R = {
      val t0 = System.nanoTime()
      val result = block
      val t1 = System.nanoTime()
      println(msg + ": "+ (t1 - t0)/1000000 + "ms")
      result
  }
}

object Benchmark {
  def apply(ecNameMap: Map[Object, String])(load: () => Any)(showResult: Boolean=false) = {
    new Benchmark(ecNameMap: Map[Object, String], load, showResult)
  }
}