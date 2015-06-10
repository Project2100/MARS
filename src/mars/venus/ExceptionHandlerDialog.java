package mars.venus;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import mars.Main;

/*
 Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

 Developed by Pete Sanderson (psanderson@otterbein.edu)
 and Kenneth Vollmar (kenvollmar@missouristate.edu)

 Permission is hereby granted, free of charge, to any person obtaining 
 a copy of this software and associated documentation files (the 
 "Software"), to deal in the Software without restriction, including 
 without limitation the rights to use, copy, modify, merge, publish, 
 distribute, sublicense, and/or sell copies of the Software, and to 
 permit persons to whom the Software is furnished to do so, subject 
 to the following conditions:

 The above copyright notice and this permission notice shall be 
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 (MIT license, http://www.opensource.org/licenses/mit-license.html)
 */
/**
 *
 * @author Project2100
 */
final class ExceptionHandlerDialog extends JDialog {

    JTextField exceptionHandlerDisplay;
    JButton exceptionHandlerSelectionButton;
    String initialPathname; // selected exception handler when dialog initiated.
    JCheckBox exceptionHandlerSetting;
    boolean initialSelected; // state of check box when dialog initiated.

    ExceptionHandlerDialog() {

        super(Main.getGUI().mainFrame, "Exception Handler", true);
        initialSelected = Main.getSettings().getExceptionHandlerEnabled();
        initialPathname = Main.getSettings().getExceptionHandler();

        exceptionHandlerSetting = new JCheckBox("Include this exception handler file in all assemble operations");
        exceptionHandlerSetting.setSelected(Main.getSettings().getExceptionHandlerEnabled());
        exceptionHandlerSetting.addActionListener((ActionEvent e) -> {
            boolean selected = ((JCheckBox) e.getSource()).isSelected();
            exceptionHandlerSelectionButton.setEnabled(selected);
            exceptionHandlerDisplay.setEnabled(selected);
        });

        exceptionHandlerSelectionButton = new JButton("Browse");
        exceptionHandlerSelectionButton.setEnabled(exceptionHandlerSetting.isSelected());
        exceptionHandlerSelectionButton.addActionListener((ActionEvent e) -> {
            JFileChooser chooser = new JFileChooser();
            String pathname = Main.getSettings().getExceptionHandler();
            if (pathname != null) {
                File file = new File(pathname);
                if (file.exists()) chooser.setSelectedFile(file);
            }
            int result = chooser.showOpenDialog(Main.getGUI().mainFrame);
            if (result == JFileChooser.APPROVE_OPTION) {
                pathname = chooser.getSelectedFile().getPath();//.replaceAll("\\\\","/");
                exceptionHandlerDisplay.setText(pathname);
            }
        });

        exceptionHandlerDisplay = new JTextField(Main.getSettings().getExceptionHandler(), 30);
        exceptionHandlerDisplay.setEditable(false);
        exceptionHandlerDisplay.setEnabled(exceptionHandlerSetting.isSelected());

        JPanel specifyHandlerFile = new JPanel();
        specifyHandlerFile.add(exceptionHandlerSelectionButton);
        specifyHandlerFile.add(exceptionHandlerDisplay);

        Box controlPanel = Box.createHorizontalBox();
        JButton okButton = new JButton("OK");

        okButton.addActionListener((ActionEvent e) -> {
            boolean finalSelected = exceptionHandlerSetting.isSelected();
            String finalPathname = exceptionHandlerDisplay.getText();
            if (initialSelected != finalSelected || initialPathname == null && finalPathname != null || initialPathname != null && !initialPathname.equals(finalPathname)) {
                Main.getSettings().setExceptionHandlerEnabled(finalSelected);
                if (finalSelected)
                    Main.getSettings().setExceptionHandler(finalPathname);
            }
            dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener((ActionEvent e) -> {
            dispose();
        });

        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(okButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(cancelButton);
        controlPanel.add(Box.createHorizontalGlue());

        JPanel contents = new JPanel(new BorderLayout(20, 20));
        contents.setBorder(new EmptyBorder(10, 10, 10, 10));
        contents.add(exceptionHandlerSetting, BorderLayout.NORTH);
        contents.add(specifyHandlerFile, BorderLayout.CENTER);
        contents.add(controlPanel, BorderLayout.SOUTH);
        setContentPane(contents);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            }
        });
        pack();
        setLocationRelativeTo(Main.getGUI().mainFrame);
    }
}
