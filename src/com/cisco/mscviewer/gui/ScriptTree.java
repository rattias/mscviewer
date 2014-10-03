/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Aug 2012
 */
package com.cisco.mscviewer.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.cisco.mscviewer.model.MSCDataModel;

class Script {
    private static final ScriptEngineManager factory = new ScriptEngineManager();
    private static ScriptEngine jsEngine, pyEngine;
    private final File f;

    public Script(File f) {
        this.f = f;
        if (f.getName().endsWith(".js")) {
            if (jsEngine == null)
                jsEngine = factory.getEngineByName("JavaScript");
        } else if (f.getName().endsWith(".py")) {
            if (pyEngine == null)
                pyEngine = factory.getEngineByName("Python");
        } 
    }

    public Object eval(MSCDataModel dm) throws FileNotFoundException, ScriptException, NoSuchMethodException {		
        Invocable inv = null;
        if (f.getName().endsWith(".js")) {
            jsEngine.eval(new FileReader(f));
            inv = (Invocable) jsEngine;
        } else if (f.getName().endsWith(".py")) {
            pyEngine.eval(new FileReader(f));
            inv = (Invocable) jsEngine;
        } else
            throw new ScriptException("File "+f.getName()+" is neither js nor py");
        // invoke the global function named "hello"
        Object res = inv.invokeFunction("isValid", dm);
        return res;
    }

    @Override
    public String toString() {
        return f.getName();
    }
}

@SuppressWarnings("serial")
class ScriptTree extends JTree {
    final static String dir = "scripts";
    ArrayList<Script> sinfo = new ArrayList<Script>();
    private MSCDataModel dm;

    public ScriptTree() {
        DefaultTreeModel dtm = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Scripts");
        dtm.setRoot(root);

        File d = new File(dir);
        if (! d.exists()) {
            System.err.println("script directory "+d+" does not exist");
            return;
        }
        if (! d.isDirectory()) {
            System.err.println("script directory "+d+" exists, but it's not a directory");
            return;
        }
        for (File f:d.listFiles()) {
            if (f.getName().endsWith(".js") || f.getName().endsWith(".py")) {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(new Script(f));
                root.add(child);
                dtm.nodeStructureChanged(root);
            }

        }
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() ==2 && dm != null) {
                    TreePath tp = getPathForLocation(me.getX(), me.getY());
                    DefaultMutableTreeNode tn = (DefaultMutableTreeNode )tp.getLastPathComponent();
                    Script s = (Script) tn.getUserObject();
                    try {
                        String res = (String)s.eval(dm);
                        if (res != null) {
                            JOptionPane.showMessageDialog(null, "Error: "+res);							
                        } else
                            JOptionPane.showMessageDialog(null, "Success");							
                    } catch (FileNotFoundException e) {
                        JOptionPane.showMessageDialog(null, "Script file not found");
                    } catch (ScriptException e) {
                        JOptionPane.showMessageDialog(null, "Script Exception: "+e.getMessage());
                    } catch (NoSuchMethodException e) {
                        JOptionPane.showMessageDialog(null, "No such Method Exception: "+e.getMessage());
                    }
                }
            }
        });

    }

    public void setModel(MSCDataModel dm) {
        this.dm = dm;
    }
}
