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

import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.JSonObject;
import com.cisco.mscviewer.model.JSonStringValue;
import com.cisco.mscviewer.model.JSonValue;

public class TransitionRenderer extends DefaultInteractionRenderer {
    @Override
    public void setup(JSonObject props, Event ev) {
        final JSonObject p = new JSonObject();
        JSonValue v = props.get("color");
        p.set("color", new JSonStringValue((v != null) ? v.toString()
                : "00AA00"));

        v = props.get("stroke_width");
        p.set("stroke_width", new JSonStringValue((v != null) ? v.toString()
                : "2.0"));

        p.set("draw_tip", new JSonStringValue("false"));
        p.set("straight", new JSonStringValue("true"));
        super.setup(p, ev);
    }

}
