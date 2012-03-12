package com.micronautics.akka

/** Default load for ExecutorBenchmark
 * @author Mike Slinn
 * @author calculatePiFor() provided by Typesafe
 */

class ExpensiveCalc {}

object ExpensiveCalc {
  def run(): Any = { calculatePiFor(0, 1000000) }

  def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    var acc = 0.0
    for (i <- start until (start + nrOfElements))
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    acc
  }
}
