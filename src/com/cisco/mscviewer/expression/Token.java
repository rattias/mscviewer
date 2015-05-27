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
                "label"), TIME("time"), DURATION("duration"), STARTSWITH(
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

    // final static int TT_STRING=0;
    // final static int TT_NUM=1;
    // final static int TT_OPEN=2;
    // final static int TT_CLOSE=3;
    // final static int TT_GEQ=4;
    // final static int TT_LEQ=5;
    // final static int TT_GT=6;
    // final static int TT_LT=7;
    // final static int TT_EQ=8;
    // final static int TT_OR=9;
    // final static int TT_AND=10;
    // final static int TT_NOT=11;
    // final static int TT_LABEL=12;
    // final static int TT_TIME=13;
    // final static int TT_DURATION=14;
    // final static int TT_STARTSWITH=15;
    // final static int TT_ENDSWITH=16;
    // final static int TT_CONTAINS=17;
    // final static int TT_NS=18;
    // final static int TT_US=19;
    // final static int TT_MS=20;
    // final static int TT_S=21;
    // final static int TT_EVENT=22;
    // final static int TT_INTERACTION=23;
    // final static int TT_UNKNOWN=24;
    // final static int TT_WHERE=25;
    // final static int TT_TYPE=26;
    // final static int TT_SOURCE=27;
    // final static int TT_SINK=28;
    // final static int TT_OPEN_SQUARE=29;
    // final static int TT_CLOSE_SQUARE=30;
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