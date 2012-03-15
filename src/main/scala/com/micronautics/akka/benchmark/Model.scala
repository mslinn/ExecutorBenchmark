package com.micronautics.akka.benchmark

import collection.mutable.ListMap

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

case class TimedResult[T](millis: Long, result: T)

case class TestResult(test: Object, testName: String, millis: Long, result: Any)

object Model {
  val ecNameMap = new ListMap[Object, String]

  /** Contains results that do not matter, executed just to warm up hotspot */
  val testResultMapWarmup = new ListMap[Object,  TestResult]

  /** Contains results that do matter, after hotspot is warmed up */
  val testResultMapHot = new ListMap[Object,  TestResult]

  addTest(test: Object, testName: String, timedResult: TimedResult, isWarmup: Boolean) {
    val testResult = new TestResult(test, testName, timedResult)
    if (isWarmup)
      testResultMapWarmup += test -> testResult
    else
      testResultMapHot += test -> testResult
  }
}
