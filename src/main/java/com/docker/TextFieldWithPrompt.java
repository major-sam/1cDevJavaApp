package com.docker;

import javax.swing.*;
import java.awt.*;

public class TextFieldWithPrompt extends JTextField {

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);

        if(getText().isEmpty() && ! (FocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == this)){
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setBackground(Color.gray);
          //  g2.setFont(getFont().deriveFont(Font.ITALIC));
            g2.drawString("Можно оставить пустым", 3, 15); //figure out x, y from font's FontMetrics and size of component.
            g2.dispose();
        }
    }
}