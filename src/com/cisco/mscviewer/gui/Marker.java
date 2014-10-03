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
package com.cisco.mscviewer.gui;

import java.awt.Color;

public enum Marker {

    ERROR(Color.red),
    RED(Color.red),
    YELLOW(new Color(0xFFFF99)),
    BLUE(new Color(0x99FFFF)),
    GREEN(new Color(0x29FF29));

    private Color transparentColor;
    private final Color color;

    Marker(Color c) {
        color = c;
    }

    public Color getColor() {
        return color;
    }

    public Color getTransparentColor() {
        if (transparentColor == null)
            transparentColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);
        return transparentColor;
    }

}
