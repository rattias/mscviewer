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
import com.cisco.mscviewer.script.PythonChangeListener;
import com.cisco.mscviewer.script.PythonFunction;
import com.cisco.mscviewer.util.Report;
import com.cisco.mscviewer.util.Utils;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingWorker;
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
            //System.out.println("Instantiating python");
            py = new Python(mainPanel);
            py.addChangeListener(new PythonChangeListener() {
                @Override
                public void moduleAdded(String module) {
                    initTreeContent();                    
                }

                @Override
                public void moduleRemoved(String module) {
                    initTreeContent();                    
                }

                @Override
                public void moduleChanged(String module) {
                    initTreeContent();                    
                }
                
            });
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
        }catch(Throwable ex) {
            ex.printStackTrace();
            Report.exception(ex);
        }
    }

    private void handlePopupMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            int x = e.getX();
            int y = e.getY();
            final TreePath path = getPathForLocation(x, y);
            if (path != null) {
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode)path.getLastPathComponent();
                if (tn.getUserObject() instanceof PythonFunction) {
                    final PythonFunction fun = (PythonFunction)tn.getUserObject();

                    JPopupMenu popup = new JPopupMenu();
                    popup.add(new AbstractAction("Open in editor") { 
                        public void actionPerformed(ActionEvent e) {
                            Desktop dt = Desktop.getDesktop();
                            try {
                                dt.open(new File(py.getPyPathForFunction(fun)));
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    popup.show(PyScriptTree.this, x, y);
                }
            }
        }
    }
    
    public PyScriptTree(MainPanel mp) {
        this.mainPanel = mp;
        DefaultTreeModel dtm = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Functions");
        dtm.setRoot(root);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(this);
        setCellRenderer(new PyScriptTreeRenderer());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePopupMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handlePopupMenu(e);
            }
            
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() ==2) {
                    TreePath tp = getPathForLocation(me.getX(), me.getY());
                    if (tp == null)
                        return;
                    DefaultMutableTreeNode tn = (DefaultMutableTreeNode )tp.getLastPathComponent();
                    Object o = tn.getUserObject();
                    if (o instanceof PythonFunction) {
                        PythonFunction fun = (PythonFunction)o;
                        boolean invokable = fun.canBeInvoked();
                        if ((!invokable) || me.isShiftDown()) {
                            int res = new FunctionParametersDialog(fun).open();
                            if (res != FunctionParametersDialog.OK)
                                return;
                        }
                        try {
                            fun.invoke();
                        } catch (ScriptException e) {
                            e.printStackTrace();
                            Report.exception(e);
                        }
                    }                        
                }
            }

        });
    }
}
