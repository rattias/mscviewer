package com.cisco.mscviewer.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.cisco.mscviewer.gui.colorpicker.ColorPicker;
import com.cisco.mscviewer.gui.colorpicker.TextColorPicker;


@SuppressWarnings("serial")
public class NoteEditor extends JPanel {
    public static Color DEFAULT_FOREGROUND = Color.black;
    public static Color DEFAULT_BACKGROUND = Color.white;
    
    private JTextPane tp;
    private StyledToggleButton bold;
    private StyledToggleButton italic;
    private StyledToggleButton underline;
    private ColorPicker fgPicker;
    private Vector<DocumentListener> listeners = new Vector<DocumentListener>();
    private ColorPicker bgPicker;
    private static int FONT_SIZE = 20;
    
    public static void main(String args[]) {
        JFrame f = new JFrame("Test");
        f.setSize(1024, 768);
        Container c = f.getContentPane();
        c.add(new NoteEditor());
        f.setVisible(true);
    }
    
    public void setEnabled(boolean v) {
        super.setEnabled(v);
        tp.setEnabled(v);
        bold.setEnabled(v);
        italic.setEnabled(v);
        underline.setEnabled(v);
        fgPicker.setEnabled(v);
        bgPicker.setEnabled(v);
    }
    public NoteEditor() {
        setLayout(new BorderLayout());
        StyledDocument doc = new DefaultStyledDocument();
        
        tp = new JTextPane(doc);
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, DEFAULT_FOREGROUND);
        StyleConstants.setBackground(set, DEFAULT_BACKGROUND);
        tp.setCharacterAttributes(set, false);
        tp.setEditable(true);
        Font fontB = new Font("Serif", Font.BOLD, FONT_SIZE);
        Font fontI = new Font("Serif", Font.ITALIC, FONT_SIZE);
        HashMap<TextAttribute, Object> textAttrMap = new HashMap<TextAttribute, Object>();
        textAttrMap.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        Font fontU = new Font("Serif", 0, FONT_SIZE).deriveFont(textAttrMap);
        tp.addCaretListener((ev) -> {
           int pos = ev.getDot();
           int otherPos = ev.getMark();
           if (pos == otherPos) {
               AttributeSet as = tp.getCharacterAttributes();
               bold.setSelected(StyleConstants.isBold(as));
               italic.setSelected(StyleConstants.isItalic(as));
               underline.setSelected(StyleConstants.isUnderline(as));
           } else {
               boolean boldOn = true, italicOn = true, underlineOn = true;
               if (pos < otherPos) {
                   int tmp = pos;
                   pos = otherPos;
                   otherPos = tmp;
               }
               for(int i=otherPos; i<pos; i++) {
                   Element el = doc.getCharacterElement(i);
                   AttributeSet at = el.getAttributes();
//                   System.out.println("attrs = "+at);
//                   System.out.println("OFF = "+i+", b="+StyleConstants.isBold(at)+", i="+StyleConstants.isItalic(at)+", u="+StyleConstants.isUnderline(at));
                   boldOn &= StyleConstants.isBold(at);
                   italicOn &= StyleConstants.isItalic(at);
                   underlineOn &= StyleConstants.isUnderline(at);
               }
               bold.setSelected(boldOn);                       
               italic.setSelected(italicOn);                       
               underline.setSelected(underlineOn);                       
           }
        });
        add(new JScrollPane(tp), BorderLayout.CENTER);
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        add(tb, BorderLayout.NORTH);

        SimpleAttributeSet boldOn = new SimpleAttributeSet(); 
        StyleConstants.setBold(boldOn, true);
        SimpleAttributeSet boldOff = new SimpleAttributeSet(); 
        StyleConstants.setBold(boldOff, false);

        SimpleAttributeSet italicOn = new SimpleAttributeSet(); 
        StyleConstants.setItalic(italicOn, true);
        SimpleAttributeSet italicOff = new SimpleAttributeSet(); 
        StyleConstants.setItalic(italicOff, false);

        SimpleAttributeSet underlineOn = new SimpleAttributeSet(); 
        StyleConstants.setUnderline(underlineOn, true);
        SimpleAttributeSet underlineOff = new SimpleAttributeSet(); 
        StyleConstants.setUnderline(underlineOff, false);
        
        HashMap<Color, SimpleAttributeSet> hm = new HashMap<Color, SimpleAttributeSet>();
        
        bold = new StyledToggleButton("B");
        tb.add(bold);
        bold.setToolTipText("bold");
        bold.setFont(fontB);
        bold.setMaximumSize(bold.getPreferredSize());
        bold.addActionListener((ev) -> {
            setStyle(bold.isSelected() ? boldOn : boldOff);
        });
        tb.add(Box.createHorizontalStrut(20));
  
        italic = new StyledToggleButton("I");
        tb.add(italic);
        italic.setToolTipText("Italic");
        italic.setFont(fontI);
        italic.setMaximumSize(bold.getPreferredSize());
        italic.addActionListener((ev) -> {
            setStyle(italic.isSelected() ? italicOn : italicOff);
        });
        tb.add(Box.createHorizontalStrut(20));

        underline = new StyledToggleButton("U");
        tb.add(underline);
        underline.setToolTipText("Underline");
        underline.setFont(fontU);
        underline.setMaximumSize(bold.getPreferredSize());
        underline.addActionListener((ev) -> {
            setStyle(underline.isSelected() ? underlineOn : underlineOff);
        });
        tb.add(Box.createHorizontalStrut(20));

        fgPicker = new TextColorPicker(TextColorPicker.TYPE_FOREGROUND);
        JLayer<ColorPicker> jl = new JLayer<ColorPicker>(fgPicker, new RolloverUI<ColorPicker>());
        tb.add(jl);
        jl.setToolTipText("Foreground Color");
        fgPicker.setToolTipText("Foreground Color");
        fgPicker.addColorSelectionListener((c) -> {
           SimpleAttributeSet s = hm.get(c);
           if (s == null) {
               s = new SimpleAttributeSet();
               StyleConstants.setForeground(s, c);
               hm.put(c, s);
           }
           setStyle(s);
        });
        tb.add(Box.createHorizontalStrut(20));

        bgPicker = new TextColorPicker(TextColorPicker.TYPE_BACKGROUND);
        jl = new JLayer<ColorPicker>(bgPicker, new RolloverUI<ColorPicker>());
        jl.setToolTipText("Background Color");
        tb.add(jl);
        bgPicker.setToolTipText("Background Color");
        bgPicker.addColorSelectionListener((c) -> {
           SimpleAttributeSet s = hm.get(c);
           if (s == null) {
               s = new SimpleAttributeSet();
               StyleConstants.setBackground(s, c);
               hm.put(c, s);
           }
           setStyle(s);
        });
        tb.add(Box.createHorizontalGlue());

    }
    

    private void setStyle(SimpleAttributeSet attrs) {
        int start = tp.getSelectionStart();
        int end = tp.getSelectionEnd();
        if (start != end)
            tp.getStyledDocument().setCharacterAttributes(start, end-start, attrs, false);
        else
            tp.setCharacterAttributes(attrs, false);
        tp.requestFocusInWindow();
    }
    
    
    public void registerDocumentChangeListener(DocumentListener l) {
        listeners .add(l);
        tp.getDocument().addDocumentListener(l);
    }

    public void setDocument(StyledDocument doc) {
        tp.setDocument(doc);
        
    }

    public StyledDocument getDocument() {
        return tp.getStyledDocument();
    }
}
