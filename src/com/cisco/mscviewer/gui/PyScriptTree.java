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

import com.cisco.mscviewer.script.Python;
import com.cisco.mscviewer.script.PythonFunction;
import com.cisco.mscviewer.util.Report;
import com.cisco.mscviewer.util.Utils;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.script.ScriptException;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

@SuppressWarnings("serial")
class PyScriptTree extends JTree {
    final static String dir = "scripts";
    private Python py = null;
    private boolean updating;
    private MainPanel mainPanel;
    
    class PyScriptTreeRenderer extends DefaultTreeCellRenderer {        
        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(
                    tree, value, sel,
                    expanded, leaf, row,
                    hasFocus);
            setToolTipText(null);
            DefaultMutableTreeNode n = (DefaultMutableTreeNode)value;
            Object obj = n.getUserObject();
            PythonFunction f;
            if (leaf && obj instanceof PythonFunction) {
                f = (PythonFunction)n.getUserObject();
                String doc = f.getDoc();
                if (doc != null) {
                    doc = "<html>"+Utils.stringToHTML(doc)+"</html>";
                    setToolTipText(doc);
                }
            }
            return c;
        }
    }

    public void initTreeContent() {
//        if (! SwingUtilities.isEventDispatchThread())
//            throw new Error("Should be called in event dispatch thread, called by "+Thread.currentThread());
        if (updating)
            return;
        updating = true;
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                initTreeContentInWorkerThread();
                return null;
            }
            
            @Override
            public void done() {
                updating = false;
            }
        };
        worker.execute();
    }
    
    private void initTreeContentInWorkerThread() {        
        DefaultTreeModel dtm = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode )dtm.getRoot();
        root.removeAllChildren();
        dtm.reload();
        if (py == null) {
            System.out.println("Instantiating python");
            py = new Python(mainPanel);
        } else
            py.init(mainPanel);
        try{
            for(String pkg: py.getPackages()) {
                DefaultMutableTreeNode pkgNode = new DefaultMutableTreeNode(pkg);
                if (py.getFunctions(pkg).length > 0)
                    root.add(pkgNode);
                else {
                    //System.out.println("no functions in package "+pkg);
                }
                for(PythonFunction fn: py.getFunctions(pkg)) {
                    DefaultMutableTreeNode fnNode = new DefaultMutableTreeNode(fn);
                    pkgNode.add(fnNode);
                }
            }
            dtm.reload();
            //dtm.nodeStructureChanged(root);
        }catch(Throwable ex) {
            Report.exception(ex);
        }
    }

    public PyScriptTree(MainPanel mp) {
        this.mainPanel = mp;
        DefaultTreeModel dtm = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Functions");
        dtm.setRoot(root);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(this);
        setCellRenderer(new PyScriptTreeRenderer());
        System.out.println("!!!");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                System.out.println("mouse clicked");
                if (me.getClickCount() ==2) {
                    System.out.println("mouse double clicked");
                    TreePath tp = getPathForLocation(me.getX(), me.getY());
                    if (tp == null)
                        return;
                    DefaultMutableTreeNode tn = (DefaultMutableTreeNode )tp.getLastPathComponent();
                    Object o = tn.getUserObject();
                    if (o instanceof PythonFunction) {
                        PythonFunction fun = (PythonFunction)o;
                        boolean invokable = fun.canBeInvoked();
                        System.out.println("fun is "+fun+", invokable ="+invokable);
                        if ((!invokable) || me.isShiftDown()) {
                            int res = new FunctionParametersDialog(fun).open();
                            if (res != FunctionParametersDialog.OK)
                                return;
                        }
                        try {
                            System.out.println("invoking");
                            fun.invoke();
                        } catch (ScriptException e) {
                            Report.exception(e);
                            e.printStackTrace();
                        }
                    }                        
                }
            }

        });
        addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (py != null && py.scriptsChanged()) {
                    System.out.println("scripts changed, reloading tree");
                    initTreeContent();
                }
            }
        });
    }
}
