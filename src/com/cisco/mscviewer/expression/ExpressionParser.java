/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *------------------------------------------------------------------*/

package com.cisco.mscviewer.expression;

import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.util.MSCViewerError;
import com.cisco.mscviewer.util.Report;

/**
 * Parser for the expression grammar accepted by search fields. THe grammar is 
 * as follows:
 * <pre>
 * GRAMMAR:
 * start   ::= evorexp | orexp
 * evorexp ::= evandexp ( 'or' evorexp)*
 * evandexp::= evunexp (and avandexp)* 
 * evunexp ::= evterm | '(' evorexp ')' | 'not' evorexp
 * evterm  ::= ('source' '[' orexp ']')| ('sink' '[' orexp ']') | durcomp    
 *  orexp  ::= andexp ( 'or' orexp)*
 * andexp  ::= unexp (and andexp)*
 *  unexp  ::= term | '(' orexp ')' | 'not' orexp
 *   term  ::= stringcomp | timecomp  
 * stringcomp::= 'label' ('is' | 'contains' | 'starts-with' | 'ends-with') STRINGVAL
 * timecomp ::= 'time' ('=' | '<' | '>' | '<=' | '>=') TIME
 * durcomp ::= 'duration' ('=' | '<' | '>' | '<=' | '>=') LONG ('ns' | 'us' | 'ms' | 's')
 * </pre>
 * 
 * @author Roberto Attias
 * @since   May 2012
 */

public class ExpressionParser {
    private static ScriptEngine engine;
    static {
        final ScriptEngineManager mgr = new ScriptEngineManager();
        final List<ScriptEngineFactory> factories = mgr.getEngineFactories();
        for (final ScriptEngineFactory factory : factories) {
            final String langName = factory.getLanguageName();

            if ("ECMAScript".equals(langName)) {
                engine = factory.getScriptEngine();
                break;
            }
        }
        if (engine == null) {
            throw new MSCViewerError("Unable to retrieve JavaScript engine.");
        }
    }

    static final int LEFT = 0;
    static final int RIGHT = 1;

    /*
     * start ::= 'event' [type=event] 'where' orexp | 'interaction' [type=int]
     * 'where' orexp orexp ::= andexp ( 'or' orexp)* andexp ::= unexp (and
     * andexp)* unexp ::= term | '(' orexp ')' | 'not' orexp term ::= stringcomp
     * | ?[type==event] timecomp | ?[type==int] durcomp stringcomp::= 'label'
     * ('is' | 'contains' | 'starts-with' | 'ends-with') STRINGVAL timecomp ::=
     * 'time' ('=' | '<' | '>' | '<=' | '>=') TIME durcomp ::= 'duration' ('=' |
     * '<' | '>' | '<=' | '>=') LONG ('ns' | 'us' | 'ms' | 's')
     */

    public static void main(String[] args) {
        final ParserState ps = new ParserState(args[0]);
        final ParsedExpression expr = new ExpressionParser().parse(ps);
        if (expr != null) {
            expr.printRPN();
            System.out.println();
            System.out.println(toJS(expr.getFirstToken()));
        }
        if (args[0].endsWith("" + Token.COMPL_CHAR)) {
            final List<String> compl = ps.getCompletions();
            System.out.println("completions:");
            for (final String tt : compl) {
                System.out.println("    " + tt);
            }
        }
    }

