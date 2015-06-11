package com.cisco.mscviewer.gui.colorpicker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import com.cisco.mscviewer.gui.Marker;
import com.cisco.mscviewer.gui.StyledToggleButton;
import com.cisco.mscviewer.gui.Styles;
import com.cisco.mscviewer.util.Resources;

@SuppressWarnings("serial")
class HighlighterButton extends StyledToggleButton implements ColorSelectionListener {
    final static ImageIcon[] imgs = new ImageIcon[4];

    private ColorPicker cp;

    private static ImageIcon getImageIcon(Color c) {
        for(int i=0; i<HighlighterColorPicker.colors.length; i++) {
            if (HighlighterColorPicker.colors[i].equals(c)) {
                String name = HighlighterColorPicker.markers[i].getName();
                return getImageIcon(name);
            }
        }
        return null;
    }

    public HighlighterButton(int sz) {
        ImageIcon icn = getImageIcon(HighlighterColorPicker.markers[0].getName()); 
        setIcon(icn);
        
        addActionListener((e) -> {
            // button was pressed: notify all listeners
            getColorPicker().notifyColorSelectionListeners();
        });        
    }



    private static ImageIcon getImageIcon(String name) {
        String res = "highlight_"+name+".png";
        ImageIcon icn = Resources.getImageIcon(res, name);
        return icn;
    }


    private ColorPicker getColorPicker() {
        if (cp == null) {
            Component c = this;
            while(! (c instanceof ColorPicker))
                c = c.getParent();
            cp = (ColorPicker)c;
        }
        return cp;
    }


    @Override
    public void colorSelected(Color c) {
        setIcon(getImageIcon(c));
        repaint();
    }


};

@SuppressWarnings("serial")
public class HighlighterColorPicker extends ColorPicker {
    public final static Marker[] markers = {Marker.GREEN, Marker.BLUE, Marker.YELLOW, Marker.RED};
    public final static Color[] colors = {Color.GREEN, Color.BLUE, Color.YELLOW, Color.RED};

    static String getColorName(Color c) {
        for(int i=0; i<colors.length; i++) {
            if (colors[i].equals(c))
                return markers[i].getName();
        }
        return null;
    }
    
    public HighlighterColorPicker(int sz) {
        super(new HighlighterButton(sz), new Color[][]{colors}, 0, 0);
        addColorSelectionListener((HighlighterButton)getButton());
        getButton().setToolTipText("Highlighter tool");
    }

    public static void main(String args[]) {
        JFrame f = new JFrame();
        f.setSize(1024, 768);
        Container c = f.getContentPane();
        c.setLayout(new BorderLayout());
        JToolBar b = new JToolBar();
        c.add(b, BorderLayout.NORTH);
        b.add(new HighlighterColorPicker(32));
        f.setVisible(true);
    }

}
