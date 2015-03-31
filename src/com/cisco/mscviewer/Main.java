/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *------------------------------------------------------------------*/
package com.cisco.mscviewer;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.script.ScriptException;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.cisco.mscviewer.gui.MainFrame;
import com.cisco.mscviewer.gui.MainPanel;
import com.cisco.mscviewer.io.LegacyLoader;
import com.cisco.mscviewer.io.Loader;
import com.cisco.mscviewer.model.Entity;
import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.model.Interaction;
import com.cisco.mscviewer.model.MSCDataModel;
import com.cisco.mscviewer.model.ViewModel;
import com.cisco.mscviewer.script.Python;
import com.cisco.mscviewer.script.ScriptResult;
import com.cisco.mscviewer.util.Resources;
import com.cisco.mscviewer.util.Utils;

abstract class Opt {
    char shortName;
    String longName;
    String descr;
    boolean hasArg;

    public Opt(char sh, String ln, boolean arg, String ds) {
        shortName = sh;
        longName = ln;
        hasArg = arg;
        descr = ds;
    }
    abstract void found(String arg);
}

/**
 * class containing the main() method.
 * 
 * @author Roberto Attias
 * @since   Jun 2012 
 */
public class Main {
    public static final String VERSION = "2.0B1";
    public static final boolean WITH_BLOCKS = true;
    private static Loader loader;
    public static String[] pypath;
    public static MainFrame mf;
    public static boolean extra;
    public static String script;
    public static String loaderClass = "JsonLoader";
    //    public static String loaderClass = "LegacyLoader";
    public static boolean batchMode = false;
    public static String batchFun = null;
    private static MSCDataModel dataModel;
    public static ProgressMonitor pm;

    private static final void appendToPyPath(String s) {
        String v = System.getProperty("pypath");
        if (v != null && v.length() > 0)
            v += File.pathSeparator + s;
        else
            v = s;
        System.setProperty("pypath", v);
    }

    private static final Opt[] opts = new Opt[] {
        new Opt('h', "help", true, "shows this help") {
            @Override
            void found(String arg) {
                printHelp();
                System.exit(0);
            }
        },
        new Opt('b', "batch", true, "executes the passed python script in batch mode") {
            @Override
            void found(String arg) {
                Main.batchMode = true;
                int idx = arg.indexOf(',');
                if (idx == -1)
                    Main.script = arg;
                else {
                    Main.script = arg.substring(0, idx);
                    batchFun = arg.substring(idx+1);
                }
            }
        },
        new Opt('p', "pypath", true, "specify a Python module search path") {
            @Override
            void found(String arg) {
                appendToPyPath(arg);
            }
        },
        new Opt('x', "extra", false, "enable some extra features") {
            @Override
            void found(String arg) {
                Main.extra = true;
            }
        },
        new Opt('s', "script", true, "opens the GUI and executes the passed python script") {
            @Override
            void found(String arg) {
                Main.script = arg;
            }
        },
        new Opt('l', "loader", true, "specify loader to use for input file") {
            @Override
            void found(String arg) {
                Main.loaderClass = arg;
            }
        },
        new Opt('r', "resource", true, "specify a path for domain-specific resources") {
            @Override
            void found(String arg) {
                Main.plugins = arg;
                for (String s : arg.split(File.pathSeparator)) {
                    String dir = s+"/script";
                    if (new File(dir).isDirectory())
                        appendToPyPath(dir);
                }
            }
        }
    };
    public static String plugins;

    private static void printHelp() {
        System.out.println("mscviewer options [file]");
        System.out.println("  starts mscviewer");
        for (Opt opt: opts) {
            System.out.println("-"+opt.shortName+"\t--"+opt.longName+(opt.hasArg ? " arg\t" : "\t")+opt.descr);
        }

    }


