package mars.venus;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import mars.Main;
import mars.ProcessingException;
import mars.mips.hardware.RegisterFile;
import mars.settings.BooleanSettings;
import mars.simulator.ProgramArgumentList;
import mars.simulator.Simulator;
import mars.util.SystemIO;

/*
 Copyright (c) 2003-2007,  Pete Sanderson and Kenneth Vollmar

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
 * Action class for the Run -> Go menu item (and toolbar icon)
 */
public class RunGoAction extends AbstractAction {

    public static final int defaultMaxSteps = -1; // "forever", formerly 10000000; // 10 million
    public static int maxSteps = defaultMaxSteps;

    public RunGoAction() {
        super("Go", new ImageIcon(GuiAction.class.getResource(Main.imagesPath + "Play16.png")));

        putValue(LARGE_ICON_KEY, new ImageIcon(GuiAction.class.getResource(Main.imagesPath + "Play22.png")));
        putValue(SHORT_DESCRIPTION, "Run the current program");
        putValue(MNEMONIC_KEY, KeyEvent.VK_G);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
    }

    /**
     * Action to take when GO is selected -- run the MIPS program!
     *
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (Main.getGUI().executePane.isShowing()) {
            if (!VenusUI.getStarted())
                processProgramArgumentsIfAny(); // DPS 17-July-2008
            if (VenusUI.getReset() || VenusUI.getStarted()) {

                VenusUI.setStarted(true);  // added 8/27/05

                Main.getGUI().messagesPane.postMarsMessage("Running " + Main.getGUI().editTabbedPane.getSelectedComponent().getPath().getFileName().toString() + "\n\n");
                Main.getGUI().messagesPane.selectRunMessageTab();
                Main.getGUI().textSegment.setCodeHighlighting(false);
                Main.getGUI().textSegment.unhighlightAllSteps();
                Main.getGUI().setMenuStateRunning();
                try {
                    Main.program.simulateFromPC(
                            Main.getGUI().textSegment.getSortedBreakPointsArray(),
                            maxSteps,
                            this);
                }
                catch (ProcessingException pe) {
                }
            }
            else
                // This should never occur because at termination the Go and Step buttons are disabled.
                JOptionPane.showMessageDialog(Main.getGUI().mainFrame, "reset " + VenusUI.getReset() + " started " + VenusUI.getStarted());//"You must reset before you can execute the program again.");                 
        }
        else
            // note: this should never occur since "Go" is only enabled after successful assembly.
            JOptionPane.showMessageDialog(Main.getGUI().mainFrame, "The program must be assembled before it can be run.");
    }

    /**
     * Method to be called when Pause is selected through menu/toolbar/shortcut.
     * This should only happen when MIPS program is running
     * (FileStatus.RUNNING). See VenusUI.java for enabled status of menu items
     * based on FileStatus. Set GUI as if at breakpoint or executing step by
     * step.
     *
     * @param done
     * @param pauseReason
     * @param pe
     */
    public static void paused(boolean done, int pauseReason, ProcessingException pe) {
        // I doubt this can happen (pause when execution finished), but if so treat it as stopped.
        if (done) {
            stopped(pe, Simulator.NORMAL_TERMINATION);
            return;
        }

        Main.getGUI().messagesPane.postMarsMessage("Execution paused at "
                + ((pauseReason == Simulator.BREAKPOINT)? "breakpoint: " : "user: ")
                + Main.getGUI().editTabbedPane.getSelectedComponent().getPath().getFileName().toString()
                + "\n\n");

        Main.getGUI().messagesPane.selectMarsMessageTab();
        Main.getGUI().textSegment.setCodeHighlighting(true);
        Main.getGUI().textSegment.highlightStepAtPC();
        Main.getGUI().registersTab.updateRegisters();
        Main.getGUI().coprocessor1Tab.updateRegisters();
        Main.getGUI().coprocessor0Tab.updateRegisters();
        Main.getGUI().dataSegment.updateValues();
        Main.getGUI().setMenuStateRunnable();
        VenusUI.setReset(false);
    }

