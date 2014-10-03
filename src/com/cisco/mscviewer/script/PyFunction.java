/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Nov 2013
 */
package com.cisco.mscviewer.script;

import javax.script.ScriptException;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyTuple;

public class PyFunction {
    private final String name;
    private String[] argNames;
    private String[] argDefaults;
    private String[] argValues;
    private String doc;
    private boolean wasSet;
    private Python python;

    public PyFunction(Python p, String pkg, String name) {
        this.name = name;
        python = p;
        try {
            // note: by passing the unquoted name, the actual reference is
            // passed. The object is actually an instance of @msc_fun, due to
            // decoration,
            // so we extract the actual function f
            Object args = p.getEngine().eval("inspect.getargspec(" + name + ".f)");
            PyTuple t = (PyTuple) args;
            PyObject[] objs = ((PyList) t.get(0)).getArray();
            argNames = new String[objs.length];
            for (int i = 0; i < argNames.length; i++) {
                argNames[i] = objs[i].toString();
            }
            argValues = new String[objs.length];
            Object l = t.get(3);
            if (l != null) {
                objs = ((PyTuple) l).getArray();
                argDefaults = new String[objs.length];
            } else {
                argDefaults = new String[0];
            }

            for (int i = 0; i < argDefaults.length; i++) {
                argDefaults[i] = objs[i].toString();
                int idx = argNames.length - argDefaults.length + i;
                argValues[idx] = argDefaults[i];
            }
            doc = (String) p.getEngine().eval(name + ".__doc__");
        } catch (ScriptException e) {
            throw new Error(e);
        }

    }

    public String getName() {
        return name;
    }

//    public void setDoc(String d) {
//        doc = d;
//    }
//
    public String getDoc() {
        return doc;
    }

    public String[] getArgNames() {
        return argNames;
    }

    public String[] getArgDefaults() {
        return argDefaults;
    }

    public String[] getArgValues() {
        return argValues;
    }

    public void setArgValue(String name, String value) {
        wasSet = true;
        for (int i = 0; i < argNames.length; i++)
            if (argNames[i].equals(name)) {
                argValues[i] = value;
                return;
            }
        throw new Error("Invalid arg name " + name);
    }

    public String getInvocation() {
        StringBuilder sb = new StringBuilder();
        int regArgCount = argValues.length - argDefaults.length;
        sb.append(name).append("(");
        for (int i = 0; i < regArgCount; i++)
            sb.append(argValues[i]);
        for (int i = 0; i < argDefaults.length; i++) {
            if (argValues[regArgCount + i] == null)
                sb.append(argNames[regArgCount + i]).append("=").append(argValues[regArgCount + i]);
            if (i < argNames.length - 1)
                sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    public boolean canBeInvoked() {
        int regArgCount = argValues.length - argDefaults.length;
        for (int i = 0; i < regArgCount; i++) {
            if (argValues[i] == null)
                return false;
        }
        return true;
    }

    public void invoke() throws ScriptException {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        StringBuffer args = new StringBuffer();
        int regArgCount = argValues.length - argDefaults.length;
        for (int i = 0; i < regArgCount; i++) {
            if (args.length() != 0)
                args.append(", ");
            if (argValues[i]!= null)
                args.append(argValues[i]);
            else
                args.append("None");
        }
        for (int i = 0; i < argDefaults.length; i++) {
            String v = argValues[regArgCount + i]; 
            if (! v.equals(argDefaults[i])) {
                if (args.length() != 0)
                    args.append(", ");
                args.append(argNames[regArgCount + i]).append("=").append((v != null)? v : "None");
            }
        }
        sb.append(args);
        sb.append(")");
        String f = sb.toString(); 
        ScriptResult res = new ScriptResult();
        python.eval(f, res);
    }

    @Override
    public String toString() {
        return name+"("+(argNames.length > 0 ? "...)" : ")");
    }

    public boolean wasSet() {
        return wasSet;
    }
}
