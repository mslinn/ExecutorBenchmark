package com.micronautics.akka.benchmark

import com.micronautics.util.PersistableApp
import org.joda.time.DateTime
import java.awt.{Dimension, Point}
import java.io.File
import java.util.Properties
import swing._
import event.{ButtonClicked, WindowOpened, WindowClosing}
import javax.swing.WindowConstants

/**
 * @author Mike Slinn */
class Gui (benchmark: Benchmark) extends SimpleSwingApplication with PersistableApp {
  var running: Boolean = false
  private val navigator = new Navigator

  /** Swing entry point */
  def top = new MainFrame {
    /** When this program was last run */
    var lastRun: DateTime = new DateTime(0)

    peer.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
    title = "Executor Benchmark v0.1"
    location = new Point(200, 200)
    size = new Dimension(400, 400)

    contents = new BoxPanel(Orientation.Vertical) {
      contents += navigator
      border = Swing.EmptyBorder(30, 30, 10, 30)
      preferredSize = new Dimension(400, 400)
    }

    reactions += {
      case WindowOpened(_) =>
        loadProperties
        visible = true

      case WindowClosing(_) =>
        saveProperties(locationOnScreen, size)
        sys.exit(0)
    }

    private def loadProperties {
      val props = readProperties(new File("executorBenchmark.properties"))
      location = new Point(props.getOrElse("x", "0").toInt, props.getOrElse("y", "0").toInt)
      size = new Dimension(props.getOrElse("width", "400").toInt, props.getOrElse("height", "400").toInt)
      lastRun = new DateTime(props.getOrElse("lastRun", "0"))
    }

    private def saveProperties(location: Point, size: Dimension) {
      val props = new Properties
      props.setProperty("x", location.getX.asInstanceOf[Int].toString)
      props.setProperty("y", location.getY.asInstanceOf[Int].toString)
      props.setProperty("width", size.getWidth.asInstanceOf[Int].toString)
      props.setProperty("height", size.getHeight.asInstanceOf[Int].toString)
      props.setProperty("lastRun", new DateTime().toString)
      writeProperties(new File("executorBenchmark.properties"), props)
    }
  }

  def graphSet(key: Object, value: String) = new BoxPanel(Orientation.Vertical) {
    border = Swing.EmptyBorder(30, 30, 10, 30)
  }
  
  def graphSets = new BoxPanel(Orientation.Vertical) {
    benchmark.ecNameMap.keys.foreach { k =>
      contents += new graphSet(k, benchmark.ecNameMap.get(k))
    }
  }

  private class Navigator extends BoxPanel(Orientation.Horizontal) {
    val navItemHeight:Int = 20
    val buttonRunStop = new Button("Run")
    buttonRunStop.size = new Dimension(navItemHeight, navItemHeight)

//    contents += buttonPrev
    contents += buttonRunStop

//    listenTo(buttonPrev)
    listenTo(buttonRunStop)

    reactions += {
      case ButtonClicked(button) =>
        if (button==buttonRunStop) {
          if (running) {
            benchmark.stop()
            button.text = "Run"
          } else {
            benchmark.run()
            button.text = "Stop"
          }
        }
//        else if (button==buttonNext)
//          index = math.min(Pirates.length-1, index+1)
    }
  }
}
