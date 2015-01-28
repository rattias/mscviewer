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

import com.cisco.mscviewer.io.JSonException;
import com.cisco.mscviewer.util.JSonParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;


/**
 * Model element representing a JSon object. 
 * 
 * A JSon object is expressed by the grammar shown in {@link http://json.org/}
 * 
 * @author rattias
 */
public class JSonObject extends Object {

    private HashMap<String, Object> map;

    public JSonObject() {
        map = new LinkedHashMap<String, Object>();
    }

    public JSonObject(String str) throws JSonException {
        this();
        JSonParser.parseObject(str, this);
    }

    public static JSonObject parse(String text, String file, int lineNum, int pos) throws JSonException {
        return JSonParser.parseObject(text, file, lineNum);
    }
    
    public static JSonObject parse(String text) throws JSonException {
        return parse(text, null, 0, 0);
    }

    private String toStringInternal(boolean pretty, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        for (String k : map.keySet()) {
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
            Object value = map.get(k);
            if (value instanceof JSonObject) {
                JSonObject v = (JSonObject) value;
                sb.append(v.toStringInternal(pretty, indent + 1));
            } else if (value instanceof String) {
                sb.append('"');
                sb.append(value);
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

    public Object get(String key) {
        return map.get(key);
    }
    
    public JSonObject getJSon(String key) {
        return (JSonObject)map.get(key);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Object> getArray(String key) {
        return (ArrayList<Object>)map.get(key);
    }

    
    public void set(String key, Object value) {
        if (value instanceof String ||
            value instanceof Boolean ||
            value instanceof Integer ||
            value instanceof ArrayList ||
            value instanceof JSonObject)
            map.put(key, value);
        else
            throw new Error("value of type "+value.getClass().getName()+" not supported");
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
        return map.equals(((JSonObject)o2).map);
    }

    
    public static void main(String args[]) throws JSonException {
        String s = "{ \"ciao\":\"bello\", \"foo\":true, \"bar\":[1, 2], \"xyz\":{\"k1\":\"v1\"} }";
        //String s = "{ \"ciao\":\"bello\"}";
        JSonObject o1 = new JSonObject(s);
        JSonObject o2 = new JSonObject(s);
        System.out.println("o1="+o1);
        System.out.println("o2="+o2);
        System.out.println("h(o1)="+o1.hashCode());
        System.out.println("h(o2)="+o2.hashCode());
        System.out.print("o1.equals(o2):"+o1.equals(o2));
    }

    public Object getValueByPath(String path) {
        String [] toks = path.split("/");
        Object o = this;
        String tmp = "";
        for(String t: toks) {
            if (! (o instanceof JSonObject))
                throw new NoSuchFieldError("Field '"+t+"' is not a JSON object in path '"+tmp+"'.");
            tmp+= t+"/";
            o = ((JSonObject)o).get(t);
            if (o == null)
                throw new NoSuchFieldError("Field '"+t+"' not found in path '"+tmp+"'.");
        }
        return o;
    }

    public String[] getKeys() {
        Set<String> s = map.keySet();
        return s.toArray(new String[s.size()]);
    }
    public Object[] getValues() {
        Collection<Object> s = map.values();
        return s.toArray(new Object[s.size()]);
    }

    public int getFieldCount() {
        return map.size();
    }
}
