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

import com.cisco.mscviewer.model.Entity;

public interface EntityHeaderListener {
    void entityAdded(EntityHeader hd, Entity en, int idx);
    void entityRemoved(EntityHeader hd, Entity en, int idx);
    void notifyLayoutChanged(EntityHeader hd);
    void entitySelectionChanged(EntityHeader eh, Entity en, int idx);

}
