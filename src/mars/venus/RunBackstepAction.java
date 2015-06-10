package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import mars.Main;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;

/*
 Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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
 * Action for the Run -> Backstep menu item
 */
public class RunBackstepAction extends GuiAction {

//    String name;
//    ExecutePane executePane;

    public RunBackstepAction(String name, Icon icon, String descrip,
            Integer mnemonic, KeyStroke accel, VenusUI gui) {
        super(name, icon, descrip, mnemonic, accel, gui);
    }

    /**
     * perform next simulated instruction step.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        
        if (!Main.getGUI().executePane.isShowing()) {
            // note: this should never occur since backstepping is only enabled after successful assembly.
            JOptionPane.showMessageDialog(Main.getGUI().mainFrame, "The program must be assembled before it can be run.");
            return;
        }
        VenusUI.setStarted(true);
        Main.getGUI().messagesPane.setSelectedComponent(Main.getGUI().messagesPane.runTab);
        Main.getGUI().executePane.getTextSegmentWindow().setCodeHighlighting(true);

        if (Main.getSettings().getBackSteppingEnabled()) {
            boolean inDelaySlot = Main.program.getBackStepper().inDelaySlot(); // Added 25 June 2007
            Memory.getInstance().addObserver(Main.getGUI().executePane.getDataSegmentWindow());
            RegisterFile.addRegistersObserver(Main.getGUI().registersTab);
            Coprocessor0.addRegistersObserver(Main.getGUI().coprocessor0Tab);
            Coprocessor1.addRegistersObserver(Main.getGUI().coprocessor1Tab);
            Main.program.getBackStepper().backStep();
            Memory.getInstance().deleteObserver(Main.getGUI().executePane.getDataSegmentWindow());
            RegisterFile.deleteRegistersObserver(Main.getGUI().registersTab);
            Main.getGUI().registersTab.updateRegisters();
            Main.getGUI().coprocessor1Tab.updateRegisters();
            Main.getGUI().coprocessor0Tab.updateRegisters();
            Main.getGUI().executePane.getDataSegmentWindow().updateValues();
            Main.getGUI().executePane.getTextSegmentWindow().highlightStepAtPC(inDelaySlot); // Argument aded 25 June 2007
            Main.getGUI().setMenuStateRunnable();
            
            VenusUI.setReset(false);
        }
    }
}
