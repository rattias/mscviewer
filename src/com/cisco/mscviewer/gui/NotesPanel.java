package com.cisco.mscviewer.gui;

import java.awt.BorderLayout;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.HTMLEditor;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.cisco.mscviewer.model.Event;

@SuppressWarnings("serial")
public class NotesPanel extends JPanel {
    private HTMLEditor htmlEditor;
    private Event selectedEvent;
    private Pattern pat = Pattern.compile("<body contenteditable=\"true\">(.*)</body>");
    private MainPanel mainPanel;
    
    public NotesPanel(MainPanel mp) {
        mainPanel = mp;
        setLayout(new BorderLayout());
        final JFXPanel fxPanel = new JFXPanel();
        this.add(fxPanel, BorderLayout.CENTER);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(fxPanel);
            }
       });
    }

    private void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        Scene scene = createScene();
        fxPanel.setScene(scene);
    }

    private Scene createScene() {
        htmlEditor = new HTMLEditor();
        htmlEditor.setPrefHeight(245);
        htmlEditor.setDisable(true);
        Scene scene = new Scene(htmlEditor);       
        return (scene);
    }

    public void selectionChanged(Event ev) {
        final Event prevSelected = selectedEvent;
        final String newSelectedHtml;
        if (ev == null || ev.getNote() == null) {
            newSelectedHtml = "";
        } else
            newSelectedHtml = ev.getNote();
        Platform.runLater(() -> {
            htmlEditor.setDisable(ev == null);
            if (prevSelected != null) {
                updateEventInternal(prevSelected);
            }
            htmlEditor.setHtmlText(newSelectedHtml);
        });
        selectedEvent = ev;
    }

    private void updateEventInternal(Event ev) {
        String currEditorHtml = htmlEditor.getHtmlText();
        SwingUtilities.invokeLater(() -> {
            Matcher m = pat.matcher(currEditorHtml);
            if (m.find()) {
                String body = m.group(1);
                String prevSelectedNote = body.trim().equals("") ? null : currEditorHtml;
                ev.setNote(prevSelectedNote);
                mainPanel.repaint();
            }
        });       
    }
    
    public void updateSelectedEvent() {
        final Event selEvent = selectedEvent;
        if (selEvent != null) {
            Platform.runLater(() -> {
                updateEventInternal(selEvent);
            });
        }
    }
    
  
}
