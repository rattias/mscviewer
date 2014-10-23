package com.cisco.mscviewer.script;
import com.cisco.mscviewer.gui.MainFrame;
import com.cisco.mscviewer.gui.MainPanel;
import com.cisco.mscviewer.util.DirSetWatcher;
import com.cisco.mscviewer.util.ProgressReport;
import com.cisco.mscviewer.util.Report;
import com.cisco.mscviewer.util.Watcher;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.SwingWorker;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.python.core.*;

public class Python  {
    private  PythonInterpreter interpreter;
    private  String[] pypath;
    private final  HashMap<String, PythonFunction[]> module2funcs = new LinkedHashMap<String, PythonFunction[]>();
    private DirSetWatcher dw;
    private boolean scriptsChanged;
    
    public PythonInterpreter getInterpreter() {
        return interpreter;
    }

    private static boolean isModule(File f, String fullpath) {
        return f.isFile() && fullpath.endsWith(".py") && ! fullpath.endsWith("__init__.py"); 
    }
    private static String pathToName(String path) {
        String res = path.replace('/', '.');
        return res.endsWith(".py") ? res.substring(0, path.length()-3): res;

    }

    private void traverse(String pathEl, String fpath, int level, ArrayList<String> dirs) {
        String fullpath =pathEl + "/" + fpath;
        File f = new File(fullpath);
        if (f.isDirectory()) {
            dirs.add(f.getAbsolutePath());
            String fn = fullpath+"/__init__.py";
            if (new File(fn).exists()) {
                if (! fpath.equals("")) {
                    String pkg = pathToName(fpath);
                    module2funcs.put(pkg, new PythonFunction[0]);
                    interpreter.exec("import "+pkg);
                }
                String[] subFiles = f.list();
                for(String s: subFiles)
                    traverse(pathEl, fpath.equals("") ? s : fpath + "/" + s, level+1, dirs);
            }
        } else {
            if (isModule(f, fullpath)) {
                String pkg = pathToName(fpath);
                module2funcs.put(pkg, new PythonFunction[0]);
                interpreter.exec("import "+pkg);
                interpreter.exec("from "+pkg+" import *");
            }
        }
    }

    public Python(MainPanel mp) {
        init(mp);        
    }
    
    public boolean scriptsChanged() {
        return scriptsChanged;
    }
    
    
    public void init(MainPanel mp) {
        String path = System.getProperty("pypath");
        if (path == null) { 
            throw new Error("-Dpypath=<path> missing");
        }
        pypath = path.split(System.getProperty("path.separator"));
        int cnt = pypath.length;
        ProgressReport pr = new ProgressReport("Initializing Python", "Scanning scripts...", 0, cnt+3);
        try {
            ArrayList<String> dirs = new ArrayList<String>();
            pr.progress("Creating Python system state", 0);
            PySystemState engineSys = new PySystemState();
            for(String s: pypath) {
                engineSys.path.append(Py.newString(s));
            }
            interpreter = new PythonInterpreter(new PyDictionary(), engineSys);
            pr.progress("Importing packages", 1);

            interpreter.exec("import sys"); 
            interpreter.exec("import mscviewer");
            interpreter.set("msc_main_panel", mp);
            int v=4;
            for(String pathEl: pypath) {
                pr.progress("traversing path", v++);
                traverse(pathEl, "", 0, dirs);
            }
            if (dw != null)
                dw.destroy();
            dw = new DirSetWatcher(dirs.toArray(new String[dirs.size()]));
            dw.add(new Watcher() {
                @Override
                public void event(String parentPath, WatchEvent<?> ev) {
                    Path path = (Path)ev.context();
                    File  file = new File(parentPath, path.toString());
                    System.out.println("-->"+file.toString());
                    System.out.println("isFile: "+file.isFile());
                    if (file.isFile() && path.toString().endsWith(".py")) {
                        scriptsChanged = true;
                    }
                }
            });
            dw.start();

            for(String m: module2funcs.keySet()) {
                System.out.println("traversing module "+m);
                Object[] fnNames = ((PyList)interpreter.eval("msc_list_functions("+m+")")).toArray();
                System.out.print("    found functions: ");
                for(Object fn: fnNames)
                    System.out.print(fn+" ");
                System.out.println();
                if (fnNames.length > 0) {
                    PythonFunction[] funcs = new PythonFunction[fnNames.length];
                    for(int i=0; i<fnNames.length; i++) {
                        funcs[i] = new PythonFunction(this, m, (String)fnNames[i]);
                    }
                    module2funcs.put(m, funcs);
                }
            }
        } catch (Exception e) {
            Report.exception(e);
        }finally {
            pr.progressDone();
        }
    }

    public String[] getPackages() {
        Set<String> s = module2funcs.keySet();
        System.out.println("getPackages(): "+s.size()+" packages");
        return s.toArray(new String[s.size()]);
    }

    public PythonFunction[] getFunctions(String pkg) {
        return module2funcs.get(pkg);
    }

    public void eval(final String cmd, final ScriptResult sc) {
        SwingWorker<Object, Object> sw = new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                return interpreter.eval(cmd);
            }

            @Override
            public void done() {
                try {
                    @SuppressWarnings("unused")
                    Object res = get();
                }catch(InterruptedException ex){
                    // shouldn't happen
                } catch(ExecutionException ex) {
                    Throwable ex1 = ex.getCause();
                    Report.exception(ex1);
                } catch(CancellationException ex) {
                    System.out.println("cancelled by user");
                }
                MainFrame.getInstance().repaint();
            }
        };
        sw.execute();
    }

    /**
     * Used in batch mode
     * @param cmd
     * @return
     * @throws ScriptException
     */
    public void exec(String cmd) {
        interpreter.exec(cmd);        
    }

    public PyObject eval(String cmd) {
        return interpreter.eval(cmd);        
    }

    
    public PyObject call(String fname, PyObject... args) {
        PyObject fn = interpreter.get(fname);
        return fn.__call__(args);
    }

    
    /**
     * Used in batch mode
     * @param r
     * @return
     * @throws ScriptException
     */
//    public Object eval(Reader r) throws ScriptException {
//        r.read
//        return interpreter.eval(r);
//    }

    public void set(String name, Object value) {
        interpreter.set(name, value);
    }
    
    public PyObject get(String name) {
        return interpreter.get(name);
    }

}
