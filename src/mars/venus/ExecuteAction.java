package mars.venus;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import mars.ErrorList;
import mars.ErrorMessage;
import mars.MIPSprogram;
import mars.Main;
import mars.ProcessingException;
import mars.Settings;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.util.FilenameFinder;
import mars.util.SystemIO;

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
 * Action for the Run -> Reset menu item
 */
class ExecuteAction extends GuiAction {

    private static boolean warningsAreErrors;
    // Threshold for adding filename to printed message of files being assembled.
    private static final int LINE_LENGTH_LIMIT = 60;
    private static ArrayList<MIPSprogram> MIPSprogramsToAssemble;
    private static boolean extendedAssemblerEnabled;

    protected ExecuteAction(String name, Icon icon, String descrip,
            Integer mnemonic, KeyStroke accel, VenusUI gui) {
        super(name, icon, descrip, mnemonic, accel, gui);
    }

    public static ArrayList<MIPSprogram> getMIPSprogramsToAssemble() {
        return MIPSprogramsToAssemble;
    }

    public static boolean assemble() {
        String name = "Assemble";
        extendedAssemblerEnabled = Settings.BooleanSettings.EXTENDED_ASSEMBLER.isSet();
        warningsAreErrors = Settings.BooleanSettings.WARNINGS_ARE_ERRORS.isSet();

        EditPane current = Main.getGUI().editTabbedPane.getSelectedComponent();

        // return if file isn't saved
        if (Main.getGUI().editTabbedPane.isShowing()) {
            Main.getGUI().executePane.clearPane();
            if (current == null || (current.isNew() || current.hasUnsavedEdits()) && !current.save(false))
                return false;
        }

        Main.program = new MIPSprogram();
        ArrayList<String> filesToAssemble;
        if (Settings.BooleanSettings.ASSEMBLE_ALL.isSet())
            filesToAssemble = FilenameFinder.getFilenameList(current.getParentDirectory(), Main.fileExtensions);
        else
            (filesToAssemble = new ArrayList<>()).add(current.getPathname());
        String exceptionHandler = null;
        if (Settings.BooleanSettings.EXCEPTION_HANDLER.isSet()
                && Settings.StringSettings.EXCEPTION_HANDLER_FILE.get() != null
                && Settings.StringSettings.EXCEPTION_HANDLER_FILE.get().length() > 0)
            exceptionHandler = Settings.StringSettings.EXCEPTION_HANDLER_FILE.get();

        try {
            MIPSprogramsToAssemble = Main.program.prepareFilesForAssembly(filesToAssemble, current.getPathname(), exceptionHandler);
            Main.getGUI().messagesPane.postMarsMessage("Assembling files: "
                    + MIPSprogramsToAssemble.stream().map(s -> s.getFilename()).reduce((s, t) -> {
                        int lowerBound = s.lastIndexOf('\n');

                        return s + (s.substring(lowerBound == -1 ? 0 : lowerBound).length() >= LINE_LENGTH_LIMIT
                                ? ",\n"
                                : ", ") + t;
                    }).get() + "\n\n");

            ErrorList warnings = Main.program.assemble(MIPSprogramsToAssemble, extendedAssemblerEnabled, warningsAreErrors);

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
                        Main.getGUI().messagesPane.selectEditorTextLine(em.getFilename(), em.getLine(), em.getPosition());
                    break;
                }
            }
            return false;
        }

        Main.getGUI().messagesPane.postMarsMessage("Assemble operation completed successfully.\n\n");
        Main.getGUI().setMenuStateRunnable();
        RegisterFile.resetRegisters();
        Coprocessor1.resetRegisters();
        Coprocessor0.resetRegisters();
        Main.getGUI().executePane.getTextSegmentWindow().setupTable();
        Main.getGUI().executePane.getDataSegmentWindow().setupTable();
        Main.getGUI().executePane.getDataSegmentWindow().highlightCellForAddress(Memory.dataBaseAddress);
        Main.getGUI().executePane.getDataSegmentWindow().clearHighlighting();
        Main.getGUI().executePane.getLabelsWindow().setupTable();
        Main.getGUI().executePane.getTextSegmentWindow().setCodeHighlighting(true);
        Main.getGUI().executePane.getTextSegmentWindow().highlightStepAtPC();
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
        ExecutePane executePane = Main.getGUI().executePane;
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
        executePane.getDataSegmentWindow().highlightCellForAddress(Memory.dataBaseAddress);
        executePane.getDataSegmentWindow().clearHighlighting();
        executePane.getTextSegmentWindow().resetModifiedSourceCode();
        executePane.getTextSegmentWindow().setCodeHighlighting(true);
        executePane.getTextSegmentWindow().highlightStepAtPC();
        Main.getGUI().registersPane.setSelectedComponent(Main.getGUI().registersTab);
        Main.getGUI().setMenuStateRunnable();
        VenusUI.setReset(true);
        VenusUI.setStarted(false);

        // Aug. 24, 2005 Ken Vollmar
        SystemIO.resetFiles();  // Ensure that I/O "file descriptors" are initialized for a new program run

        Main.getGUI().messagesPane.postRunMessage("\nReset completed.\n\n");
    }
}
