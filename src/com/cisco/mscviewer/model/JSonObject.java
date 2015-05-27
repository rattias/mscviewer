/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Aug 2014
 */
package com.cisco.mscviewer.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import com.cisco.mscviewer.io.JSonException;
import com.cisco.mscviewer.util.JSonParser;

/**
 * Model element representing a JSon object.
 * 
 * A JSon object is expressed by the grammar shown in {@link http://json.org/}
 * 
 * @author rattias
 */
public class JSonObject implements JSonValue {

    private final HashMap<String, JSonValue> map;

    public JSonObject() {
        map = new LinkedHashMap<String, JSonValue>();
    }

    public JSonObject(String str) throws JSonException {
        this();
        JSonParser.parseObject(str, this);
    }

    public static JSonObject parse(String text, String file, int lineNum,
            int pos) throws JSonException {
        return JSonParser.parseObject(text, file, lineNum);
    }

    public static JSonObject parse(String text) throws JSonException {
        return parse(text, null, 0, 0);
    }

    private String toStringInternal(boolean pretty, int indent) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        for (final String k : map.keySet()) {
            if (pretty) {
                sb.append('\n');
                for (int c = 0; c < indent * 2; c++) {
                    sb.append(' ');
                }
            }
            sb.append('"');
            sb.append(k);
            sb.append('"');
            sb.append(':');
            final JSonValue value = map.get(k);
            if (value instanceof JSonObject) {
                final JSonObject v = (JSonObject) value;
                sb.append(v.toStringInternal(pretty, indent + 1));
            } else if (value instanceof JSonStringValue) {
                sb.append('"');
                sb.append(value.toString());
                sb.append('"');
            } else {
                sb.append(value.toString());
            }
            sb.append(',');
            if (pretty) {
                sb.append('\n');
            } else {
                sb.append(' ');
            }
        }
        if (map.size() > 0) {
            // remove extra comma
            sb.deleteCharAt(sb.length() - (pretty ? 3 : 2));
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String toString() {
        return toStringInternal(false, 0);
    }

    public String toPrettyString() {
        return toStringInternal(true, 0);
    }

    public JSonValue get(String key) {
        return map.get(key);
    }

    public JSonObject getJSon(String key) {
        return (JSonObject) map.get(key);
    }

    public JSonArrayValue getArray(String key) {
        return (JSonArrayValue) map.get(key);
    }

    public void set(String key, JSonValue value) {
        map.put(key, value);
    }

    public void remove(String key) {
        map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object o2) {
        if (!(o2 instanceof JSonObject))
            return false;
        return map.equals(((JSonObject) o2).map);
    }

    public static void main(String args[]) throws JSonException {
        final String s = "{ \"ciao\":\"bello\", \"foo\":true, \"bar\":[1, 2], \"xyz\":{\"k1\":\"v1\"} }";
        // String s = "{ \"ciao\":\"bello\"}";
        final JSonObject o1 = new JSonObject(s);
        final JSonObject o2 = new JSonObject(s);
        System.out.println("o1=" + o1);
        System.out.println("o2=" + o2);
        System.out.println("h(o1)=" + o1.hashCode());
        System.out.println("h(o2)=" + o2.hashCode());
        System.out.print("o1.equals(o2):" + o1.equals(o2));
    }

    public JSonValue getValueByPath(String path) {
        final String[] toks = path.split("/");
        JSonValue o = this;
        String tmp = "";
        for (final String t : toks) {
            if (!(o instanceof JSonObject))
                throw new NoSuchFieldError("Field '" + t
                        + "' is not a JSON object in path '" + tmp + "'.");
            tmp += t + "/";
            o = ((JSonObject) o).get(t);
            if (o == null)
                throw new NoSuchFieldError("Field '" + t
                        + "' not found in path '" + tmp + "'.");
        }
        return o;
    }

    public String[] getKeys() {
        final Set<String> s = map.keySet();
        return s.toArray(new String[s.size()]);
    }

    public JSonValue[] getValues() {
        final Collection<JSonValue> s = map.values();
        return s.toArray(new JSonValue[s.size()]);
    }

    public int getFieldCount() {
        return map.size();
    }
}