    public static String toJS(Token t) {
        switch (t.type) {
        // case Token.TT.EVENT:
        // case Token.TT.INTERACTION:
        // case Token.TT.WHERE: return toJS(t.l);

        case OPEN:
            return "(" + toJS(t.l) + ")";
        case AND:
            return toJS(t.l) + " && " + toJS(t.r);
        case OR:
            return toJS(t.l) + " || " + toJS(t.r);
        case NOT:
            return "!" + toJS(t.l);
        case CONTAINS:
            return "(" + toJS(t.l) + ".indexOf(" + toJS(t.r) + ") >= 0)";
        case ENDSWITH:
            return "(" + toJS(t.l) + ".match(" + toJS(t.r) + "$) == "
                    + toJS(t.r) + ")";
        case STARTSWITH:
            return "(" + toJS(t.l) + ".indexOf(" + toJS(t.r) + ") == 0)";
        case STRING:
            return t.string;
        case NUM:
            return "" + t.num;
        case EQ:
            if (t.l.type == Token.TT.LABEL)
                return "(" + toJS(t.l) + " == " + toJS(t.r) + ")";
            else if (t.l.type == Token.TT.TYPE)
                return "(" + toJS(t.l) + " == " + toJS(t.r) + ")";
            else if (t.l.type == Token.TT.NOTE)
                return "(" + toJS(t.l) + " == " + toJS(t.r) + ")";
            else if (t.l.type == Token.TT.TIME)
                return toJS(t.l) + ".equals(DateFormat.parse(" + toJS(t.r)
                        + "))";
            else if (t.l.type == Token.TT.DURATION)
                return toJS(t.l) + "==" + toJS(t.r);
            else
                throw new Error("Unsupported field type");
        case LABEL:
            return "label";
        case TYPE:
            return "type";
        case NOTE:
            return "note";
        case TIME:
            return "time";
        case UNKNOWN:
            return "{" + t.string + "}";
        default:
            return "[" + t.type + "]";

        }
    }

    public boolean evaluateAsJavaScriptonEvent(Event ev, ParsedExpression expr) {
        try {
            engine.put("label", ev.getLabel());
            engine.put("type", ev.getType());
            engine.put("time", ev.getTimestampRepr());
            final String type = ev.getType();
            engine.put("type", type);
            final String note = ev.getNote();
            if (note != null)
                engine.put("note", note);
            else
                engine.put("note", "____@@@@invalid node@@@@____");
            final String js = toJS(expr.getFirstToken());
            final boolean b = (Boolean) engine.eval(js);
            return b;
        } catch (final ScriptException e) {
            Report.exception("Exception while running script", e);
        }
        return false;
    }

    public boolean evaluateAsJavaScriptonInteraction(Interaction in,
            ParsedExpression expr) {
        try {
            Event ev = in.getFromEvent();
            if (ev != null) {
                engine.put("source.label", ev.getLabel());
                engine.put("source.type", ev.getType());
                engine.put("source.time", ev.getTimestampRepr());
                engine.put("source.type", ev.getType());
            }
            ev = in.getToEvent();
            if (ev != null) {
                engine.put("sink.label", ev.getLabel());
                engine.put("sink.type", ev.getType());
                engine.put("sink.time", ev.getTimestampRepr());
                engine.put("sink.type", ev.getType());
            }
            final String js = toJS(expr.getFirstToken());
            final boolean b = (Boolean) engine.eval(js);
            return b;
        } catch (final ScriptException e) {
            Report.exception("Exception while running script", e);
        }
        return false;
    }

    /*
     * start ::= evorexp | orexp
     */
    public ParsedExpression parse(ParserState ps) {
        final int pos = ps.getPos();
        Token l = evorexp(ps);
        if (l != null)
            return new ParsedExpression(l);
        l = orexp(ps);
        if (l != null)
            return new ParsedExpression(l);
        ps.setPos(pos);
        return null;
    }

    /**
     * orexp ::= andexp ( 'or' orexp)*
     * 
     * @param ps
     * @return
     */
    public Token orexp(ParserState ps) {
        final int pos = ps.getPos();
        final Token l = andexp(ps);
        if (l == null) {
            ps.setPos(pos);
            return null;
        }
        try {
            ps.next();
        } catch (final NoMoreTokensException ex) {
            ps.prev();
            return l;
        }
        final Token op = ps.tok();
        if (op.type == Token.TT.OR) {
            final Token r = orexp(ps);
            if (r == null) {
                ps.setPos(pos);
                return null;
            } else {
                op.l = l;
                op.r = r;
                return op;
            }
        } else {
            ps.compl(Token.TT.OR.toString());
            ps.prev();
            return l;
        }
    }

