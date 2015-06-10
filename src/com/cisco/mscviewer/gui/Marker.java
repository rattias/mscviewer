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

    ERROR(Color.red, "error"), 
    RED(Color.red, "red"), 
    YELLOW(new Color(0xFFFF99), "yellow"), 
    BLUE(new Color(0x99FFFF), "blue"), 
    GREEN(new Color(0x29FF29), "green");

    private Color transparentColor;
    private final Color color;
    private final String name;

    public static Marker forColor(Color c) {
        for(Marker m: Marker.values()) {
            if (m.getColor().equals(c))
                return m;
        }
        return null;
    }
    
    Marker(Color c, String name) {
        color = c;
        this.name = name;
    }

    public Color getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public Color getTransparentColor() {
        if (transparentColor == null)
            transparentColor = new Color(color.getRed(), color.getGreen(),
                    color.getBlue(), 128);
        return transparentColor;
    }

}
