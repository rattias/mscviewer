/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jan 2012
 */
package com.cisco.mscviewer.gui.renderer;

import com.cisco.mscviewer.model.JSonObject;

import com.cisco.mscviewer.model.Event;

public class TransitionRenderer extends DefaultInteractionRenderer {
    public void setup(JSonObject props, Event ev) {
        JSonObject p = new JSonObject();
        String v = (String)props.get("color");
        p.set("color", (v != null) ? v : "00AA00");

        v = (String)props.get("stroke_width");
        p.set("stroke_width", (v != null) ? v : "2.0");

        p.set("draw_tip", "false");
        p.set("straight", "true");
        super.setup(p, ev);
    }

}
