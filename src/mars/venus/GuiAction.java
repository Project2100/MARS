package mars.venus;

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.function.BiConsumer;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;
import mars.Main;
import mars.Settings;
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
        Main.getGUI().editTabbedPane.getSelectedComponent().save(false);
    }

    /**
     * Perform "save as" operation on current tab's file.
     */
    void saveAs(ActionEvent event) {
        Main.getGUI().editTabbedPane.getSelectedComponent().save(true);
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

    void print(ActionEvent event) {
        Main.getGUI().editTabbedPane.getSelectedComponent().print();
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
        Main.getGUI().editTabbedPane.getSelectedComponent().undo();
    }

    void redo(ActionEvent event) {
        Main.getGUI().editTabbedPane.getSelectedComponent().redo();
    }

    void selectAll(ActionEvent event) {
        Main.getGUI().editTabbedPane.getSelectedComponent().selectAllText();
    }

    void findAndReplace(ActionEvent event) {
        new FindReplaceDialog().setVisible(true);
    }

    void dumpMemory(ActionEvent e) {
        new DumpMemoryDialog().setVisible(true);
    }

    void toggleBreakpoints(ActionEvent event) {
        Main.getGUI().executePane.getTextSegmentWindow().toggleBreakpoints();
    }

    void toggleWarningsAreErrors(ActionEvent e) {
        Main.getSettings().setBool(Settings.WARNINGS_ARE_ERRORS, ((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void togglePopupInput(ActionEvent e) {
        Main.getSettings().setBool(Settings.POPUP_SYSCALL_INPUT, ((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleProgramArguments(ActionEvent e) {
        boolean selected = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Main.getSettings().setBool(Settings.PROGRAM_ARGUMENTS, selected);
        if (selected)
            Main.getGUI().executePane.getTextSegmentWindow().addProgramArgumentsPanel();
        else
            Main.getGUI().executePane.getTextSegmentWindow().removeProgramArgumentsPanel();
    }

    void toggleSelfModifyingCode(ActionEvent e) {
        Main.getSettings().setBool(Settings.SELF_MODIFYING_CODE_ENABLED, ((JCheckBoxMenuItem) e.getSource()).isSelected());
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
        Main.getGUI().executePane.labelValues.setVisible(visibility);
        Main.getSettings().setBool(Settings.LABEL_WINDOW_VISIBILITY, visibility);
    }

    void toggleStartAtMain(ActionEvent e) {
        Main.getSettings().setBool(Settings.START_AT_MAIN, ((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleDelayedBranching(ActionEvent e) {
        Main.getSettings().setBool(Settings.DELAYED_BRANCHING_ENABLED,
                ((JCheckBoxMenuItem) e.getSource()).isSelected());
        // 25 June 2007 Re-assemble if the situation demands it to maintain consistency.
        if (Main.getGUI().executePane.isShowing()) {
            // Stop execution if executing -- should NEVER happen because this 
            // Action's widget is disabled during MIPS execution.
            if (VenusUI.getStarted())
                Simulator.getInstance().stopExecution(this);
            ExecuteAction.assemble();
        }
    }

    void toggleAssembleOnOpen(ActionEvent e) {
        Main.getSettings().setBool(Settings.ASSEMBLE_ON_OPEN_ENABLED, ((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleAssembleAll(ActionEvent e) {
        Main.getSettings().setBool(Settings.ASSEMBLE_ALL_ENABLED, ((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleValueDisplayBase(ActionEvent e) {
        boolean isHex = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Main.getGUI().executePane.getValueDisplayBaseChooser().setSelected(isHex);
        Main.getSettings().setBool(Settings.DISPLAY_VALUES_IN_HEX, isHex);
    }

    void toggleAddressDisplayBase(ActionEvent e) {
        boolean isHex = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Main.getGUI().executePane.getAddressDisplayBaseChooser().setSelected(isHex);
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
