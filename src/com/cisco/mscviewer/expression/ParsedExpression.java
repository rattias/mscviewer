/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since   Aug 2012
 */
package com.cisco.mscviewer.expression;

public class ParsedExpression {
    private final Token token;

    ParsedExpression(Token t) {
        token = t;
    }

    public void printRPN() {
        token.printRPN();
    }

    public Token getFirstToken() {
        return token;
    }
}
