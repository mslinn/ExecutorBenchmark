package com.micronautics.akka.benchmark

import akka.jsr166y.ForkJoinPool
import java.util.concurrent.Executors
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import collection.parallel.ForkJoinTasks
import collection.immutable.ListMap
import com.micronautics.akka.ExpensiveCalc

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

  val ecNameMap = ListMap(
    system1 -> "akka.actor.ActorSystem fork-join-executor",
    system2 -> "akka.actor.ActorSystem thread-pool-executor with parallelism-factor = 3.0",
    system3 -> "akka.actor.ActorSystem thread-pool-executor with parallelism-factor = 1.0",
    esFJP -> "akka.jsr166y.ForkJoinPool",
    esFTP1 -> "FixedThreadPool with nProcessors=1 (java.util.concurrent.ThreadPoolExecutor)",
    esFTPn -> "FixedThreadPool with nProcessors=%d (java.util.concurrent.ThreadPoolExecutor)".format(nProcessors),
    esCTP -> "CachedThreadPool (java.util.concurrent.ThreadPoolExecutor)",
    esSTE -> "SingleThreadExecutor (java.util.concurrent.ThreadPoolExecutor)"
  )

  ForkJoinTasks.defaultForkJoinPool.setParallelism(nProcessors)
  Benchmark(ecNameMap)(ExpensiveCalc.run)(true) run
}
