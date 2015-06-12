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

import com.cisco.mscviewer.script.Python;
import com.cisco.mscviewer.script.PythonChangeListener;
import com.cisco.mscviewer.script.PythonFunction;
import com.cisco.mscviewer.util.Report;
import com.cisco.mscviewer.util.Utils;

@SuppressWarnings("serial")
class PyScriptTree extends JTree {
    final static String dir = "scripts";
    private Python py = null;
    private boolean updating;
    private MainPanel mainPanel;
    private PythonFunction latestFunction;

    class PyScriptTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            final Component c = super.getTreeCellRendererComponent(tree, value, sel,
                    expanded, leaf, row, hasFocus);
            setToolTipText(null);
            final DefaultMutableTreeNode n = (DefaultMutableTreeNode) value;
            final Object obj = n.getUserObject();
            PythonFunction f;
            if (leaf && obj instanceof PythonFunction) {
                f = (PythonFunction) n.getUserObject();
                String doc = f.getDoc();
                if (doc != null) {
                    doc = "<html>" + Utils.stringToHTML(doc) + "</html>";
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
        final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
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
        final DefaultTreeModel dtm = (DefaultTreeModel) getModel();
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) dtm.getRoot();
        root.removeAllChildren();
        Utils.dispatchOnAWTThreadLater(() -> dtm.reload());
        if (py == null) {
            // System.out.println("Instantiating python");
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
        for (final String pkg : py.getPackages()) {
            final DefaultMutableTreeNode pkgNode = new DefaultMutableTreeNode(pkg);
            if (py.getFunctions(pkg).length > 0)
                root.add(pkgNode);
            else {
                // System.out.println("no functions in package "+pkg);
            }
            for (final PythonFunction fn : py.getFunctions(pkg)) {
                final DefaultMutableTreeNode fnNode = new DefaultMutableTreeNode(
                        fn);
                pkgNode.add(fnNode);
            }
        }
        Utils.dispatchOnAWTThreadLater(() -> dtm.reload());
    }

    private void handlePopupMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            final int x = e.getX();
            final int y = e.getY();
            final TreePath path = getPathForLocation(x, y);
            if (path != null) {
                final DefaultMutableTreeNode tn = (DefaultMutableTreeNode) path
                        .getLastPathComponent();
                if (tn.getUserObject() instanceof PythonFunction) {
                    final PythonFunction fun = (PythonFunction) tn
                            .getUserObject();

                    final JPopupMenu popup = new JPopupMenu();
                    popup.add(new AbstractAction("Open in editor") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            final Desktop dt = Desktop.getDesktop();
                            try {
                                dt.open(new File(py.getPyPathForFunction(fun)));
                            } catch (final IOException e1) {
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
        final DefaultTreeModel dtm = (DefaultTreeModel) getModel();
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Functions");
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
                if (me.getClickCount() == 2) {
                    final TreePath tp = getPathForLocation(me.getX(), me.getY());
                    if (tp == null)
                        return;
                    final DefaultMutableTreeNode tn = (DefaultMutableTreeNode) tp
                            .getLastPathComponent();
                    final Object o = tn.getUserObject();
                    if (o instanceof PythonFunction) {
                        latestFunction = (PythonFunction) o;

                        final boolean invokable = latestFunction.canBeInvoked();
                        if ((!invokable) || me.isShiftDown()) {
                            final int res = new FunctionParametersDialog(
                                    latestFunction).open();
                            if (res != FunctionParametersDialog.OK)
                                return;
                        }
                        runLatestFunction();
                    }
                }
            }

        });
    }

    public void runLatestFunction() {
        if (latestFunction == null)
            return;
        try {
            latestFunction.invoke();
        } catch (final ScriptException e) {
            e.printStackTrace();
            Report.exception("Exception while running function "+latestFunction, e);
        }
    }

    public PythonFunction getLatestFunction() {
        return latestFunction;
    }
}
