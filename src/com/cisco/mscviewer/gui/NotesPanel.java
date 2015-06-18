package com.cisco.mscviewer.gui;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.cisco.mscviewer.model.Event;
import com.cisco.mscviewer.util.StyledDocumentUtils;

@SuppressWarnings("serial")
public class NotesPanel extends JPanel implements DocumentListener {
    public static final String NAME = "Event Note";
    private MainPanel mainPanel;
    private NoteEditor editor;
    
    public NotesPanel(MainPanel mp) {
        mainPanel = mp;
        setLayout(new BorderLayout());
        editor = new NoteEditor();
        editor.setEnabled(false);
        editor.registerDocumentChangeListener(this);
        add(editor, BorderLayout.CENTER);
    }

    public void selectionChanged(Event ev) {
        final String newSelectedContent;
        if (ev == null || ev.getNote() == null) {
            newSelectedContent = null;
        } else
            newSelectedContent = ev.getNote();
        editor.setEnabled(ev != null);
        StyledDocumentUtils.importXML(newSelectedContent, editor.getDocument());
    }

    private void updateEventFromEditor() {
        Event ev = mainPanel.getMSCRenderer().getSelectedEvent();
        if (ev != null) {
            String content = StyledDocumentUtils.exportXML(editor.getDocument());
//            System.out.println("CONTENT: "+content);
            ev.setNote(content);
            mainPanel.repaint();
        }
    }
    
    @Override
    public void insertUpdate(DocumentEvent e) {
        updateEventFromEditor();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        updateEventFromEditor();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        updateEventFromEditor();
    }

    public NoteEditor getEditor() {
        return editor;
    }
    
    public void makeVisible() {
        JComponent c = this;
        while (! (c instanceof JTabbedPane))
            c = (JComponent)c.getParent();
        JTabbedPane p = (JTabbedPane)c;
        p.setSelectedComponent(this);
    }

}
