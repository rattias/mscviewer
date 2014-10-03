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

import com.cisco.mscviewer.script.PyFunction;
import com.cisco.mscviewer.script.Python;
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
            PyFunction f;
            if (leaf && obj instanceof PyFunction) {
                f = (PyFunction)n.getUserObject();
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
        System.out.println("in initTreeContentInWorkerThread, Thread = "+Thread.currentThread().getName());
        if (py == null) {
            py = new Python();
        } else
            py.init();
        try{
            DefaultTreeModel dtm = (DefaultTreeModel) getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode )dtm.getRoot();
            root.removeAllChildren();
            dtm.reload();
            for(String pkg: py.getPackages()) {
                DefaultMutableTreeNode pkgNode = new DefaultMutableTreeNode(pkg);
                if (py.getFunctions(pkg).length > 0)
                    root.add(pkgNode);
                for(PyFunction fn: py.getFunctions(pkg)) {
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

    public PyScriptTree() {
        DefaultTreeModel dtm = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Functions");
        dtm.setRoot(root);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(this);
        setCellRenderer(new PyScriptTreeRenderer());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() ==2) {
                    TreePath tp = getPathForLocation(me.getX(), me.getY());
                    if (tp == null)
                        return;
                    DefaultMutableTreeNode tn = (DefaultMutableTreeNode )tp.getLastPathComponent();
                    Object o = tn.getUserObject();
                    if (o instanceof PyFunction) {
                        PyFunction fun = (PyFunction)o;

                        boolean invokable = fun.canBeInvoked();
                        if ((!invokable) || me.isShiftDown()) {
                            int res = new FunctionParametersDialog(fun).open();
                            if (res != FunctionParametersDialog.OK)
                                return;
                        }
                        try {
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
                System.out.println("selection changed");
                if (py.scriptsChanged()) {
                    System.out.println("scripts changed, reloading tree");
                    initTreeContent();
                }
            }
        });
    }
}
