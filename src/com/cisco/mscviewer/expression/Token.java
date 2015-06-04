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

public class Token {
    final static char COMPL_CHAR = '$';

    // String toks[] = new String[] {
    // null, null, "(", ")", ">=", "<=", ">", "<", "=",
    // "or", "and", "not", "label", "time", "duration",
    // "startswith", "endswith", "contains", "ns", "us",
    // "ms", "s", "event", "interaction", null, "where",
    // "type", "source", "sink", "[", "]"
    // };
    enum TT {
        STRING(null), NUM(null), OPEN("("), CLOSE(")"), GEQ(">="), LEQ("<="), GT(
                ">"), LT("<"), EQ("="), OR("or"), AND("and"), NOT("not"), LABEL(
                "label"), NOTE("note"), TIME("time"), DURATION("duration"), STARTSWITH(
                "startswith"), ENDSWITH("endswith"), CONTAINS("contains"), UNKNOWN(
                null), TYPE("type"), SOURCE("source"), SINK("sink"), OPEN_SQUARE(
                "["), CLOSE_SQUARE("]"), NS("ns"), US("us"), MS("ms"), S("s");
        String s;

        TT(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }
    };

    TT type;
    String string;
    long num;
    Token l, r;

    Token(TT type, String str) {
        this.type = type;
        this.string = str;
    }

    Token(long num, String str) {
        this.type = TT.NUM;
        this.num = num;
        this.string = str;
    }

    @Override
    public String toString() {
        return string;
    }

    public void printRPN() {
        System.out.print(string + " ");
        if (l != null)
            l.printRPN();
        if (r != null)
            r.printRPN();
    }

}