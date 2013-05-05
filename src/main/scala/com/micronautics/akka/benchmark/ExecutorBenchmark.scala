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

import scala.concurrent.forkjoin.ForkJoinPool
import java.util.concurrent.Executors
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.LinkedHashMap
import com.micronautics.akka.DefaultLoads

/** Sample setup for Benchmark
  * @author Mike Slinn */
object ExecutorBenchmark extends App {
  Benchmark.consoleOutput = true
  reset
  Benchmark(DefaultLoads.cpuIntensive).showGui

  /** Invoked from Benchmark.scala; yes this is horrible, but I just want this to work right now without any fuss */
  def reset {
    Model.reset
    val nProcessors = Runtime.getRuntime.availableProcessors
    val esFJP  = new ForkJoinPool(nProcessors)
    val esFTPn = Executors.newFixedThreadPool(nProcessors)
    val esFTP1 = Executors.newFixedThreadPool(1)
    val esCTP  = Executors.newCachedThreadPool()
    val esSTE  = Executors.newSingleThreadExecutor()

    val configString1: String = """akka {
      logConfigOnStart=off
      executor = "fork-join-executor"
      fork-join-executor {
          parallelism-min = %d
          parallelism-factor = 3.0
          parallelism-max = %d
      }
    }""".format(nProcessors, nProcessors)
    val system1 = ActorSystem("default1", ConfigFactory.parseString(configString1))

    val configString2: String = """akka {
      logConfigOnStart=off
      executor = "thread-pool-executor"
      fork-join-executor {
          core-pool-size-min = %d
          parallelism-factor = 3.0
          core-pool-size-max = %d
      }
    }""".format(nProcessors, nProcessors)
    val system2 = ActorSystem("default2", ConfigFactory.parseString(configString2))

    val configString3: String = """akka {
      logConfigOnStart=off
      executor = "thread-pool-executor"
      fork-join-executor {
          core-pool-size-min = %d
          parallelism-factor = 1.0
          core-pool-size-max = %d
      }
    }""".format(nProcessors, nProcessors)
    val system3 = ActorSystem("default2", ConfigFactory.parseString(configString3))

    // labels are short so they fit on the Y axis
    Model.ecNameMap = LinkedHashMap(
      1           -> "PC 1",      // parallel collection with 1 thread
      nProcessors -> "PC %d".format(nProcessors), // parallel collection with nProcessor threads
      system1     -> "Akka FJ",
      system2     -> "Akks TP 3",
      system3     -> "Akka TP 1", // ActorSystem thread-pool-executor & parallelism-factor=1
      esFJP       -> "Akka FJP",  // ActorSystem ForkJoinPool
      esFTP1      -> "FT 1",      // FixedThreadPool w/ nProcessors=1
      esFTPn      -> "FT %d".format(nProcessors), // FixedThreadPool w/ nProcessors
      esCTP       -> "CT",        // CachedThreadPool
      esSTE       -> "ST"         // SingleThreadExecutor
    )
  }
}
