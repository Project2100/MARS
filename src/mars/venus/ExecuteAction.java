/*
 * MIT License
 * 
 * Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar
 * Developed by Pete Sanderson (psanderson@otterbein.edu)
 * and Kenneth Vollmar (kenvollmar@missouristate.edu)
 * 
 * Copyright (c) 2020 Andrea Proietto [substantial edits]
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mars.venus;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import mars.ErrorList;
import mars.ErrorMessage;
import mars.MIPSprogram;
import mars.Main;
import mars.ProcessingException;
import mars.assembler.staticassembler.StaticAssembler;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.mips.newhardware.MIPSMachine;
import mars.settings.BooleanSettings;
import mars.settings.StringSettings;
import mars.util.SystemIO;

/**
 * Action for the Run -> Reset menu item
 */
class ExecuteAction extends AbstractAction {

    // Threshold for adding filename to printed message of files being assembled.
    private static final int LINE_LENGTH_LIMIT = 60;
    private static ArrayList<MIPSprogram> MIPSprogramsToAssemble;
    private static boolean warningsAreErrors, extendedAssemblerEnabled;

    protected ExecuteAction() {
        super("Reset", new ImageIcon(GuiAction.class.getResource(Main.imagesPath + "Reset16.png")));

        putValue(LARGE_ICON_KEY, new ImageIcon(GuiAction.class.getResource(Main.imagesPath + "Reset22.png")));
        putValue(SHORT_DESCRIPTION, "Reset MIPS memory and registers");
        putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
    }

    public static ArrayList<MIPSprogram> getMIPSprogramsToAssemble() {
        return MIPSprogramsToAssemble;
    }

