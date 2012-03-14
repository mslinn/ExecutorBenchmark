package com.micronautics.util

import java.util.Properties
import java.io.{FileOutputStream, OutputStream, FileInputStream, File}
import scala.collection.JavaConversions._

/**
 * Provides ability for a Swing app to save and restore to a properties file.
 * Methods are protected so unit tests can be written, but not accessed outside this package.
 * @author Mike Slinn
 */
trait PersistableApp {
  self =>
  protected def readProperties(file: File): scala.collection.mutable.Map[String, String] = {
    val props = new Properties
    val in = new FileInputStream(file)
    try {
      props.load(in)
    } finally {
      in.close
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
