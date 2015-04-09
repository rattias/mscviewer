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
package com.cisco.mscviewer.io;

import java.io.IOException;

import com.cisco.mscviewer.model.MSCDataModel;

public interface Loader {
    void waitIfLoading();
    void load(final String fname, final MSCDataModel dm, boolean batchMode) throws IOException;
}
