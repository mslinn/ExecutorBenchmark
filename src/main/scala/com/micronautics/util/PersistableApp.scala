package com.micronautics.util

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

import java.io.{FileOutputStream, OutputStream, FileInputStream, File}
import scala.collection.JavaConversions._
import java.util.{Collections, Properties}

/**
  * Provides ability for a Swing app to save and restore to a properties file.
  * Methods are protected so unit tests can be written, but not accessed outside this package.
  * @author Mike Slinn
  */
trait PersistableApp {
  self =>
  protected def readProperties(file: File): scala.collection.mutable.Map[String, String] = {
    val props = new Properties
    if (file.exists) {
      val in = new FileInputStream(file)
      try {
        props.load(in)
      } finally {
        in.close
      }
    }
    props
  }

  protected def writeProperties(file: File, props: Properties) = {
    val out: OutputStream = new FileOutputStream(file)
    try {
      props.store(out, "")
    } finally {
      out.close
    }
  }
}

/** Writes properties in alpha order */
class SortedProperties extends Properties {
  override def keys = {
    var keyList = new java.util.Vector[Object]()
    var keysEnum = super.keys()
    while (keysEnum.hasMoreElements())
      keyList.add(keysEnum.nextElement())
    Collections.sort(keyList.asInstanceOf[java.util.Vector[String]])
    keyList.elements()
  }
}