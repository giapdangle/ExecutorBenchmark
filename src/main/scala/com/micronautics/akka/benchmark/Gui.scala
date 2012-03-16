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
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.{ChartPanel, ChartFactory}
import org.jfree.chart.renderer.category.BarRenderer
import java.net.URI
import Model.ecNameMap
import java.awt.{Dimension, Cursor, Desktop}
import scala.swing._
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.ui.{TextAnchor, RectangleInsets}
import org.jfree.chart.labels.{ItemLabelPosition, ItemLabelAnchor, StandardCategoryItemLabelGenerator}
import javax.swing.{JPanel, WindowConstants}
import scala.swing.event._
import javax.swing.border.EmptyBorder
import java.util.{Enumeration, Collections, Properties}

/**
  * @author Mike Slinn */
class Gui (benchmark: Benchmark) extends SimpleSwingApplication with PersistableApp {
  private val dataset = new DefaultCategoryDataset()
  private val barChart = ChartFactory.createBarChart("", "", "milliseconds",  dataset, PlotOrientation.HORIZONTAL, true, true, false)
  var chartPanel: ChartPanel = null
  var navigator: Navigator = null
  val barHeight = 125

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

  def addValue(testResult: TestResult, isWarmup: Boolean): DefaultCategoryDataset = {
    dataset.addValue(testResult.millis, testResult.testName, if (isWarmup) Benchmark.strWarmup else Benchmark.strTimed)
    dataset
  }

  def computeChartPanelSize {
    var height = barHeight.toInt * Model.ecNameMap.keys.size
    height = math.max(250, height)
    if (Benchmark.showWarmUpTimes)
      height = height * 2 + 50
    var width = chartPanel.getSize().getWidth.toInt
    chartPanel.setSize(width, height)
  }
  
