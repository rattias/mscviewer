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
package com.cisco.mscviewer.model;


public interface EntityHeaderModelListener {
    void entityAdded(ViewModel eh, Entity en, int idx);    
    void entityRemoved(ViewModel eh, Entity en, int idx);    
    void entitySelectionChanged(ViewModel eh, Entity en, int idx);
    void entityMoved(ViewModel eh, Entity en, int toIdx);
    int getEntityHeaderModelNotificationPriority();
    void boundsChanged(ViewModel entityHeaderModel, Entity en, int idx);
}
