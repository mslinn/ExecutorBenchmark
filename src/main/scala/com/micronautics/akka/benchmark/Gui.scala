package com.micronautics.akka.benchmark

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
import org.jfree.chart.renderer.category.BarRenderer
import java.net.URI
import java.awt.{Cursor, Desktop}
import Model.ecNameMap
import org.jfree.ui.RectangleInsets

/**
  * @author Mike Slinn */
class Gui (benchmark: Benchmark) extends SimpleSwingApplication with PersistableApp {
  var running: Boolean = false
  private val navigator = new Navigator
  private val attribution = new Label() { // The license requires this block to remain untouched
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
    var lastRun: DateTime = new DateTime(0)

    peer.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
    title = "Executor Benchmark v0.1"
    size = new Dimension(575, 900)

    contents = new BoxPanel(Orientation.Vertical) {
      val graphs = graphSets
      peer.setBackground(graphs.getBackground)

      contents += navigator
      peer.add(graphs)
      contents += new Label(" ")
      contents += new Label(" ")
      contents += attribution
      border = Swing.EmptyBorder(30, 30, 10, 30)
    }

    reactions += {
      case WindowOpened(_) =>
        loadProperties
        visible = true

      case WindowClosing(_) =>
        saveProperties(locationOnScreen, size)
        sys.exit(0)
    }

    def graphSets: JPanel = {
      val dataset = new DefaultCategoryDataset()
      var i = 100
      ecNameMap.keys.foreach { k =>
        dataset.addValue(1234, ecNameMap.get(k).get, "Warm-up")
        dataset.addValue(1111, ecNameMap.get(k).get, "Timed")
        i = i + 100
      }

      val barChart = ChartFactory.createBarChart("", "", "milliseconds",  dataset, PlotOrientation.HORIZONTAL, true, true, false)
      barChart.getTitle.setFont(new Font("SanSerif", 0, 16))
      barChart.setPadding(new RectangleInsets(20, 0, 0, 0))
      barChart.setBackgroundPaint(new Color(0.8f, 0.8f, 0.8f))
      barChart.getLegend.setMargin(20, 0, 0, 0)
      barChart.setAntiAlias(true)

      val renderer = barChart.getCategoryPlot().getRenderer().asInstanceOf[BarRenderer]
      renderer.setDrawBarOutline(false)
      //renderer.setSeriesPaint(0, new Color(14, 107, 14));
      //renderer.setSeriesPaint(1, new Color(42, 94, 130));
      renderer.setMaximumBarWidth(1.0/(ecNameMap.keys.size+1))

      val chartPanel = new ChartPanel(barChart, false) {
        setPreferredSize(new Dimension(500, 50 * ecNameMap.keys.size))
        setBackground(barChart.getBackgroundPaint.asInstanceOf[Color])
      }
      val panel = new JPanel
      panel.setBackground(chartPanel.getBackground)
      panel.add(chartPanel)
      panel
    }

    private def loadProperties {
      val props = readProperties(new File("executorBenchmark.properties"))
      location = new Point(props.getOrElse("x", "0").toInt, props.getOrElse("y", "0").toInt)
      size = new Dimension(props.getOrElse("width", "575").toInt, props.getOrElse("height", "900").toInt)
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
