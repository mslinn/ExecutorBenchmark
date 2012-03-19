package com.micronautics.akka.benchmark

import scala.collection.mutable.{LinkedHashMap}


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

/**
  * @author Mike Slinn */

case class TimedResult[T](millis: Long, results: T)

case class TestResult(test: Any, testName: String, millis: Long, result: Any)
case class TestResult2(test: Any, testName: String, millisMean: Long, millisStdDev: Long, result: Any)

object Model {
  /** Map of Executor to descriptive name */
  var ecNameMap = new LinkedHashMap[Any, String]

  /** Contains results that do not matter, executed just to warm up hotspot */
  val testResultMapWarmup = new LinkedHashMap[Any,  TestResult]

  /** Contains results that do matter, after hotspot is warmed up */
  val testResultMapHot = new LinkedHashMap[Any,  TestResult]


  def addTest(test: Any, testName: String, timedResult: TimedResult[Seq[Any]], isWarmup: Boolean): TestResult = {
    val testResult = new TestResult(test, testName, timedResult.millis, timedResult.results)
    if (isWarmup)
      testResultMapWarmup += test -> testResult
    else
      testResultMapHot += test -> testResult
    testResult
  }

  def reset {
    ecNameMap.empty
    testResultMapHot.empty
    testResultMapWarmup.empty
  }
}
