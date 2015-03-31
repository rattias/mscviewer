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

@SuppressWarnings("serial")
public class JSonException extends Exception {

    private String file, input;
    private int line, column;

    public JSonException(String msg) {
        super(msg);
    }

    public JSonException(String file, int line, int column, String input, String msg) {
        super(msg);
        this.file = file;
        this.line = line;
        this.column = column;
        this.input = input;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('\n').append(input).append('\n');
        for(int i=0; i<column; i++)
            sb.append(' ');
        sb.append("^\n");
        if (file != null) {
            sb.append(file).append(":");
        }
        if (line >= 0) {
            sb.append(line).append(":");
        }
        if (column >= 0) {
            sb.append(column).append(":");
        }
        sb.append(getMessage());
        return sb.toString();
    }

    int position() {
        return column;
    }
}