  def top = new MainFrame {
    var lastRun: DateTime = new DateTime(0)

    peer.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
    title = "Executor Benchmark v0.1"
    size = new Dimension(575, 900)

    contents = new BoxPanel(Orientation.Vertical) {
      val graphs: JPanel = graphSets
      navigator = new Navigator(graphs.getBackground)
      contents += navigator 
      peer.setBackground(graphs.getBackground)
      peer.add(graphs)
      contents += new Label(" ")
      contents += new Label(" ")
      contents += attribution
      border = Swing.EmptyBorder(30, 30, 10, 30)
    }
    pack

    reactions += {
      case WindowOpened(_) =>
        loadProperties
        visible = true

      case WindowClosing(_) =>
        saveProperties(locationOnScreen, size)
        sys.exit(0)
    }

    def graphSets: JPanel = {
      barChart.getTitle.setFont(new Font("SanSerif", 0, 16))
      barChart.setPadding(new RectangleInsets(20, 0, 0, 0))
      barChart.setBackgroundPaint(new Color(0.8f, 0.8f, 0.8f))
      barChart.getLegend.setMargin(20, 0, 0, 0)
      barChart.setAntiAlias(true)

      val renderer = barChart.getCategoryPlot().getRenderer().asInstanceOf[BarRenderer]
      renderer.setDrawBarOutline(false)
      renderer.setMaximumBarWidth(1.0/(ecNameMap.keys.size+1))
      renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator())
      renderer.setBaseItemLabelsVisible(true)
      renderer.setBasePositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.INSIDE3, TextAnchor.CENTER_RIGHT))

      chartPanel = new ChartPanel(barChart, false) {
        setSize(new Dimension(500, 500))
        setBackground(barChart.getBackgroundPaint.asInstanceOf[Color])
      }
      val panel = new JPanel
      panel.setBackground(chartPanel.getBackground)
      panel.add(chartPanel)
      computeChartPanelSize
      panel
    }

    private def loadProperties {
      val props = readProperties(new File("executorBenchmark.properties"))
      location = new Point(props.getOrElse("x", "0").toInt, props.getOrElse("y", "0").toInt)
      size = new Dimension(props.getOrElse("width", "575").toInt, props.getOrElse("height", "900").toInt)
      lastRun = new DateTime(props.getOrElse("lastRun", "0"))
      Benchmark.showWarmUpTimes       = props.getOrElse("showWarmUpTimes",       "false").toBoolean
      Benchmark.doParallelCollections = props.getOrElse("doParallelCollections", "true").toBoolean
      Benchmark.doFutures             = props.getOrElse("doFutures",             "true").toBoolean
      Benchmark.numInterations        = props.getOrElse("numInterations",        "1000").toInt
      Benchmark.consoleOutput         = props.getOrElse("consoleOutput",         "true").toBoolean
      navigator.checkboxFutures.selected    = Benchmark.doFutures
      navigator.checkboxParallel.selected   = Benchmark.doParallelCollections
      navigator.checkboxShowWarmup.selected = Benchmark.showWarmUpTimes
      navigator.enableRunButton
    }

    private def saveProperties(location: Point, size: Dimension) {
      val props = new Properties { // write properties in alpha order
        override def keys = {
          var keyList = new java.util.Vector[Object]()
          var keysEnum = super.keys()
          while (keysEnum.hasMoreElements())
            keyList.add(keysEnum.nextElement())
          Collections.sort(keyList.asInstanceOf[java.util.Vector[String]])
          keyList.elements()
        }
      }
      props.setProperty("consoleOutput",         Benchmark.consoleOutput.toString)
      props.setProperty("doFutures",             Benchmark.doFutures.toString)
      props.setProperty("doParallelCollections", Benchmark.doParallelCollections.toString)
      props.setProperty("numInterations",        Benchmark.numInterations.toString)
      props.setProperty("showWarmUpTimes",       Benchmark.showWarmUpTimes.toString)
      props.setProperty("height",  size.getHeight.asInstanceOf[Int].toString)
      props.setProperty("lastRun", new DateTime().toString)
      props.setProperty("width",   size.getWidth.asInstanceOf[Int].toString)
      props.setProperty("x",       location.getX.asInstanceOf[Int].toString)
      props.setProperty("y",       location.getY.asInstanceOf[Int].toString)
      writeProperties(new File("executorBenchmark.properties"), props)
    }
  }

  class Navigator(bgColor: Color) extends BoxPanel(Orientation.Horizontal) {
    val gap = 25
    background = bgColor
    val buttonRun = new Button("Run")
    buttonRun.size = new Dimension(itemHeight, itemHeight)
    val checkboxShowWarmup = new CheckBox("Show warm-up times") {
      selected = Benchmark.showWarmUpTimes
      border = new EmptyBorder(new Insets(0, 0, 0, gap))
      background = bgColor
    }
    val checkboxParallel = new CheckBox("Time Scala Parallel Collections") {
      selected = Benchmark.doParallelCollections
      border = new EmptyBorder(new Insets(0, 0, 0, gap))
      background = bgColor
    }
    val checkboxFutures = new CheckBox("Time Akka Futures") {
      selected = Benchmark.doFutures
      border = new EmptyBorder(new Insets(0, 0, 0, gap))
      background = bgColor
    }

    val itemHeight:Int = 20
    var index = 0

    contents += checkboxShowWarmup
    contents += checkboxParallel
    contents += checkboxFutures
    contents += buttonRun

    listenTo(checkboxShowWarmup)
    listenTo(checkboxParallel)
    listenTo(checkboxFutures)
    listenTo(buttonRun)

    reactions += {
      case ButtonClicked(`buttonRun`) =>
        computeChartPanelSize
        dataset.clear()
        benchmark.run()
      case ButtonClicked(`checkboxParallel`) =>
        Benchmark.doParallelCollections = checkboxParallel.selected
        enableRunButton
        ExecutorBenchmark.reset
        ExecutorBenchmark.reset
      case ButtonClicked(`checkboxFutures`) =>
        Benchmark.doFutures = checkboxFutures.selected
        enableRunButton
        ExecutorBenchmark.reset
      case ButtonClicked(`checkboxShowWarmup`) =>
        Benchmark.showWarmUpTimes = checkboxShowWarmup.selected
        ExecutorBenchmark.reset
    }
    
    def enableRunButton {
      buttonRun.enabled = checkboxFutures.selected || checkboxParallel.selected
    }
  }
}
