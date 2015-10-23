package mars.venus;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import mars.Main;
import mars.ProcessingException;
import mars.mips.hardware.RegisterFile;
import mars.settings.BooleanSettings;
import mars.simulator.ProgramArgumentList;
import mars.simulator.Simulator;

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
public class RunStepAction extends AbstractAction {

    public RunStepAction() {
        super("Step", new ImageIcon(GuiAction.class.getResource(Main.imagesPath + "StepForward16.png")));

        putValue(LARGE_ICON_KEY, new ImageIcon(GuiAction.class.getResource(Main.imagesPath + "StepForward22.png")));
        putValue(SHORT_DESCRIPTION, "Run one step at a time");
        putValue(MNEMONIC_KEY, KeyEvent.VK_T);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
    }

    /**
     * perform next simulated instruction step.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (Main.getGUI().executePane.isShowing()) {
            if (!VenusUI.getStarted()) { // DPS 17-July-2008
                ////////////////////////////////////////////////////////////////////////////////////
                // Store any program arguments into MIPS memory and registers before
                // execution begins. Arguments go into the gap between $sp and kernel memory.  
                // Argument pointers and count go into runtime stack and $sp is adjusted accordingly.
                // $a0 gets argument count (argc), $a1 gets stack address of first arg pointer (argv).
                String programArguments = Main.getGUI().textSegment.getProgramArguments();
                if (programArguments != null && programArguments.length() != 0
                        && BooleanSettings.PROGRAM_ARGUMENTS.isSet())
                    new ProgramArgumentList(programArguments).storeProgramArguments();
            }
            VenusUI.setStarted(true);
            Main.getGUI().messagesPane.setSelectedComponent(Main.getGUI().messagesPane.runTab);
            Main.getGUI().textSegment.setCodeHighlighting(true);
            try {
                Main.program.simulateStepAtPC(this);
            }
            catch (ProcessingException ev) {
            }
        }
        else
            // note: this should never occur since "Step" is only enabled after successful assembly.
            JOptionPane.showMessageDialog(Main.getGUI().mainFrame, "The program must be assembled before it can be run.");
    }

    // When step is completed, control returns here (from execution thread, indirectly) 
    // to update the GUI.
    public void stepped(boolean done, int reason, ProcessingException pe) {
        Main.getGUI().registersTab.updateRegisters();
        Main.getGUI().coprocessor1Tab.updateRegisters();
        Main.getGUI().coprocessor0Tab.updateRegisters();
        Main.getGUI().dataSegment.updateValues();
        if (!done) {
            Main.getGUI().textSegment.highlightStepAtPC();
            Main.getGUI().setMenuStateRunnable();
        }
        if (done) {
            RunGoAction.resetMaxSteps();
            Main.getGUI().textSegment.unhighlightAllSteps();
            Main.getGUI().setMenuStateTerminated();
        }
        if (done && pe == null) {
            Main.getGUI().messagesPane.postMarsMessage(
                    "\n" + getValue(Action.NAME) + ": execution "
                    + ((reason == Simulator.CLIFF_TERMINATION) ? "terminated due to null instruction."
                            : "completed successfully.") + "\n\n");
            Main.getGUI().messagesPane.postRunMessage(
                    "\n-- program is finished running "
                    + ((reason == Simulator.CLIFF_TERMINATION) ? "(dropped off bottom)" : "") + " --\n\n");
            Main.getGUI().messagesPane.selectRunMessageTab();
        }
        if (pe != null) {
            RunGoAction.resetMaxSteps();
            Main.getGUI().messagesPane.postMarsMessage(
                    pe.errors().generateErrorReport());
            Main.getGUI().messagesPane.postMarsMessage(
                    "\n" + getValue(Action.NAME) + ": execution terminated with errors.\n\n");
            Main.getGUI().registersPane.setSelectedComponent(Main.getGUI().coprocessor0Tab);
            Main.getGUI().setMenuStateTerminated(); // should be redundant.
            Main.getGUI().textSegment.setCodeHighlighting(true);
            Main.getGUI().textSegment.unhighlightAllSteps();
            Main.getGUI().textSegment.highlightStepAtAddress(RegisterFile.getProgramCounter() - 4);
        }
        VenusUI.setReset(false);
    }

}