    /**
     * evorexp ::= evandexp ( 'or' evorexp)*
     * 
     * @param ps
     * @return
     */
    public Token evorexp(ParserState ps) {
        final int pos = ps.getPos();
        final Token l = evandexp(ps);
        if (l == null) {
            ps.setPos(pos);
            return null;
        }
        try {
            ps.next();
        } catch (final NoMoreTokensException ex) {
            ps.prev();
            return l;
        }
        final Token op = ps.tok();
        if (op.type == Token.TT.OR) {
            final Token r = evorexp(ps);
            if (r == null) {
                ps.setPos(pos);
                return null;
            } else {
                op.l = l;
                op.r = r;
                return op;
            }
        } else {
            ps.compl(Token.TT.OR.toString());
            ps.prev();
            return l;
        }
    }

    /**
     * andexp ::= unexp (and andexp)*
     * 
     * @param ps
     * @return
     */
    public Token andexp(ParserState ps) {
        final int pos = ps.getPos();
        final Token l = unexp(ps);
        if (l == null) {
            ps.setPos(pos);
            return null;
        }

        try {
            ps.next();
        } catch (final NoMoreTokensException ex) {
            ps.prev();
            return l;
        }
        final Token op = ps.tok();
        if (op.type == Token.TT.AND) {
            final Token r = andexp(ps);
            if (r == null) {
                ps.setPos(pos);
                return null;
            } else {
                op.l = l;
                op.r = r;
                return op;
            }
        } else {
            ps.compl(Token.TT.AND.toString());
            ps.prev();
            return l;
        }
    }

    /**
     * evandexp ::= evunexp (and evandexp)*
     * 
     * @param ps
     * @return
     */
    public Token evandexp(ParserState ps) {
        final int pos = ps.getPos();
        final Token l = evunexp(ps);
        if (l == null) {
            ps.setPos(pos);
            return null;
        }

        try {
            ps.next();
        } catch (final NoMoreTokensException ex) {
            ps.prev();
            return l;
        }
        final Token op = ps.tok();
        if (op.type == Token.TT.AND) {
            final Token r = evandexp(ps);
            if (r == null) {
                ps.setPos(pos);
                return null;
            } else {
                op.l = l;
                op.r = r;
                return op;
            }
        } else {
            ps.compl(Token.TT.AND.toString());
            ps.prev();
            return l;
        }
    }

    /**
     * unexp ::= term | '(' orexp ')' | 'not' orexp
     * 
     * @param ps
     * @return
     */
    public Token unexp(ParserState ps) {
        final int pos = ps.getPos();
        try {
            Token l = term(ps);
            if (l != null)
                return l;
            ps.next();
            final Token op = ps.tok();
            if (op.type == Token.TT.OPEN) {
                if ((l = orexp(ps)) != null) {
                    if (ps.next().type == Token.TT.CLOSE) {
                        op.l = l;
                        return op;
                    } else {
                        ps.compl(Token.TT.CLOSE.toString());
                        ps.setPos(pos);
                        return null;
                    }
                }
            } else {
                ps.compl(Token.TT.OPEN.toString());
            }
            if (op.type == Token.TT.NOT && (l = orexp(ps)) != null) {
                op.l = l;
                return op;
            } else
                ps.compl(Token.TT.NOT.toString());
            ps.setPos(pos);
            return null;
        } catch (final NoMoreTokensException ex) {
            ps.setPos(pos);
            return null;
        }
    }