    /**
     * Method to be called when Stop is selected through menu/toolbar/shortcut.
     * This should only happen when MIPS program is running
     * (FileStatus.RUNNING). See VenusUI.java for enabled status of menu items
     * based on FileStatus. Display finalized values as if execution terminated
     * due to completion or exception.
     *
     * @param pe
     * @param reason
     */
    public static void stopped(ProcessingException pe, int reason) {
        // show final register and data segment values.
        Main.getGUI().registersTab.updateRegisters();
        Main.getGUI().coprocessor1Tab.updateRegisters();
        Main.getGUI().coprocessor0Tab.updateRegisters();
        Main.getGUI().dataSegment.updateValues();
        Main.getGUI().setMenuStateTerminated();
        SystemIO.resetFiles(); // close any files opened in MIPS program
        // Bring coprocessor 0 to the front if terminated due to exception.
        if (pe != null) {
            Main.getGUI().registersPane.setSelectedComponent(Main.getGUI().coprocessor0Tab);
            Main.getGUI().textSegment.setCodeHighlighting(true);
            Main.getGUI().textSegment.unhighlightAllSteps();
            Main.getGUI().textSegment.highlightStepAtAddress(RegisterFile.getProgramCounter() - 4);
        }
        switch (reason) {
            case Simulator.NORMAL_TERMINATION:
                Main.getGUI().messagesPane.postMarsMessage(
                        "\nExecution completed successfully.\n\n");
                Main.getGUI().messagesPane.postRunMessage(
                        "\n-- program is finished running --\n\n");
                Main.getGUI().messagesPane.selectRunMessageTab();
                break;
            case Simulator.CLIFF_TERMINATION:
                Main.getGUI().messagesPane.postMarsMessage(
                        "\nExecution terminated by null instruction.\n\n");
                Main.getGUI().messagesPane.postRunMessage(
                        "\n-- program is finished running (dropped off bottom) --\n\n");
                Main.getGUI().messagesPane.selectRunMessageTab();
                break;
            case Simulator.EXCEPTION:
                Main.getGUI().messagesPane.postMarsMessage(
                        pe.errors().generateErrorReport());
                Main.getGUI().messagesPane.postMarsMessage(
                        "\nExecution terminated with errors.\n\n");
                break;
            case Simulator.PAUSE_OR_STOP:
                Main.getGUI().messagesPane.postMarsMessage(
                        "\nExecution terminated by user.\n\n");
                Main.getGUI().messagesPane.selectMarsMessageTab();
                break;
            case Simulator.MAX_STEPS:
                Main.getGUI().messagesPane.postMarsMessage(
                        "\nExecution step limit of " + maxSteps + " exceeded.\n\n");
                Main.getGUI().messagesPane.selectMarsMessageTab();
                break;
            case Simulator.BREAKPOINT: // should never get here
                break;
        }
        RunGoAction.resetMaxSteps();
        VenusUI.setReset(false);
    }

    /**
     * Reset max steps limit to default value at termination of a simulated
     * execution.
     */
    public static void resetMaxSteps() {
        maxSteps = defaultMaxSteps;
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Method to store any program arguments into MIPS memory and registers before
    // execution begins. Arguments go into the gap between $sp and kernel memory.  
    // Argument pointers and count go into runtime stack and $sp is adjusted accordingly.
    // $a0 gets argument count (argc), $a1 gets stack address of first arg pointer (argv).
    private void processProgramArgumentsIfAny() {
        String programArguments = Main.getGUI().textSegment.getProgramArguments();
        if (programArguments == null || programArguments.length() == 0
                || !BooleanSettings.PROGRAM_ARGUMENTS.isSet())
            return;
        new ProgramArgumentList(programArguments).storeProgramArguments();
    }

}
