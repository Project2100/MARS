package mars.venus;

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import mars.Main;
import mars.Settings;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.simulator.Simulator;
import mars.util.Binary;
import mars.util.MemoryDump;

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
 * Parent class for Action subclasses to be defined for every menu/toolbar
 * option.
 */
class GuiAction extends AbstractAction {

    VenusUI mainUI;
    private BiConsumer<GuiAction, ActionEvent> delegate;

    protected GuiAction(String name, Icon icon, String descrip,
            Integer mnemonic, KeyStroke accel, VenusUI gui) {
        this(name, icon, descrip, mnemonic, accel, (BiConsumer<GuiAction, ActionEvent>) null);
        mainUI = gui;
    }

    protected GuiAction(String name, Icon icon, String descrip,
            Integer mnemonic, KeyStroke accel, BiConsumer<GuiAction, ActionEvent> c) {
        super(name, icon);
        putValue(SHORT_DESCRIPTION, descrip);
        if (mnemonic != null)
            putValue(MNEMONIC_KEY, mnemonic);
        if (accel != null)
            putValue(ACCELERATOR_KEY, accel);
        delegate = c;
    }

    /**
     * ActionListener's actionPerformed(). @see java.awt.event.ActionListener
     *
     * @param e the event dispatched by the EDT
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        delegate.accept(this, e);
    }

    /////////////////////////////////////////////////////////////////
    // Action functions
    /////////////////////////////////////////////////////////////////
    /**
     * Perform "new" operation to create an empty tab.
     */
    void newFile(ActionEvent event) {
        Main.getGUI().editTabbedPane.newFile();
    }

    /**
     * Launch a file chooser for name of file to open.
     */
    void open(ActionEvent event) {
        Main.getGUI().editTabbedPane.openFile();
    }

    /**
     * Perform "save" operation on current tab's file.
     */
    void save(ActionEvent event) {
        Main.getGUI().editTabbedPane.saveCurrentFile();
    }

    /**
     * Perform "save as" operation on current tab's file.
     */
    void saveAs(ActionEvent event) {
        Main.getGUI().editTabbedPane.saveAsCurrentFile();
    }

    /**
     * Perform save operation on all open files (tabs).
     */
    void saveAll(ActionEvent event) {
        Main.getGUI().editTabbedPane.saveAllFiles();
    }

    /**
     * Perform "close" operation on current tab's file.
     */
    void close(ActionEvent event) {
        Main.getGUI().editTabbedPane.closeCurrentFile();
    }

    /**
     * Close all currently open files.
     */
    void closeAll(ActionEvent event) {
        Main.getGUI().editTabbedPane.closeAllFiles();
    }

    /**
     * Exit MARS, unless one or more files have unsaved edits and user cancels.
     */
    void exit(ActionEvent event) {
        // Crafting a WindowClosing event, will call mainFrame's WindowListener
        Main.getGUI().mainFrame.dispatchEvent(new WindowEvent(Main.getGUI().mainFrame, WindowEvent.WINDOW_CLOSING));
    }

    /**
     * Uses the HardcopyWriter class developed by David Flanagan for the book
     * "Java Examples in a Nutshell". It will do basic printing of multi-page
     * text documents. It displays a print dialog but does not act on any
     * changes the user may have specified there, such as number of copies.
     */
    void print(ActionEvent event) {
        EditTab tab = Main.getGUI().editTabbedPane.getSelectedComponent();
        if (tab == null) return;
        int fontsize = 10;  // fixed at 10 point
        double margins = .5; // all margins (left,right,top,bottom) fixed at .5"

        int lineNumberDigits = Integer.toString(tab.getSourceLineCount()).length();
        String line;
        String lineNumberString = "";
        int lineNumber = 0;

        try (BufferedReader in = new BufferedReader(new StringReader(tab.getSource()));
                HardcopyWriter printer = new HardcopyWriter(Main.getGUI().mainFrame, tab.getFilename(),
                        fontsize, margins, margins, margins, margins)) {

//            Iterator<String> lineNumbers = Stream.iterate(1, i -> i + 1)
//                    .map(i->Integer.toString(i)+": ")
//                    .limit(tab.getSourceLineCount()).iterator();
//
//            Arrays.stream(tab.getSource().split(System.lineSeparator(), -1))
//                    .map(lin -> lineNumbers.next() + lin + System.lineSeparator())
//                    .forEach(lin -> printer.write(lin.toCharArray(), 0, lin.length()));
            line = in.readLine();
            while (line != null) {
                if (tab.showingLineNumbers()) {
                    lineNumber++;
                    lineNumberString = Integer.toString(lineNumber) + ": ";
                    while (lineNumberString.length() < lineNumberDigits)
                        lineNumberString = lineNumberString + " ";
                }
                line = lineNumberString + line + "\n";
                printer.write(line.toCharArray(), 0, line.length());
                line = in.readLine();
            }
        }
        catch (HardcopyWriter.PrintCanceledException ex) {
            // TODO
        }
        catch (IOException ex) {
            Main.logger.log(Level.WARNING, "Exception while printing file " + tab.getFilename(), ex);
        }
    }

