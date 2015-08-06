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
import com.cisco.mscviewer.util.PersistentPrefs;

/**
 * @author Roberto Attias
 * @since Aug 2014
 */
@SuppressWarnings("serial")
class CustomButton extends JToggleButton {

    transient private final Entity en;

    public CustomButton(Entity en) {
        String p = en.getPath();
        int idx = p.lastIndexOf('/');
        if (idx >= 0) {
            String parent = p.substring(0, idx+1);
            String name = p.substring(idx+1);
            setText("<HTML>"+parent+ "<BR><center>"+name+((en.getDescription() != null) ? "("+en.getDescription()+")" : "") + "</center></BR></HTML");
        } else
            setText("<HTML>"+p+"</HTML>");
        this.en = en;
    }

    public void updateText() {
        System.out.println("Updating text");
        PersistentPrefs p = MainFrame.getInstance().getPrefs();
        StringBuilder sb = new StringBuilder("<HTML>");
        String s = p.getShowEntityAsID() ? en.getId() : en.getPath();
        int idx = s.lastIndexOf('/');
        String name = s.substring(idx+1);
        if (p.getShowEntityFullPath() && idx >= 0) {
            String parent = s.substring(0, idx+1);
            sb.append(parent+"<BR>");
        }
        sb.append(name);
        if (p.getShowEntityDescription()) {
            String descr = en.getDescription() != null? en.getDescription() : "";
            sb.append("<BR>"+descr);
        }    
        setText(sb.toString());
    }
    
        
    public Entity getEntity() {
        return en;
    }
}
