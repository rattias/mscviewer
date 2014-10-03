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

class Tokenizer {
    //	String toks[] = new String[] {
    //			null, null, "(", ")", ">=", "<=", ">", "<", "=", 
    //			"or", "and", "not", "label", "time", "duration",
    //			"startswith", "endswith", "contains", "ns", "us", 
    //			"ms", "s", "event", "interaction", null, "where",
    //			"type", "source", "sink", "[", "]"
    //	};
    private ArrayList<Token> tokens = new ArrayList<Token>();

    public static void main(String args[]) {
        Tokenizer t= new Tokenizer(args[0]);
        t.print();
    }

    public Tokenizer(String txt) {
        int start = 0;
        int end;
        int len = txt.length();
        outer:
            while(true) {
                //skip spaces
                while (start<len && txt.charAt(start) == ' ') {
                    start++;
                }
                if (start == len)
                    break;
                if (txt.charAt(start) == Token.COMPL_CHAR) {
                    tokens.add(new Token(Token.TT.UNKNOWN, ""+Token.COMPL_CHAR));
                    start++;
                    continue;
                }

                // add known token
                for(Token.TT tt : Token.TT.values()) {
                    String str = tt.toString();
                    if (str != null && txt.substring(start).startsWith(str)) {
                        int toklen = str.length() ;
                        if (! Character.isLetter(str.charAt(0)) ||
                                start + toklen == len ||
                                (! Character.isLetter(txt.charAt(start+toklen)) &&
                                        txt.charAt(start+toklen) != Token.COMPL_CHAR)) {
                            tokens.add(new Token(tt, tt.toString()));
                            start += toklen;
                            continue outer;
                        }
                    }
                }
                end = start;
                // parse number
                if (Character.isDigit(txt.charAt(start))) {
                    while(end < len && Character.isDigit(txt.charAt(end)))
                        end++;
                    String s = txt.substring(start, end);
                    long v= Long.parseLong(s);
                    tokens.add(new Token(v, s));
                    start = end;
                    continue;
                }
                end = start;

                if (txt.charAt(start) == '"') {
                    end = start+1;
                    while(end < len && (txt.charAt(end)!='"'))
                        end++;				
                    if (end == len) {
                        tokens.add(new Token(Token.TT.UNKNOWN, txt.substring(start, end)));
                        break;
                    } else {
                        end++;
                        tokens.add(new Token(Token.TT.STRING, txt.substring(start, end)));
                        start = end;
                        continue;
                    }
                }
                //parse anything else
                while(end < len && (! Character.isWhitespace(txt.charAt(end))))
                    end++;
                tokens.add(new Token(Token.TT.UNKNOWN, txt.substring(start, end)));
                start = end;		
            }
    }

    public void print() {
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    public int getTokenCount() {
        return tokens.size();
    }

    public Token getTokenAt(int pos) {
        return tokens.get(pos);
    }
}