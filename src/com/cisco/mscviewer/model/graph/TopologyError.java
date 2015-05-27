/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jan 2014
 */
package com.cisco.mscviewer.model.graph;

@SuppressWarnings("serial")
public class TopologyError extends Exception {
    public TopologyError(String s) {
        super(s);
    }
}
