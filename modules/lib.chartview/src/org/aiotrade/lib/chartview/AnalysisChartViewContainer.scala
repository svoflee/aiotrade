/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.chartview

import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import org.aiotrade.lib.charting.view.ChartViewContainer
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.WithDrawingPane
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor
import org.aiotrade.lib.math.timeseries.computable.Indicator
import org.aiotrade.lib.math.timeseries.computable.IndicatorDescriptor
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.util.collection.ArrayList


/**
 *
 * @author Caoyuan Deng
 */
class AnalysisChartViewContainer extends ChartViewContainer {
    
  override def init(focusableParent: Component, controller: ChartingController) {
    super.init(focusableParent, controller)
  }
    
  protected def initComponents {
    setLayout(new GridBagLayout)
    val gbc = new GridBagConstraints
    gbc.fill = GridBagConstraints.BOTH
    gbc.gridx = 0
    gbc.weightx = 100
    gbc.weighty = 618
        
    val quoteSer = getController.getMasterSer.asInstanceOf[QuoteSer]
    quoteSer.shortDescription = getController.getContents.uniSymbol
    val quoteChartView = new AnalysisQuoteChartView(getController, quoteSer)
    setMasterView(quoteChartView, gbc)
        
    /** use two list to record the active indicators and their order(index) for later showing */
    val indicatorDescriptorsToBeShowing = new ArrayList[IndicatorDescriptor]
    val  indicatorsToBeShowing = new ArrayList[Indicator]
    for (descriptor <- getController.getContents.lookupDescriptors(classOf[IndicatorDescriptor])) {
      if (descriptor.active && descriptor.freq.equals(getController.getMasterSer.freq)) {
        descriptor.serviceInstance(getController.getMasterSer) foreach {indicator =>
          /**
           * @NOTICE
           * As the quoteSer may has been loaded, there may be no more UpdatedEvent
           * etc. fired, so, computeFrom(0) first.
           */
          indicator.computeFrom(0) // don't remove me
                    
          if (indicator.isOverlapping) {
            addSlaveView(descriptor, indicator, null)
          } else {
            /** To get the extract size of slaveViews to be showing, store them first, then add them later */
            indicatorDescriptorsToBeShowing += descriptor
            indicatorsToBeShowing += indicator
          }
        }
      }
    }
        
    /** now add slaveViews, the size has excluded those indicators not showing */
    val size = indicatorDescriptorsToBeShowing.size
    for (i <- 0 until size) {
      gbc.weighty = 382f / size.floatValue
      addSlaveView(indicatorDescriptorsToBeShowing(i), indicatorsToBeShowing(i), gbc)
    }
        
    for (descriptor <- getController.getContents.lookupDescriptors(classOf[DrawingDescriptor])) {
      if (descriptor.freq.equals(getController.getMasterSer.freq)) {
        descriptor.serviceInstance(getMasterView) foreach {drawing =>
          getMasterView.asInstanceOf[WithDrawingPane].addDrawing(descriptor, drawing)
        }
      }
    }
  }
    
}