package com.micronautics.akka

import scala.util.Random

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

/** Default load for ExecutorBenchmark
 * @author Mike Slinn
 * @author calculatePiFor() provided by Typesafe
 */

class DefaultLoads {}

object DefaultLoads {
  private val random = new Random()
  var intensity = 1000000
  var fetchCount = 10


  /** Simulate a CPU-bound task (compute Pi to a million places) */
  def cpuIntensive(): Any = { calculatePiFor(0, intensity) }

  private def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    var acc = 0.0
    for (i <- start until (start + nrOfElements))
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    acc
  }


  /** Simulate an IO-bound task (web spider) */
  def ioBound(): Any = simulateSpider(5, 30, fetchCount)

  /** @param minDelay minimum time (ms) to sleep per invocation
    * @param maxDelay maximum time (ms) to sleep per invocation
   * @param nrOfFetches number of times to repeatedly sleep then run a short computation per invocation */
  private def simulateSpider(minDelay: Int, maxDelay: Int,  nrOfFetches: Int) {
    for (i <- 0 until nrOfFetches) {
      // simulate from minDelay to maxDelay ms latency
      Thread.sleep(random.nextInt(maxDelay-minDelay) + minDelay)
      calculatePiFor(0, 50) // simulate a tiny amount of computation
    }
  }
}
