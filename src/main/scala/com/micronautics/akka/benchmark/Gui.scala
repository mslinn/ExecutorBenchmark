package com.micronautics.akka.benchmark

import com.micronautics.util.PersistableApp
import org.joda.time.DateTime
import java.io.File
import java.util.Properties
import swing._
import event.{ButtonClicked, WindowOpened, WindowClosing}
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.{ChartPanel, ChartFactory}
import java.awt.{BorderLayout, Dimension, Point}
import javax.swing.{JPanel, WindowConstants}

/**
 * @author Mike Slinn */
class Gui (benchmark: Benchmark) extends SimpleSwingApplication with PersistableApp {
  var running: Boolean = false
  private val navigator = new Navigator
  private val attribution = new Label("Copyright Micronautics Research Corporation. All rights reserved.")

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
      contents += attribution
      peer.add(graphSets)
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

  def graphSet(key: Object, value: String): JPanel = {
    val dataset = new DefaultCategoryDataset()
    //benchmark.ecNameMap
    dataset.addValue(1.0, "Row 1", "Column 1")
    dataset.addValue(5.0, "Row 1", "Column 2")
    dataset.addValue(3.0, "Row 1", "Column 3")
    dataset.addValue(2.0, "Row 2", "Column 1")
    dataset.addValue(3.0, "Row 2", "Column 2")
    dataset.addValue(2.0, "Row 2", "Column 3")

    val barChart = ChartFactory.createBarChart(
      "Bar Chart Demo", // chart title
      "Category", // domain axis label
      "Value", // range axis label
      dataset,
      PlotOrientation.HORIZONTAL, // orientation
      true, // include legend
      true, // tooltips?
      false // URLs?
    )
    val chartPanel = new ChartPanel(barChart, false) {
      setPreferredSize(new Dimension(500, 270))
    }
    val panel = new JPanel
    panel.add(chartPanel)
    panel
  }

  def graphSets = {
    val panel = new JPanel
    benchmark.ecNameMap.keys.foreach { k =>
      panel.add(graphSet(k, benchmark.ecNameMap.get(k).get))
    }
    panel
  }

  private class Navigator extends BoxPanel(Orientation.Horizontal) {
    val navItemHeight:Int = 20
    val buttonRunStop = new Button("Run")
    buttonRunStop.size = new Dimension(navItemHeight, navItemHeight)

    contents += buttonRunStop
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
    }
  }
}