    public static void main(String args[]) throws IOException,
    ClassNotFoundException, SecurityException, NoSuchMethodException,
    IllegalArgumentException, IllegalAccessException,
    InstantiationException, ScriptException, InterruptedException, InvocationTargetException {
        System.setProperty("pypath", Main.getInstallDir()+"/resources/default/script");
        
        setupUIDefaults();
        int idx = processOptions(args);

        final String fname = (idx<args.length) ? args[idx]: null;
        Class<?> cl = Class.forName("com.cisco.mscviewer.io."+loaderClass);

        Resources.init(Main.plugins);

        loader = (Loader)cl.newInstance();
        dataModel = new MSCDataModel();
        if (batchMode()) {
            if (fname == null) {
                System.err.println("Missing input file");
                System.exit(1);
            }
            loader.load(fname, dataModel);
        } else {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // If Nimbus is not available, you can set the GUI to another look and feel.
            }
            SwingUtilities.invokeAndWait(new Runnable(){
                @Override
                public void run() {
                    mf = new MainFrame(10, 10, 1024, 600);
                    mf.setVisible(true);
                }
            });
            if (fname != null) {
                loader.load(fname, dataModel);
            }
        }
        if (script != null) {
            loader.waitIfLoading();
            MainPanel mp = (mf != null) ? mf.getMainPanel() : null;
            Python p = new Python(mp);
            ScriptResult sr = new ScriptResult();
            String text = new String(Files.readAllBytes(Paths.get(script)), StandardCharsets.UTF_8);
            p.exec(text);
            if (batchFun != null) {
                p.eval(batchFun, sr);
            }
        }
    }

    private static int processOptions(String[] args) {
        int idx = 0;
        int len = args.length;
        while(idx < len && args[idx].startsWith("-")) {
            char sa = '\0';
            String la = null;
            if (args[idx].length() == 2) {
                sa =  args[idx].charAt(1);
                la = null;
            } else if (args[idx].charAt(1) == '-') {
                sa =  0;
                la = args[idx].substring(2);
            } else {
                System.err.println("Invalid option "+args[idx]);
                System.exit(1);
            }
            int i;
            for(i=0; i<opts.length; i++) {
                if (opts[i].shortName == sa || opts[i].longName.equals(la)) {
                    String arg;
                    if (opts[i].hasArg && len > idx+1) {
                        arg = args[idx+1];
                        idx += 2;
                    } else {
                        arg = null;
                        idx += 1;
                    }
                    opts[i].found(arg);
                    break;
                }
            }
            if (i == opts.length) {
                System.err.println("Invalid option "+args[idx]);
                System.exit(1);
            }
        }
        return idx;
    }


    private static void setupUIDefaults() {
        Font f = (Font)UIManager.getDefaults().get("Tree.font");
        UIManager.put("Button.font", f);
        UIManager.put("ToggleButton.font", f);
        UIManager.put("RadioButton.font", f);
        UIManager.put("CheckBox.font", f);
        UIManager.put("ColorChooser.font", f);
        UIManager.put("ComboBox.font", f);
        UIManager.put("Label.font", f);
        UIManager.put("List.font", f);
        UIManager.put("MenuBar.font", f);
        UIManager.put("MenuItem.font", f);
        UIManager.put("RadioButtonMenuItem.font", f);
        UIManager.put("CheckBoxMenuItem.font", f);
        UIManager.put("Menu.font", f);
        UIManager.put("PopupMenu.font", f);
        UIManager.put("OptionPane.font", f);
        UIManager.put("Panel.font", f);
        UIManager.put("ProgressBar.font", f);
        UIManager.put("ScrollPane.font", f);
        UIManager.put("Viewport.font", f);
        UIManager.put("TabbedPane.font", f);
        UIManager.put("Table.font", f);
        UIManager.put("TableHeader.font", f);
        UIManager.put("TextField.font", f);
        UIManager.put("PasswordField.font", f);
        UIManager.put("TextArea.font", f);
        UIManager.put("TextPane.font", f);
        UIManager.put("EditorPane.font", f);
        UIManager.put("TitledBorder.font", f);
        UIManager.put("ToolBar.font", f);
        UIManager.put("ToolTip.font", f);

//        ImageIcon icon = Resources.getImageIcon("entity.gif", "Entity");
//        if (icon != null) {
//            UIManager.put("Tree.leafIcon", icon);
//            UIManager.put("Tree.openIcon", icon);
//            UIManager.put("Tree.closedIcon", icon);
//        } else {
//            throw new Error("Couldn't find file entity.gif");
//        }
    }


    public static boolean batchMode() {
        return batchMode;
    }

    public static MainFrame getMainFrame() {
        return mf;
    }

    public static MSCDataModel getModel() {
        return dataModel;
    }

    public static Event getSelectedEvent() {
        return mf.getMainPanel().getMSCRenderer().getSelectedEvent();
    }

    public static Interaction getSelectedInteraction() {
        return mf.getMainPanel().getMSCRenderer().getSelectedInteraction();
    }

    public static void show(final Entity en) {
        Utils.dispatchOnAWTThreadLater(new Runnable() {
            @Override
            public void run() {
                mf.getViewModel().add(en);
            }
        });
    }

    public static void show(final String id) {
        Entity en = mf.getDataModel().getEntity(id);
        show(en);
    }

    public static void show(final Entity[] en) {
        Utils.dispatchOnAWTThreadLater(new Runnable() {
            @Override
            public void run() {
                mf.getViewModel().add(en);
            }
        });
    }

    public static void show(final Event ev) {
        Utils.dispatchOnAWTThreadLater(new Runnable() {
            @Override
            public void run() {
                ViewModel vm = mf.getViewModel();
                vm.add(ev.getEntity());
                int idx = vm.indexOf(ev);
                mf.getMainPanel().makeEventWithIndexVisible(idx);
                mf.getMainPanel().getMSCRenderer().setSelectedEventByViewIndex(idx);
            }
        });
    }

    public static void hide(Entity en) {
        mf.getEntityHeader().remove(en);
    }

    public static void addResult(final String res) {
        Utils.dispatchOnAWTThreadLater(new Runnable() {
            @Override
            public void run() {
                MainFrame.getInstance().addResult(res);
            }
        });
    }

    public static Loader getLoader() {
        return loader;
    }

    public static MSCDataModel batchLoad(String path) throws IOException {
        MSCDataModel dm = new MSCDataModel();
        Loader l = new LegacyLoader();
        l.load(path, dm);
        return dm;
    }

    public static void start(String[] args) throws IOException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, ScriptException, InterruptedException, InvocationTargetException {
        main(args);
    }

    public static void clearModel() {
        MSCDataModel dm  = mf.getDataModel();
        dm.reset();
    }

    public static void maximize() {
        mf.setExtendedState(mf.getExtendedState() | JFrame.MAXIMIZED_BOTH);
    }


    public static MSCDataModel getDataModel() {
        return dataModel;
    }

    public static String getInstallDir() {
        URL resourceURL = ClassLoader.getSystemResource("com/cisco/mscviewer");
        String urlStr = resourceURL.getPath(); 
        int idx = urlStr.indexOf("mscviewer.jar!");
        if (idx < 0) {
            return urlStr.substring(1,  urlStr.indexOf("classes"));
        } else {
            return urlStr.substring("file:/".length(),  idx);
        }
    }        


}
