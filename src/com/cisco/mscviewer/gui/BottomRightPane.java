/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
package com.cisco.mscviewer.gui;

import java.awt.Component;

import javax.swing.JTabbedPane;

import com.cisco.mscviewer.util.PNGSnapshotTarget;

/**
 * @author Roberto Attias
 * @since Jan 2012
 */
@SuppressWarnings("serial")
class BottomRightPane extends JTabbedPane implements PNGSnapshotTarget {
    public BottomRightPane() {
        setName("BottomRightPane");
    }

    @Override
    public Component getPNGSnapshotClient() {
        return null;
    }
}
