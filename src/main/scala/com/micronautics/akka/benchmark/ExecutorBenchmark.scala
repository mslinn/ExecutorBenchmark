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

import akka.jsr166y.ForkJoinPool
import java.util.concurrent.Executors
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import collection.parallel.ForkJoinTasks
import collection.immutable.ListMap

/** Sample setup for Benchmark
  * @author Mike Slinn */
object ExecutorBenchmark extends App {
  val nProcessors = Runtime.getRuntime.availableProcessors
  val esFJP = new ForkJoinPool()
  val esFTPn = Executors.newFixedThreadPool(nProcessors)
  val esFTP1 = Executors.newFixedThreadPool(1)
  val esCTP = Executors.newCachedThreadPool()
  val esSTE = Executors.newSingleThreadExecutor()

  private val configString1: String = """akka {
    logConfigOnStart=off
    executor = "fork-join-executor"
    fork-join-executor {
        parallelism-min = %d
        parallelism-factor = 3.0
        parallelism-max = %d
    }
  }""".format(nProcessors, nProcessors)
  private val system1 = ActorSystem.apply("default1", ConfigFactory.parseString(configString1))

  private val configString2: String = """akka {
    logConfigOnStart=off
    executor = "thread-pool-executor"
    fork-join-executor {
        core-pool-size-min = %d
        parallelism-factor = 3.0
        core-pool-size-max = %d
    }
  }""".format(nProcessors, nProcessors)
  private val system2 = ActorSystem.apply("default2", ConfigFactory.parseString(configString2))

  private val configString3: String = """akka {
    logConfigOnStart=off
    executor = "thread-pool-executor"
    fork-join-executor {
        core-pool-size-min = %d
        parallelism-factor = 1.0
        core-pool-size-max = %d
    }
  }""".format(nProcessors, nProcessors)
  private val system3 = ActorSystem.apply("default2", ConfigFactory.parseString(configString3))

  Model.ecNameMap ++= ListMap(
    system1 -> "Akka ActorSystem w/ fork-join-executor",
    system2 -> "Akka ActorSystem w/ thread-pool-executor & parallelism-factor=3",
    system3 -> "Akka ActorSystem w/ thread-pool-executor & parallelism-factor=1",
    esFJP   -> "Updated ForkJoinPool",
    esFTP1  -> "FixedThreadPool w/ nProcessors=1",
    esFTPn  -> "FixedThreadPool w/ nProcessors=%d".format(nProcessors),
    esCTP   -> "CachedThreadPool",
    esSTE   -> "SingleThreadExecutor"
  )

  ForkJoinTasks.defaultForkJoinPool.setParallelism(nProcessors)

  Benchmark.consoleOutput = true
  Benchmark().showGui
}
