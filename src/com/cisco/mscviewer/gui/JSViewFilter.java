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

import com.cisco.mscviewer.expression.ExpressionParser;
import com.cisco.mscviewer.expression.ParsedExpression;
import com.cisco.mscviewer.model.*;

class JSViewFilter implements MSCDataModelEventFilter {
    private final boolean filterEvents;
    private final ExpressionParser exp = new ExpressionParser();
    private final ParsedExpression expr;
    private final ViewModel ehm;

    public JSViewFilter(MSCDataModel dm, ViewModel ehm, ParsedExpression e) {
        this.ehm = ehm;
        this.expr = e;
        filterEvents =  (expr != null); // && expr.getFirstToken().toString().equals("event");
    }

    @Override
    public boolean filter(Event ev) {
        return ehm.indexOf(ev.getEntity()) != -1 && 
        (filterEvents ? exp.evaluateAsJavaScriptonEvent(ev, expr) : true);
    }

}
