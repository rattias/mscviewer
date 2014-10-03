package com.cisco.mscviewer.script;
import com.cisco.mscviewer.gui.MainFrame;
import com.cisco.mscviewer.util.DirSetWatcher;
import com.cisco.mscviewer.util.ProgressReport;
import com.cisco.mscviewer.util.Report;
import com.cisco.mscviewer.util.Watcher;

import java.io.File;
import java.io.Reader;
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
import org.python.core.PyList;
import org.python.core.PySystemState;

public class Python  {
    private  ScriptEngine engine;
    private  String[] pypath;
    private final  HashMap<String, PyFunction[]> pkg2funcs = new LinkedHashMap<String, PyFunction[]>();
    private DirSetWatcher dw;
    private boolean scriptsChanged;
    
    public ScriptEngine getEngine() {
        return engine;
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
                String pkg = pathToName(fpath);
                pkg2funcs.put(pkg, new PyFunction[0]);
                try {
                    engine.eval("import "+pkg);
                } catch (ScriptException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            String[] subFiles = f.list();
            for(String s: subFiles)
                traverse(pathEl, fpath.equals("") ? s : fpath + "/" + s, level+1, dirs);
        } else {
            if (isModule(f, fullpath)) {
                String pkg = pathToName(fpath);
                pkg2funcs.put(pkg, new PyFunction[0]);
                try {
                    engine.eval("from "+pkg+" import *");
                }catch(Throwable t) {
                    Report.exception(t);
                }
            }
        }
    }

    public Python() {
        init();
        
    }
    
    public boolean scriptsChanged() {
        return scriptsChanged;
    }
    
    
    public void init() {
        System.out.println("python init1");
        String path = System.getProperty("pypath");
        if (path == null) 
            throw new Error("-Dpypath=<path> missing");
        pypath = path.split(System.getProperty("path.separator"));
        int cnt = pypath.length;
        System.out.println("A");
        ProgressReport pr = new ProgressReport("Initializing Python", "Scanning scripts...", 0, cnt+3);
        ArrayList<String> dirs = new ArrayList<String>();
        try {
            System.out.println("B");
            pr.progress("Creating Python system state", 0);
            PySystemState engineSys = new PySystemState();
            for(String s: pypath) {
                engineSys.path.append(Py.newString(s));
            }
            //pr.progress("Setting Interpreter system state", 0);
            Py.setSystemState(engineSys);
            engine = new ScriptEngineManager().getEngineByName("python");
            pr.progress("Importing packages", 1);
            try {
                engine.eval("import sys"); 
                pr.progress(2);   
                engine.eval("print sys.path");
                pr.progress(3);
                engine.eval("import mscviewer");
            } catch (ScriptException e) {
                Report.exception(e);
            }
            int v=4;
            for(String pathEl: pypath) {
                pr.progress("traversing path", v++);
                traverse(pathEl, "", 0, dirs);
            }
        }finally {
            pr.progressDone();
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
                    System.out.println("IS PY!");
                    scriptsChanged = true;
                }
            }
        });
        dw.start();
                        
        for(String m: pkg2funcs.keySet()) {
            try {
                Object[] fnNames = ((PyList)engine.eval("msc_list_functions("+m+")")).toArray();
                if (fnNames.length > 0) {
                    PyFunction[] funcs = new PyFunction[fnNames.length];
                    for(int i=0; i<fnNames.length; i++)
                        funcs[i] = new PyFunction(this, m, (String)fnNames[i]);
                    pkg2funcs.put(m, funcs);
                }
            } catch (ScriptException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public String[] getPackages() {
        Set<String> s = pkg2funcs.keySet();
        return s.toArray(new String[s.size()]);
    }

    public PyFunction[] getFunctions(String pkg) {
        return pkg2funcs.get(pkg);
    }

    public void eval(final String cmd, final ScriptResult sc) {
        SwingWorker<Object, Object> sw = new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                return engine.eval(cmd);
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
    public Object eval(final String cmd) throws ScriptException {
        return engine.eval(cmd);
    }

    /**
     * Used in batch mode
     * @param r
     * @return
     * @throws ScriptException
     */
    public Object eval(Reader r) throws ScriptException {
        return engine.eval(r);
    }

}