    /**
     * evunexp ::= evterm | '(' evorexp ')' | 'not' evorexp
     * 
     * @param ps
     * @return
     */
    public Token evunexp(ParserState ps) {
        final int pos = ps.getPos();
        try {
            Token l = evterm(ps);
            if (l != null)
                return l;
            ps.next();
            final Token op = ps.tok();
            if (op.type == Token.TT.OPEN) {
                if ((l = evorexp(ps)) != null) {
                    if (ps.next().type == Token.TT.CLOSE) {
                        op.l = l;
                        return op;
                    } else {
                        ps.compl(Token.TT.CLOSE.toString());
                        ps.setPos(pos);
                        return null;
                    }
                }
            } else {
                ps.compl(Token.TT.OPEN.toString());
            }
            if (op.type == Token.TT.NOT && (l = evorexp(ps)) != null) {
                op.l = l;
                return op;
            } else
                ps.compl(Token.TT.NOT.toString());
            ps.setPos(pos);
            return null;
        } catch (final NoMoreTokensException ex) {
            ps.setPos(pos);
            return null;
        }
    }

    /**
     * term ::= stringcomp | timecomp
     * 
     * @param ps
     * @return
     */
    public Token term(ParserState ps) {
        Token l = stringcomp(ps);
        if (l != null)
            return l;
        if ((l = timecomp(ps)) != null)
            return l;
        return null;
    }

    /**
     * evterm ::= durterm | 'source' orexp | 'sink' orexp
     * 
     * @param ps
     * @return
     */
    public Token evterm(ParserState ps) {
        final Token l = durcomp(ps);
        if (l != null)
            return l;
        final int pos = ps.getPos();

        try {
            ps.next();
        } catch (final NoMoreTokensException ex) {
            ps.compl(Token.TT.SOURCE.toString());
            ps.compl(Token.TT.SINK.toString());
            ps.setPos(pos);
            return null;
        }

        final Token tt = ps.tok();
        Token tt1, tt2;
        if ((tt.type == Token.TT.SOURCE || tt.type == Token.TT.SINK)) {
            try {
                ps.next();
            } catch (final NoMoreTokensException ex) {
                ps.compl(Token.TT.OPEN_SQUARE.toString());
                ps.setPos(pos);
                return null;
            }
            tt1 = ps.tok();
            if (tt1.type != Token.TT.OPEN_SQUARE) {
                ps.compl(Token.TT.OPEN_SQUARE.toString());
                ps.setPos(pos);
                return null;
            }
            tt2 = orexp(ps);
            if (tt2 != null) {
                try {
                    ps.next();
                } catch (final NoMoreTokensException ex) {
                    ps.compl(Token.TT.CLOSE_SQUARE.toString());
                    ps.setPos(pos);
                    return null;
                }
                if (ps.tok().type != Token.TT.CLOSE_SQUARE) {
                    ps.compl(Token.TT.CLOSE_SQUARE.toString());
                    ps.setPos(pos);
                    return null;
                }
                tt.l = tt2;
                return tt;
            } else {
                ps.setPos(pos);
                return null;
            }
        } else {
            ps.compl(Token.TT.SOURCE.toString());
            ps.compl(Token.TT.SINK.toString());
            ps.setPos(pos);
            return null;
        }
    }

