package org.rosuda.mondrian;//
//	File:		AboutBox.java
//

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class AboutBox extends Frame
        implements ActionListener {


    public AboutBox() {
        super();
        this.setLayout(new BorderLayout(15, 15));
        this.setFont(new Font("SansSerif", Font.BOLD, 14));

        Label aboutText = new Label("About - Mondrian");
        Panel textPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 15, 15));
//		textPanel.add(aboutText);
//		this.add (textPanel, BorderLayout.NORTH);

        this.setFont(new Font("SansSerif", Font.PLAIN, 12));

        Label centerText = new Label("e-mail: mondrian@theusRus.de\n web: http://mondrian.theusRus.de\n book: 9781584885948 (ISBN)");
        textPanel.add(centerText);
        this.add(textPanel, BorderLayout.SOUTH);

        Button okButton = new Button("OK");
        Panel buttonPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.add(okButton);
        okButton.addActionListener(this);
        this.add(buttonPanel, BorderLayout.SOUTH);
    }


    public void actionPerformed(ActionEvent newEvent) {
        setVisible(false);
    }

}