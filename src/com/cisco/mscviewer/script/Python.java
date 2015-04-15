package com.cisco.mscviewer.script;
import com.cisco.mscviewer.Main;
import com.cisco.mscviewer.gui.MainFrame;
import com.cisco.mscviewer.gui.MainPanel;
import com.cisco.mscviewer.script.PythonChangeListener;
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
    private final HashMap<String, PythonFunction[]> module2funcs = new LinkedHashMap<String, PythonFunction[]>();
    private final HashMap<String, String> module2PyFile = new HashMap<String, String>();
    private final ArrayList<PythonChangeListener> listeners = new ArrayList<PythonChangeListener>(); 
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
                module2PyFile.put(pkg,fullpath);
                module2funcs.put(pkg, new PythonFunction[0]);
                interpreter.exec("import "+pkg);
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
        module2funcs.clear();
        module2PyFile.clear();
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
            interpreter.exec("import inspect"); 
            //interpreter.exec("import mscviewer");
            interpreter.exec(
            "def list_msc_functions(pkg):\n"+
            "    ret = []\n"+
            "    for fn_name in dir(pkg):\n"+
            "        fn = getattr(pkg, fn_name)\n"+
            "        if hasattr(fn, \"is_msc_fun\"):\n"+
            "            ret.append(fn_name)\n"+ 
            "    return ret;");
            interpreter.set("msc_main_panel", mp);
            for(String pathEl: pypath) {
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
                    if ((!file.isDirectory()) && path.toString().endsWith(".py")) {
                        System.out.println("scripts Changed");
                        scriptsChanged = true;
                        for(PythonChangeListener l: listeners)
                            l.moduleChanged(file.getPath());
                    }
                }
            });
            dw.start();

            for(String m: module2funcs.keySet()) {
                Object[] fnNames = ((PyList)interpreter.eval("list_msc_functions("+m+")")).toArray();
                if (fnNames.length > 0) {
                    PythonFunction[] funcs = new PythonFunction[fnNames.length];
                    for(int i=0; i<fnNames.length; i++) {
                        funcs[i] = new PythonFunction(this, m, (String)fnNames[i]);
                    }
                    module2funcs.put(m, funcs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Report.exception(e);
        }finally {
            pr.progressDone();
        }
    }

    public String[] getPackages() {
        Set<String> s = module2funcs.keySet();
        return s.toArray(new String[s.size()]);
    }

    public PythonFunction[] getFunctions(String pkg) {
        return module2funcs.get(pkg);
    }

    public String getPyPathForFunction(PythonFunction f) {
        for(String m: module2funcs.keySet()) {
            PythonFunction[] funcs = getFunctions(m);
            for(PythonFunction f1: funcs) {
                if (f1 == f)
                    return module2PyFile.get(m);
            }
        }
        return null;
    }
    
    public void eval(final String cmd, final ScriptResult sc) {
        if (Main.batchMode)
            sc.setResult(interpreter.eval(cmd));
        SwingWorker<Object, Object> sw = new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                return interpreter.eval(cmd);
            }

            @Override
            public void done() {
                try {
                    Object res = get();
                    sc.setResult(res);
                }catch(InterruptedException ex){
                    // shouldn't happen
                } catch(ExecutionException ex) {
                    Throwable ex1 = ex.getCause();
                    System.err.println("exception while evaluating expression "+cmd);
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

    public void addChangeListener(PythonChangeListener l) {
        listeners.add(l);        
    }

}
