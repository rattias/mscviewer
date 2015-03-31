/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Aug 2014
 */
package com.cisco.mscviewer.util;

import java.nio.file.WatchEvent;

/**
 *
 * @author rattias
 */
public interface Watcher {

    /**
     *
     * @param parentPath
     * @param ev
     */
    public void event(String parentPath, WatchEvent<?> ev);
}
