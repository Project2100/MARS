package mars.venus;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import mars.ErrorList;
import mars.ErrorMessage;
import mars.MIPSprogram;
import mars.Main;
import mars.ProcessingException;
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
    private static ArrayList MIPSprogramsToAssemble;
    private static boolean extendedAssemblerEnabled;
    
    
    protected ExecuteAction(String name, Icon icon, String descrip,
            Integer mnemonic, KeyStroke accel, VenusUI gui) {
        super(name, icon, descrip, mnemonic, accel, gui);
    }

    public static ArrayList getMIPSprogramsToAssemble() {
        return MIPSprogramsToAssemble;
    }

    public static boolean assemble(boolean isInEditMode) {
        String name = "Assemble";
        ExecutePane executePane = Main.getGUI().executeTab;
        extendedAssemblerEnabled = Main.getSettings().getExtendedAssemblerEnabled();
        warningsAreErrors = Main.getSettings().getWarningsAreErrors();
        if (VenusUI.getFile() == null) return false;
        if (VenusUI.getStatus() == VenusUI.EDITED)
            Main.getGUI().editTabbedPane.saveCurrentFile();
        try {
            Main.program = new MIPSprogram();
            ArrayList filesToAssemble;
            if (Main.getSettings().getAssembleAllEnabled())
                filesToAssemble = FilenameFinder.getFilenameList(new File(VenusUI.getName()).getParent(), Main.fileExtensions);
            else {
                filesToAssemble = new ArrayList();
                filesToAssemble.add(VenusUI.getName());
            }
            String exceptionHandler = null;
            if (Main.getSettings().getExceptionHandlerEnabled() && Main.getSettings().getExceptionHandler() != null && Main.getSettings().getExceptionHandler().length() > 0)
                exceptionHandler = Main.getSettings().getExceptionHandler();
            MIPSprogramsToAssemble = Main.program.prepareFilesForAssembly(filesToAssemble, VenusUI.getFile().getPath(), exceptionHandler);
            Main.getGUI().messagesPane.postMarsMessage(buildFileNameList(name + ": assembling ", MIPSprogramsToAssemble));
            ErrorList warnings = Main.program.assemble(MIPSprogramsToAssemble, extendedAssemblerEnabled, warningsAreErrors);
            if (warnings.warningsOccurred())
                Main.getGUI().messagesPane.postMarsMessage(warnings.generateWarningReport());
            Main.getGUI().messagesPane.postMarsMessage(name + ": operation completed successfully.\n\n");
            VenusUI.setAssembled(true);
            VenusUI.setStatus(VenusUI.RUNNABLE);
            RegisterFile.resetRegisters();
            Coprocessor1.resetRegisters();
            Coprocessor0.resetRegisters();
            executePane.getTextSegmentWindow().setupTable();
            executePane.getDataSegmentWindow().setupTable();
            executePane.getDataSegmentWindow().highlightCellForAddress(Memory.dataBaseAddress);
            executePane.getDataSegmentWindow().clearHighlighting();
            executePane.getLabelsWindow().setupTable();
            executePane.getTextSegmentWindow().setCodeHighlighting(true);
            executePane.getTextSegmentWindow().highlightStepAtPC();
            Main.getGUI().registersTab.clearWindow();
            Main.getGUI().coprocessor1Tab.clearWindow();
            Main.getGUI().coprocessor0Tab.clearWindow();
            VenusUI.setReset(true);
            VenusUI.setStarted(false);
            SystemIO.resetFiles();
        }
        catch (ProcessingException pe) {
            String errorReport = pe.errors().generateErrorAndWarningReport();
            Main.getGUI().messagesPane.postMarsMessage(errorReport);
            Main.getGUI().messagesPane.postMarsMessage(name + ": operation completed with errors.\n\n");
            ArrayList errorMessages = pe.errors().getErrorMessages();
            for (Object errorMessage : errorMessages) {
                ErrorMessage em = (ErrorMessage) errorMessage;
                if (em.getLine() == 0 && em.getPosition() == 0) continue;
                if (!em.isWarning() || warningsAreErrors) {
                    (Main.getGUI().messagesPane).selectErrorMessage(em.getFilename(), em.getLine(), em.getPosition());
                    if (isInEditMode)
                        (Main.getGUI().messagesPane).selectEditorTextLine(em.getFilename(), em.getLine(), em.getPosition());
                    break;
                }
            }
            VenusUI.setAssembled(false);
            VenusUI.setStatus(VenusUI.NOT_EDITED);
            return false;
        }
        return true;
    }

    // Handy little utility for building comma-separated list of filenames
    // while not letting line length getStatus out of hand.
    private static String buildFileNameList(String preamble, ArrayList programList) {
        String result = preamble;
        int lineLength = result.length();
        for (int i = 0; i < programList.size(); i++) {
            String filename = ((MIPSprogram) programList.get(i)).getFilename();
            result += filename + ((i < programList.size() - 1) ? ", " : "");
            lineLength += filename.length();
            if (lineLength > LINE_LENGTH_LIMIT) {
                result += "\n";
                lineLength = 0;
            }
        }
        return result + ((lineLength == 0) ? "" : "\n") + "\n";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        RunGoAction.resetMaxSteps();
        String name = this.getValue(Action.NAME).toString();
        ExecutePane executePane = mainUI.executeTab;
         // The difficult part here is resetting the data segment.  Two approaches are:
        // 1. After each assembly, getStatus a deep copy of the Globals.memory array 
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
            (mainUI.messagesPane).postMarsMessage(
                    //pe.errors().generateErrorReport());
                    "Unable to reset.  Please close file then re-open and re-assemble.\n");
            return;
        }
        RegisterFile.resetRegisters();
        Coprocessor1.resetRegisters();
        Coprocessor0.resetRegisters();

        (Main.getGUI().registersTab).clearHighlighting();
        (Main.getGUI().registersTab).updateRegisters();
        (Main.getGUI().coprocessor1Tab).clearHighlighting();
        (Main.getGUI().coprocessor1Tab).updateRegisters();
        (Main.getGUI().coprocessor0Tab).clearHighlighting();
        (Main.getGUI().coprocessor0Tab).updateRegisters();
        executePane.getDataSegmentWindow().highlightCellForAddress(Memory.dataBaseAddress);
        executePane.getDataSegmentWindow().clearHighlighting();
        executePane.getTextSegmentWindow().resetModifiedSourceCode();
        executePane.getTextSegmentWindow().setCodeHighlighting(true);
        executePane.getTextSegmentWindow().highlightStepAtPC();
        (mainUI.registersPane).setSelectedComponent(Main.getGUI().registersTab);
        VenusUI.setStatus(VenusUI.RUNNABLE);
        VenusUI.setReset(true);
        VenusUI.setStarted(false);

        // Aug. 24, 2005 Ken Vollmar
        SystemIO.resetFiles();  // Ensure that I/O "file descriptors" are initialized for a new program run

        (mainUI.messagesPane).postRunMessage("\n" + name + ": reset completed.\n\n");
    }
}