    // AP190718: Invoked by:
    // - execute" action
    // - delayed-branch toggling
    // - memory segmentation updates
    public static boolean assemble() {
        extendedAssemblerEnabled = BooleanSettings.EXTENDED_ASSEMBLER.isSet();
        warningsAreErrors = BooleanSettings.WARNINGS_ARE_ERRORS.isSet();

        // NOTE: Guaranteed to be non-null from action enable-handling
        EditPane leadingTab = Main.getGUI().editTabbedPane.getSelectedComponent();

        // Check if current file has unsaved edits (or is new) and attempt to save them if so
        // Return without assembling if file save is unsuccessful
        // AP190718: Show check shouldn't be necessary
        //if (Main.getGUI().editTabbedPane.isShowing()) {
            if ((leadingTab.isNew() || leadingTab.hasUnsavedEdits()) && !leadingTab.save(false))
                return false;
        //}
        
        // Clear the execution environment
        clearExecutePane();
        
        // Instantiate a new machine
        MIPSMachine machine = new MIPSMachine(MIPSMachine.getConfigByName(StringSettings.MEMORY_CONFIGURATION.get()));
        StaticAssembler.ExecutableProgram exec;
        
        // Start the assembling process
        try {
            ErrorList warnings = new ErrorList();
            
            exec = StaticAssembler.beginAssembling(leadingTab.getPath(), machine, extendedAssemblerEnabled, warningsAreErrors, warnings);
            if (warnings.warningsOccurred())
                Main.getGUI().messagesPane.postMarsMessage(warnings.generateWarningReport());
            
        }
        catch (ProcessingException pe) {
            String errorReport = pe.errors().generateErrorAndWarningReport();
            Main.getGUI().messagesPane.postMarsMessage(errorReport);
            Main.getGUI().messagesPane.postMarsMessage("Assemble operation failed.\n\n");
            for (ErrorMessage em : pe.errors().getErrorMessages()) {
                if (em.getLine() == 0 && em.getPosition() == 0) continue;
                if (!em.isWarning() || warningsAreErrors) {
                    Main.getGUI().messagesPane.selectErrorMessage(em.getFilename(), em.getLine(), em.getPosition());
                    if (Main.getGUI().editTabbedPane.isShowing())
                        Main.getGUI().editTabbedPane.selectEditorTextLine(em.getFilename(), em.getLine(), em.getPosition());
                    break;
                }
            }
            return false;
        }

        // If we're here, then assembling has been succesful
        Main.getGUI().messagesPane.postMarsMessage("Assemble operation completed successfully.\n\n");
        
        // Change interface state
        Main.getGUI().setMenuStateRunnable();
        
        // LEGACY
//        RegisterFile.resetRegisters();
//        Coprocessor1.resetRegisters();
//        Coprocessor0.resetRegisters();


        Main.getGUI().textSegment.setupTable();
        Main.getGUI().dataSegment.setupTable();
        Main.getGUI().dataSegment.highlightCellForAddress(Memory.dataBaseAddress);
        Main.getGUI().dataSegment.clearHighlighting();
        Main.getGUI().labelValues.setupTable();
        Main.getGUI().textSegment.setCodeHighlighting(true);
        Main.getGUI().textSegment.highlightStepAtPC();
        Main.getGUI().registersTab.clearWindow();
        Main.getGUI().coprocessor1Tab.clearWindow();
        Main.getGUI().coprocessor0Tab.clearWindow();
        VenusUI.setReset(true);
        VenusUI.setStarted(false);
        SystemIO.resetFiles();

        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        RunGoAction.resetMaxSteps();
        // The difficult part here is resetting the data segment.  Two approaches are:
        // 1. After each assembly, get a deep copy of the Globals.memory array 
        //    containing data segment.  Then replace it upon reset.
        // 2. Simply re-assemble the program upon reset, and the assembler will 
        //    build a new data segment.  Reset can only be done after a successful
        //    assembly, so there is "no" chance of assembler error.
        // I am choosing the second approach although it will slow down the reset
        // operation.  The first approach requires additional Memory class methods.
        try {
            Main.program.assemble(MIPSprogramsToAssemble,
                    extendedAssemblerEnabled,
                    warningsAreErrors);
        }
        catch (ProcessingException pe) {
            Main.getGUI().messagesPane.postMarsMessage(
                    //pe.errors().generateErrorReport());
                    "Unable to reset.  Please close file then re-open and re-assemble.\n");
            return;
        }
        RegisterFile.resetRegisters();
        Coprocessor1.resetRegisters();
        Coprocessor0.resetRegisters();

        Main.getGUI().registersTab.clearHighlighting();
        Main.getGUI().registersTab.updateRegisters();
        Main.getGUI().coprocessor1Tab.clearHighlighting();
        Main.getGUI().coprocessor1Tab.updateRegisters();
        Main.getGUI().coprocessor0Tab.clearHighlighting();
        Main.getGUI().coprocessor0Tab.updateRegisters();
        Main.getGUI().dataSegment.highlightCellForAddress(Memory.dataBaseAddress);
        Main.getGUI().dataSegment.clearHighlighting();
        Main.getGUI().textSegment.resetModifiedSourceCode();
        Main.getGUI().textSegment.setCodeHighlighting(true);
        Main.getGUI().textSegment.highlightStepAtPC();
        Main.getGUI().registersPane.setSelectedComponent(Main.getGUI().registersTab);
        Main.getGUI().setMenuStateRunnable();
        VenusUI.setReset(true);
        VenusUI.setStarted(false);

        // Aug. 24, 2005 Ken Vollmar
        SystemIO.resetFiles();  // Ensure that I/O "file descriptors" are initialized for a new program run

        Main.getGUI().messagesPane.postRunMessage("\nReset completed.\n\n");
    }

    /**
     * Clears out all components of the Execute tab: text segment display, data
     * segment display, label display and register display. This will typically
     * be done upon File->Close, Open, New.
     */
    public static void clearExecutePane() {
        Main.getGUI().textSegment.clearWindow();
        Main.getGUI().dataSegment.clearWindow();
        Main.getGUI().labelValues.clearWindow();
        Main.getGUI().registersTab.clearWindow();
        Main.getGUI().coprocessor1Tab.clearWindow();
        Main.getGUI().coprocessor0Tab.clearWindow();
    }
}
