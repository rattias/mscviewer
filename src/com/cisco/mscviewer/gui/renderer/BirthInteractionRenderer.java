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


public class BirthInteractionRenderer extends DefaultInteractionRenderer {

    /**
     *
     * @param props
     * @param ev
     */
    public void setup(JSonObject props, Event ev) {
        props.clear();
        props.set("color", "000000");
        props.set("dashed", "true");
        props.set("draw_tip", "false");
        props.set("stroke_width", "2.0");
        super.setup(props, ev);
    }
}