    /**
     * stringcomp::= ('label' | 'type') ('=' | 'contains' | 'startswith' |
     * 'endswith') STRINGVAL
     * 
     * @param ps
     * @return
     */
    public Token stringcomp(ParserState ps) {
        final int pos = ps.getPos();

        try {
            final Token l = ps.next();
            if (l.type == Token.TT.LABEL || l.type == Token.TT.TYPE || l.type == Token.TT.NOTE) {
                final Token op = ps.next();
                if (op.type != Token.TT.EQ && op.type != Token.TT.STARTSWITH
                        && op.type != Token.TT.ENDSWITH
                        && op.type != Token.TT.CONTAINS) {
                    ps.compl(Token.TT.EQ.toString());
                    ps.compl(Token.TT.STARTSWITH.toString());
                    ps.compl(Token.TT.ENDSWITH.toString());
                    ps.compl(Token.TT.CONTAINS.toString());
                    ps.setPos(pos);
                    return null;
                }
                final Token r = ps.next();
                if (r.type != Token.TT.STRING) {
                    ps.compl("\"\"");
                    ps.setPos(pos);
                    return null;
                }
                if (ps.tok().string.charAt(0) != '"'
                        || ps.tok().string.charAt(ps.tok().string.length() - 1) != '"') {
                    ps.setPos(pos);
                    return null;
                }
                op.l = l;
                op.r = r;
                return op;
            } else {
                ps.compl(Token.TT.LABEL.toString());
                ps.compl(Token.TT.TYPE.toString());
                ps.compl(Token.TT.NOTE.toString());
            }
            ps.setPos(pos);
            return null;
        } catch (final NoMoreTokensException ex) {
            ps.setPos(pos);
            return null;
        }
    }

/**
     * timecomp ::= 'time' ('=' | '<' | '>' | '<=' | '>=') TIME
     * @param ps
     * @return 
     */
    public Token timecomp(ParserState ps) {
        final int pos = ps.getPos();
        try {
            final Token l = ps.next();
            if (l.type == Token.TT.TIME) {
                final Token op = ps.next();
                if (op.type != Token.TT.EQ && op.type != Token.TT.LT
                        && op.type != Token.TT.GT && op.type != Token.TT.LEQ
                        && op.type != Token.TT.GEQ) {
                    ps.compl(Token.TT.EQ.toString());
                    ps.compl(Token.TT.LT.toString());
                    ps.compl(Token.TT.GT.toString());
                    ps.compl(Token.TT.LEQ.toString());
                    ps.compl(Token.TT.GEQ.toString());
                    ps.setPos(pos);
                    return null;
                }
                final Token r = ps.next();
                if (r.type != Token.TT.STRING) {
                    ps.compl("\"\"");
                    ps.setPos(pos);
                    return null;
                }
                if (r.string.charAt(0) != '"'
                        || r.string.charAt(ps.tok().string.length() - 1) != '"') {
                    ps.setPos(pos);
                    return null;
                }
                op.l = l;
                op.r = r;
                return op;
            } else
                ps.compl(Token.TT.TIME.toString());
            ps.setPos(pos);
            return null;
        } catch (final NoMoreTokensException ex) {
            ps.setPos(pos);
            return null;
        }
    }

/**
     * durcomp ::= 'duration' ('=' | '<' | '>' | '<=' | '>=') LONG ('ns' | 'us' | 'ms' | 's')
     * @param ps
     * @return 
     */
    public Token durcomp(ParserState ps) {
        final int pos = ps.getPos();
        try {
            final Token l = ps.next();
            if (l.type == Token.TT.DURATION) {
                final Token op = ps.next();
                if (op.type != Token.TT.EQ && op.type != Token.TT.LT
                        && op.type != Token.TT.GT && op.type != Token.TT.LEQ
                        && op.type != Token.TT.GEQ) {
                    ps.compl(Token.TT.EQ.toString());
                    ps.compl(Token.TT.LT.toString());
                    ps.compl(Token.TT.GT.toString());
                    ps.compl(Token.TT.LEQ.toString());
                    ps.compl(Token.TT.GEQ.toString());
                    ps.setPos(pos);
                    return null;
                }
                final Token v = ps.next();
                if (v.type != Token.TT.NUM) {
                    ps.compl("<number>");
                    ps.setPos(pos);
                    return null;
                }
                final Token r = ps.next();
                if (r.type != Token.TT.EQ && r.type != Token.TT.NS
                        && r.type != Token.TT.US && r.type != Token.TT.MS
                        && r.type != Token.TT.S) {
                    ps.compl(Token.TT.NS.toString());
                    ps.compl(Token.TT.US.toString());
                    ps.compl(Token.TT.MS.toString());
                    ps.compl(Token.TT.S.toString());
                    ps.setPos(pos);
                    return null;
                }
                op.l = l;
                op.r = r;
                r.l = v;
                return op;
            } else
                ps.compl("duration");
            ps.setPos(pos);
            return null;
        } catch (final NoMoreTokensException ex) {
            ps.setPos(pos);
            return null;
        }
    }

}
