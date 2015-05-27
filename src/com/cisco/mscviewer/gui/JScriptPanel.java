/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jun 2011
 */
package com.cisco.mscviewer.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
class JScriptPanel extends JPanel {
    private ScriptEngine engine;
    private final JTextArea scriptTA;
    private final JTextArea errorTA;
    private final Timer t;
    private String scriptContext;

    public JScriptPanel() {
        final JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        setLayout(new BorderLayout());
        add(jsp, BorderLayout.CENTER);
        jsp.setResizeWeight(.7);
        final ScriptEngineManager mgr = new ScriptEngineManager();
        final List<ScriptEngineFactory> factories = mgr.getEngineFactories();
        for (final ScriptEngineFactory factory : factories) {
            final String langName = factory.getLanguageName();
            final String langVersion = factory.getLanguageVersion();
            if (langName.equals("ECMAScript") && langVersion.equals("1.6")) {
                engine = factory.getScriptEngine();
                break;
            }
        }
        if (engine == null) {
            throw new Error("Unable to retrieve JavaScript engine.");
        }
        scriptTA = new JTextArea();
        jsp.setTopComponent(new JScrollPane(scriptTA));
        errorTA = new JTextArea();
        errorTA.setForeground(Color.red);
        errorTA.setEditable(false);
        jsp.setBottomComponent(new JScrollPane(errorTA));

        final TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            engine.eval(getScript());
                            errorTA.setText("");
                        } catch (final ScriptException e) {
                            errorTA.setText(e.getMessage());
                        }
                    }
                });
            }
        };
        t = new Timer();
        t.schedule(tt, 1000, 1000);
    }

    public String getScript() {
        return scriptContext.replace("$$", scriptTA.getText());
    }

    public void setScriptContext(String string) {
        scriptContext = string;

    }
}
