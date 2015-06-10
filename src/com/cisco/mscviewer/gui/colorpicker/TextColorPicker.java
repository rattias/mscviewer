package com.cisco.mscviewer.gui.colorpicker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToolBar;

import com.cisco.mscviewer.gui.StyledButton;

@SuppressWarnings("serial")
class Letter extends StyledButton implements ColorSelectionListener {
    private Font f;
    private String txt;
    private ColorPicker cp;
    private int type;

    public Letter(int type, String txt, int height) {
        this.type = type;
        this.txt = txt;
        f = new Font("Serif", Font.BOLD, height);
        setMaximumSize(new Dimension(32, 32));
        setPreferredSize(new Dimension(32, 32));
        addActionListener((e) -> {
            getColorPicker().notifyColorSelectionListeners();
        });
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
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Color color = getColorPicker().getSelectedColor();
        g.setColor(color);
        if (type == TextColorPicker.TYPE_FOREGROUND)
            g.fillRect(1, getHeight()-4, getWidth()-2, getHeight()-2);
        else
            g.fillRect(3, 3, getWidth()-4, getHeight()-4);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D r = fm.getStringBounds(txt, g);
        int x = (int)(getWidth()-r.getWidth())/2;
        int y = (int)(getHeight()-r.getHeight())/2+fm.getAscent();
        if (type == TextColorPicker.TYPE_FOREGROUND)
            g.setColor(Color.black);
        else {
            int avg = (color.getRed()+color.getGreen()+color.getBlue())/3;
            g.setColor(avg<128 ? Color.white : Color.black);
        }
        g.drawString(txt, x, y);
    }

    @Override
    public void colorSelected(Color c) {
        repaint();            
    }

};

@SuppressWarnings("serial")
public class TextColorPicker extends ColorPicker {
    public static int TYPE_FOREGROUND = 1;
    public static int TYPE_BACKGROUND = 2;
    
    static Color[][] colors = {
            {new Color(0x000000), Color.red,    Color.red.darker(),     Color.red.darker().darker(),    Color.red.darker().darker().darker()},
            {new Color(0x222222), Color.pink,   Color.pink.darker(),    Color.pink.darker().darker(),   Color.pink.darker().darker().darker()},
            {new Color(0x444444), Color.orange, Color.orange.darker(),  Color.orange.darker().darker(), Color.orange.darker().darker().darker()},
            {new Color(0x666666), Color.yellow, Color.yellow.darker(),  Color.yellow.darker().darker(), Color.yellow.darker().darker().darker()},
            {new Color(0x888888), Color.green,  Color.green.darker(),   Color.green.darker().darker(),  Color.green.darker().darker().darker()},
            {new Color(0xaaaaaa), Color.magenta,Color.magenta.darker(), Color.magenta.darker().darker(),Color.magenta.darker().darker().darker()},
            {new Color(0xcccccc), Color.cyan,   Color.cyan.darker(),    Color.cyan.darker().darker(),   Color.cyan.darker().darker().darker()},
            {new Color(0xffffff), Color.blue,   Color.blue.darker(),    Color.blue.darker().darker(),   Color.blue.darker().darker().darker()}
    };                


    public TextColorPicker(int type) {
        super(new Letter(type, "A", 16), colors, type == TYPE_FOREGROUND ? 0 : 7, 0);
    }
    
    public static void main(String args[]) {
        JFrame f = new JFrame();
        f.setSize(1024, 768);
        Container c = f.getContentPane();
        c.setLayout(new BorderLayout());
        JToolBar b = new JToolBar();
        c.add(b, BorderLayout.NORTH);
        b.add(new TextColorPicker(TYPE_FOREGROUND));
        f.setVisible(true);
    }

}
