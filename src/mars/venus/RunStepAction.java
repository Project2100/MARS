package mars.venus;

import mars.*;
import mars.simulator.*;
import mars.mips.hardware.*;
import java.awt.event.*;
import javax.swing.*;

/*
 Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

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
 * Action for the Run -> Step menu item
 */
public class RunStepAction extends GuiAction {

    String name;
    ExecutePane executePane;

    public RunStepAction(String name, Icon icon, String descrip,
            Integer mnemonic, KeyStroke accel, VenusUI gui) {
        super(name, icon, descrip, mnemonic, accel, gui);
    }

    /**
     * perform next simulated instruction step.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        name = this.getValue(Action.NAME).toString();
        executePane = mainUI.executePane;
        boolean done = false;
        if (executePane.isShowing()) {
            if (!VenusUI.getStarted()) // DPS 17-July-2008
                processProgramArgumentsIfAny();
            VenusUI.setStarted(true);
            mainUI.messagesPane.setSelectedComponent(mainUI.messagesPane.runTab);
            executePane.getTextSegmentWindow().setCodeHighlighting(true);
            try {
                done = Main.program.simulateStepAtPC(this);
            }
            catch (ProcessingException ev) {
            }
        }
        else
            // note: this should never occur since "Step" is only enabled after successful assembly.
            JOptionPane.showMessageDialog(Main.getGUI().mainFrame, "The program must be assembled before it can be run.");
    }

   	// When step is completed, control returns here (from execution thread, indirectly) 
    // to simUpdate the GUI.
    public void stepped(boolean done, int reason, ProcessingException pe) {
        Main.getGUI().registersTab.updateRegisters();
        Main.getGUI().coprocessor1Tab.updateRegisters();
        Main.getGUI().coprocessor0Tab.updateRegisters();
        executePane.getDataSegmentWindow().updateValues();
        if (!done) {
            executePane.getTextSegmentWindow().highlightStepAtPC();
            Main.getGUI().setMenuStateRunnable();
        }
        if (done) {
            RunGoAction.resetMaxSteps();
            executePane.getTextSegmentWindow().unhighlightAllSteps();
            Main.getGUI().setMenuStateTerminated();
        }
        if (done && pe == null) {
            (mainUI.messagesPane).postMarsMessage(
                    "\n" + name + ": execution "
                    + ((reason == Simulator.CLIFF_TERMINATION) ? "terminated due to null instruction."
                            : "completed successfully.") + "\n\n");
            (mainUI.messagesPane).postRunMessage(
                    "\n-- program is finished running "
                    + ((reason == Simulator.CLIFF_TERMINATION) ? "(dropped off bottom)" : "") + " --\n\n");
            (mainUI.messagesPane).selectRunMessageTab();
        }
        if (pe != null) {
            RunGoAction.resetMaxSteps();
            (mainUI.messagesPane).postMarsMessage(
                    pe.errors().generateErrorReport());
            (mainUI.messagesPane).postMarsMessage(
                    "\n" + name + ": execution terminated with errors.\n\n");
            (mainUI.registersPane).setSelectedComponent(Main.getGUI().coprocessor0Tab);
            Main.getGUI().setMenuStateTerminated(); // should be redundant.
            executePane.getTextSegmentWindow().setCodeHighlighting(true);
            executePane.getTextSegmentWindow().unhighlightAllSteps();
            executePane.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.getProgramCounter() - 4);
        }
        VenusUI.setReset(false);
    }

		////////////////////////////////////////////////////////////////////////////////////
    // Method to store any program arguments into MIPS memory and registers before
    // execution begins. Arguments go into the gap between $sp and kernel memory.  
    // Argument pointers and count go into runtime stack and $sp is adjusted accordingly.
    // $a0 gets argument count (argc), $a1 gets stack address of first arg pointer (argv).
    private void processProgramArgumentsIfAny() {
        String programArguments = executePane.getTextSegmentWindow().getProgramArguments();
        if (programArguments == null || programArguments.length() == 0
                || !Main.getSettings().getProgramArguments())
            return;
        new ProgramArgumentList(programArguments).storeProgramArguments();
    }
}
