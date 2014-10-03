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
package com.cisco.mscviewer.model;

public class EventRange {
    private final int fromIdx, toIdx;
    private final MSCDataModel dm;

    public EventRange(MSCDataModel dm, int fromIdx, int toIdx) {
        this.dm = dm;
        this.fromIdx = fromIdx;
        this.toIdx = toIdx;
    }

    public int getSize() {
        return toIdx-fromIdx+1;
    }
    public Event getEventAtIndex(int idx) {
        return dm.getEventAt(fromIdx+idx);
    }
}
