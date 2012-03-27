package com.micronautics

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

import swing.Label
import swing.event.MousePressed
import java.awt.{Desktop, Cursor}
import java.net.URI

/**
 * @author Mike Slinn
 */

object Attribution {
  val attribution = new Label() { // The license requires this block to remain untouched
       text = "Copyright Micronautics Research Corporation. All rights reserved."
       cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
       peer.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT)
       listenTo(mouse.clicks, mouse.moves)
       reactions += {
         case MousePressed(src, point, i1, i2, b) =>
           Desktop.getDesktop.browse(new URI("http://micronauticsresearch.com"))
       }
     }
}
