/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.venus;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

/**
 *
 * @author Project2100
 */
public class ExceptionHandlerDialog extends JDialog{
    JTextField exceptionHandlerDisplay;
    JButton exceptionHandlerSelectionButton;
    String initialPathname; // selected exception handler when dialog initiated.
    JCheckBox exceptionHandlerSetting;
//    JDialog exceptionHandlerDialog;
    boolean initialSelected; // state of check box when dialog initiated.

    
    ExceptionHandlerDialog() {
        
        
         super(Main.getGUI().mainFrame, "Exception Handler", true);
         initialSelected = Main.getSettings().getExceptionHandlerEnabled();
         initialPathname = Main.getSettings().getExceptionHandler();
         
         setContentPane(buildDialogPanel());
         setDefaultCloseOperation(
                        JDialog.DO_NOTHING_ON_CLOSE);
         addWindowListener(new WindowAdapter() {
                   public void windowClosing(WindowEvent we) {
                     closeDialog();
                  }
               });
         pack();
         setLocationRelativeTo(Main.getGUI().mainFrame);
         
         
    }
    
    // User has clicked "OK" button, so record status of the checkbox and text field.
    private void performOK() {
        boolean finalSelected = exceptionHandlerSetting.isSelected();
        String finalPathname = exceptionHandlerDisplay.getText();
        if (initialSelected != finalSelected || initialPathname == null && finalPathname != null || initialPathname != null && !initialPathname.equals(finalPathname)) {
            Main.getSettings().setExceptionHandlerEnabled(finalSelected);
            if (finalSelected)
                Main.getSettings().setExceptionHandler(finalPathname);
        }
    }

    // The dialog box that appears when menu item is selected.
    JPanel buildDialogPanel() {
        JPanel contents = new JPanel(new BorderLayout(20, 20));
        contents.setBorder(new EmptyBorder(10, 10, 10, 10));
        exceptionHandlerSetting = new JCheckBox("Include this exception handler file in all assemble operations");
        exceptionHandlerSetting.setSelected(Main.getSettings().getExceptionHandlerEnabled());
        exceptionHandlerSetting.addActionListener(new ExceptionHandlerSettingAction());
        contents.add(exceptionHandlerSetting, BorderLayout.NORTH);
        JPanel specifyHandlerFile = new JPanel();
        exceptionHandlerSelectionButton = new JButton("Browse");
        exceptionHandlerSelectionButton.setEnabled(exceptionHandlerSetting.isSelected());
        exceptionHandlerSelectionButton.addActionListener(new ExceptionHandlerSelectionAction());
        exceptionHandlerDisplay = new JTextField(Main.getSettings().getExceptionHandler(), 30);
        exceptionHandlerDisplay.setEditable(false);
        exceptionHandlerDisplay.setEnabled(exceptionHandlerSetting.isSelected());
        specifyHandlerFile.add(exceptionHandlerSelectionButton);
        specifyHandlerFile.add(exceptionHandlerDisplay);
        contents.add(specifyHandlerFile, BorderLayout.CENTER);
        Box controlPanel = Box.createHorizontalBox();
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                performOK();
                closeDialog();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeDialog();
            }
        });
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(okButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(cancelButton);
        controlPanel.add(Box.createHorizontalGlue());
        contents.add(controlPanel, BorderLayout.SOUTH);
        return contents;
    }

    // We're finished with this modal dialog.
    void closeDialog() {
        setVisible(false);
        dispose();
    }
 
    /////////////////////////////////////////////////////////////////////////////////
   	// Associated action class: exception handler setting.  Attached to check box.   	
       private class ExceptionHandlerSettingAction implements ActionListener {
          public void actionPerformed(ActionEvent e) {
            boolean selected = ((JCheckBox) e.getSource()).isSelected();
            exceptionHandlerSelectionButton.setEnabled(selected);
            exceptionHandlerDisplay.setEnabled(selected);
         }
      }
   				
   				
   	/////////////////////////////////////////////////////////////////////////////////
   	// Associated action class: selecting exception handler file.  Attached to handler selector.
       private class ExceptionHandlerSelectionAction implements ActionListener {
          public void actionPerformed(ActionEvent e) {
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
         }
      }
    
}
