/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jun 2011
 */
package com.cisco.mscviewer.gui;

import java.awt.Component;

import javax.swing.JSplitPane;

import com.cisco.mscviewer.util.PNGSnapshotTarget;

@SuppressWarnings("serial")
class LeftPane extends JSplitPane implements PNGSnapshotTarget {
    public LeftPane(Component top, Component bottom) {
        super(JSplitPane.VERTICAL_SPLIT, top, bottom);
        setName("LeftPane");
    }

    @Override
    public Component getPNGSnapshotClient() {
        return null;
    }
}
