/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
package com.cisco.mscviewer.gui;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cisco.mscviewer.model.*;

/**
 * @author Roberto Attias
 * @since  Jan 2012
 */
class CompactViewFilter implements MSCDataModelEventFilter { 
    private final ViewModel m;
    private Matcher matcher;

    public CompactViewFilter(ViewModel m, String filterRegExp) {
        this.m = m;
        if (m == null)
            throw new NullPointerException("null EntityHeaderModel");
        if (filterRegExp != null) {
            matcher = Pattern.compile(filterRegExp).matcher("");
        }
    }

    @Override
    public boolean filter(Event ev) {
        return 
        m.indexOf(ev.getEntity()) != -1 && 
        (matcher != null ? matcher.reset(ev.getLabel()).matches() : true);
    }

}
