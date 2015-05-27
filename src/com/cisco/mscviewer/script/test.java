package com.cisco.mscviewer.script;

import org.python.core.PyDictionary;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class test {
    public static void main(String args[]) {
        final PySystemState pss = new PySystemState();
        final PythonInterpreter pi = new PythonInterpreter(new PyDictionary(), pss);
        pi.exec("def xxx(a, b, c=2):\n    return a+b+c\n");
        final PyFunction pf = (PyFunction) pi.get("xxx");
        System.out.println("res = " + pf);
        pi.exec("import inspect");
        final PyObject po = pi.eval("inspect.getargspec(xxx)");
        System.out.println("res = " + po);
    }
}
