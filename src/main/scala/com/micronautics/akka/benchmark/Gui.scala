package com.micronautics.akka.benchmark

import com.micronautics.util.PersistableApp
import org.joda.time.DateTime
import java.io.File
import java.util.Properties
import swing._
import event._
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.{ChartPanel, ChartFactory}
import javax.swing.{JPanel, WindowConstants}
import org.jfree.ui.RectangleInsets
import org.jfree.chart.renderer.category.BarRenderer
import java.net.URI
import java.awt.{Cursor, Desktop}

/**
 * @author Mike Slinn */
class Gui (benchmark: Benchmark) extends SimpleSwingApplication with PersistableApp {
  var running: Boolean = false
  private val navigator = new Navigator
  private val attribution = new Label() {
    text = "Copyright Micronautics Research Corporation. All rights reserved."
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    listenTo(mouse.clicks, mouse.moves)
    reactions += {
      case MousePressed(src, point, i1, i2, b) =>
        Desktop.getDesktop.browse(new URI("http://micronauticsresearch.com"))
    }
  }

  attribution.peer.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT)

  def top = new MainFrame {
    /** When this program was last run */
    var lastRun: DateTime = new DateTime(0)

    peer.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
    title = "Executor Benchmark v0.1"
    //location = new Point(200, 200)
    //size = new Dimension(400, 400)

    contents = new BoxPanel(Orientation.Vertical) {
      contents += navigator
      peer.add(graphSets)
      contents += new Label(" ")
      contents += new Label(" ")
      contents += attribution
      border = Swing.EmptyBorder(30, 30, 10, 30)
      //preferredSize = new Dimension(400, 400)
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
      size = new Dimension(props.getOrElse("width", "500").toInt, props.getOrElse("height", "600").toInt)
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

  def graphSet(key: Object, chartTitle: String): JPanel = {
    val dataset = new DefaultCategoryDataset()
    //benchmark.ecNameMap
    dataset.addValue(1234, "Warm up", "")
    dataset.addValue(1111, "Timing run", "")

    val barChart = ChartFactory.createBarChart(chartTitle, "", "milliseconds",  dataset, PlotOrientation.HORIZONTAL, true, true, false)
    //barChart.getTitle.setFont(barChart.getTitle.getFont.deriveFont(1))
    barChart.getTitle.setFont(new Font("SanSerif", 0, 16))
    barChart.setPadding(new RectangleInsets(20, 0, 0, 0))

    val renderer = barChart.getCategoryPlot().getRenderer().asInstanceOf[BarRenderer]
    renderer.setDrawBarOutline(false)
    renderer.setSeriesPaint(0, new Color(14, 107, 14));
    renderer.setSeriesPaint(1, new Color(42, 94, 130));

    val chartPanel = new ChartPanel(barChart, false) {
      setPreferredSize(new Dimension(500, 150))
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