    void cut(ActionEvent event) {
        Main.getGUI().editTabbedPane.getSelectedComponent().cutText();
    }

    void copy(ActionEvent event) {
        Main.getGUI().editTabbedPane.getSelectedComponent().copyText();
    }

    void paste(ActionEvent event) {
        Main.getGUI().editTabbedPane.getSelectedComponent().pasteText();
    }

    /**
     * Undo/Redo are adapted from TextComponentDemo.java in the Java Tutorial
     * "Text Component Features"
     */
    void undo(ActionEvent event) {
        EditTab tab = Main.getGUI().editTabbedPane.getSelectedComponent();
        if (tab != null) tab.undo();
    }

    void redo(ActionEvent event) {
        EditTab tab = Main.getGUI().editTabbedPane.getSelectedComponent();
        if (tab != null) tab.redo();
    }

    void selectAll(ActionEvent event) {
        Main.getGUI().editTabbedPane.getSelectedComponent().selectAllText();
    }

    void findAndReplace(ActionEvent event) {
        new FindReplaceDialog().setVisible(true);
    }

    void dumpMemory(ActionEvent e) {

        String[] segments = MemoryDump.getSegmentNames();

        String[] segNames = new String[segments.length];
        int[] segBaseAddresses = new int[segments.length];
        int[] segHighAddresses = new int[segments.length];

        // Calculate the actual highest address to be dumped.  For text segment, this depends on the
        // program length (number of machine code instructions).  For data segment, this depends on
        // how many MARS 4K word blocks have been referenced during assembly and/or execution.
        // Then generate label from concatentation of segmentArray[i], baseAddressArray[i]
        // and highAddressArray[i].  This lets user know exactly what range will be dumped.  Initially not
        // editable but maybe add this later.
        // If there is nothing to dump (e.g. address of first null == base address), then
        // the segment will not be listed.
        int segmentCount = 0;
        for (String segmentName : segments) {
            Integer[] bounds = MemoryDump.getSegmentBounds(segmentName);
            int upperBound;
            try {
                upperBound = Main.memory.getAddressOfFirstNull(bounds[0], bounds[1]);
            }
            catch (AddressErrorException aee) {
                upperBound = bounds[0];
            }
            upperBound -= Memory.WORD_LENGTH_BYTES;

            if (upperBound >= bounds[0]) {
                segBaseAddresses[segmentCount] = bounds[0];
                segHighAddresses[segmentCount] = upperBound;
                segNames[segmentCount] = segmentName + " (" + Binary.intToHexString(bounds[0]) + " - " + Binary.intToHexString(upperBound) + ")";
                segmentCount++;
            }
        }
        if (segmentCount == 0) {
            JOptionPane.showMessageDialog(Main.getGUI().mainFrame, "There is nothing to dump!", "MARS", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (segmentCount < segNames.length) {
            String[] tempArray = new String[segmentCount];
            System.arraycopy(segNames, 0, tempArray, 0, segmentCount);
            segNames = tempArray;
        }

        new DumpMemoryDialog(segNames, segBaseAddresses, segHighAddresses).setVisible(true);
    }

    void toggleBreakpoints(ActionEvent event) {
        Main.getGUI().executeTab.getTextSegmentWindow().toggleBreakpoints();
    }

    void toggleWarningsAreErrors(ActionEvent e) {
        Main.getSettings().setBool(Settings.WARNINGS_ARE_ERRORS, ((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void togglePopupInput(ActionEvent e) {
        boolean usePopup = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Main.getSettings().setBool(Settings.POPUP_SYSCALL_INPUT, usePopup);
    }

    void toggleProgramArguments(ActionEvent e) {
        boolean selected = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Main.getSettings().setBool(Settings.PROGRAM_ARGUMENTS, selected);
        if (selected)
            Main.getGUI().executeTab.getTextSegmentWindow().addProgramArgumentsPanel();
        else
            Main.getGUI().executeTab.getTextSegmentWindow().removeProgramArgumentsPanel();
    }

    void toggleSelfModifyingCode(ActionEvent e) {
        Main.getSettings().setBool(Settings.SELF_MODIFYING_CODE_ENABLED,
                ((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleExtendedInstructionSet(ActionEvent e) {
        Main.getSettings().setBool(Settings.EXTENDED_ASSEMBLER_ENABLED, ((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    /**
     * Show or hide the label window (symbol table). If visible, it is displayed
     * to the right of the text segment and the latter is shrunk accordingly.
     */
    void toggleLabelWindow(ActionEvent e) {
        boolean visibility = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Main.getGUI().executeTab.labelValues.setVisible(visibility);
        Main.getSettings().setBool(Settings.LABEL_WINDOW_VISIBILITY, visibility);
    }

    void toggleStartAtMain(ActionEvent e) {
        Main.getSettings().setBool(Settings.START_AT_MAIN, ((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleDelayedBranching(ActionEvent e) {
        Main.getSettings().setBool(Settings.DELAYED_BRANCHING_ENABLED,
                ((JCheckBoxMenuItem) e.getSource()).isSelected());
        // 25 June 2007 Re-assemble if the situation demands it to maintain consistency.
        if (Main.getGUI() != null
                && (VenusUI.getStatus() == VenusUI.RUNNABLE
                || VenusUI.getStatus() == VenusUI.RUNNING
                || VenusUI.getStatus() == VenusUI.TERMINATED)) {
            // Stop execution if executing -- should NEVER happen because this 
            // Action's widget is disabled during MIPS execution.
            if (VenusUI.getStatus() == VenusUI.RUNNING)
                Simulator.getInstance().stopExecution(this);
            ExecuteAction.assemble(false);
        }
    }

    void toggleAssembleOnOpen(ActionEvent e) {
        Main.getSettings().setBool(Settings.ASSEMBLE_ON_OPEN_ENABLED,
                ((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleAssembleAll(ActionEvent e) {
        Main.getSettings().setBool(Settings.ASSEMBLE_ALL_ENABLED,
                ((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleValueDisplayBase(ActionEvent e) {
        boolean isHex = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Main.getGUI().executeTab.getValueDisplayBaseChooser().setSelected(isHex);
        Main.getSettings().setBool(Settings.DISPLAY_VALUES_IN_HEX, isHex);
    }

    void toggleAddressDisplayBase(ActionEvent e) {
        boolean isHex = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Main.getGUI().executeTab.getAddressDisplayBaseChooser().setSelected(isHex);
        Main.getSettings().setBool(Settings.DISPLAY_ADDRESSES_IN_HEX, isHex);
    }

    void editorSettings(ActionEvent event) {
        new EditorFontDialog().setVisible(true);
    }

    void highlightingSettings(ActionEvent event) {
        new HighlightingDialog().setVisible(true);
    }

    void exceptionHandlerSettings(ActionEvent event) {
        new ExceptionHandlerDialog().setVisible(true);
    }
    
    void memoryConfigurationSettings(ActionEvent e) {
        new MemoryConfigurationDialog(this).setVisible(true);
    }

    void help(ActionEvent event) {
        new HelpDialog().setVisible(true);
    }
}
