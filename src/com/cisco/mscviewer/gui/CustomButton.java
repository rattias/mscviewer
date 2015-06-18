/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
package com.cisco.mscviewer.gui;

import javax.swing.JToggleButton;

import com.cisco.mscviewer.model.Entity;

/**
 * @author Roberto Attias
 * @since Aug 2014
 */
@SuppressWarnings("serial")
class CustomButton extends JToggleButton {
    private final Entity en;

    public CustomButton(Entity en) {
        String p = en.getPath();
        int idx = p.lastIndexOf('/');
        if (idx >= 0) {
            String parent = p.substring(0, idx+1);
            String name = p.substring(idx+1);
            setText("<HTML>"+parent+ "<BR><center>"+name+"</center></BR></HTML");
        } else
            setText("<HTML>"+p+"</HTML>");
        this.en = en;
    }

    public Entity getEntity() {
        return en;
    }
}
