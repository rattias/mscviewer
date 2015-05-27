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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

public class ParserState {
    public static int COMPLETION = 1;
    String txt;
    Properties props;
    Tokenizer tk;
    int pos;
    HashSet<String> completion;

    public ParserState(String txt) {
        this(txt, 0);
    }

    public ParserState(String txt, int flags) {
        if ((flags & COMPLETION) != 0)
            txt += Token.COMPL_CHAR;
        pos = -1;
        props = new Properties();
        completion = new HashSet<String>();
        tk = new Tokenizer(txt);
        // tk.print();
    }

    void setPos(int p) {
        pos = p;
    }

    int getPos() {
        return pos;
    }

    Token next() throws NoMoreTokensException {
        try {
            setPos(getPos() + 1);
            return tok();
        } catch (final IndexOutOfBoundsException ex) {
            throw new NoMoreTokensException();
        }
    }

    Token prev() {
        setPos(getPos() - 1);
        return tok();
    }

    void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    String getProperty(String key) {
        return props.getProperty(key);
    }

    Token tok() {
        return tk.getTokenAt(pos);
    }

    void compl(String str) {
        String t = tok().string;
        if (t.charAt(t.length() - 1) == Token.COMPL_CHAR) {
            t = t.substring(0, t.length() - 1);
            if (str.startsWith(t))
                completion.add(str);
        }
    }

    public ArrayList<String> getCompletions() {
        final ArrayList<String> al = new ArrayList<String>();
        al.addAll(completion);
        Collections.sort(al);
        return al;
    }

    public int getTokenCount() {
        return tk.getTokenCount();
    }

}