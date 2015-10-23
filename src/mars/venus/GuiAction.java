package mars.venus;

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.function.BiConsumer;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;
import mars.Main;
import mars.settings.BooleanSettings;
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

    private final BiConsumer<GuiAction, ActionEvent> delegate;

    protected GuiAction(String name, URL icon, URL largeIcon, String description,
            Integer mnemonic, KeyStroke accel, BiConsumer<GuiAction, ActionEvent> act) {

        super(name, icon == null ? null : new ImageIcon(icon));

        putValue(SHORT_DESCRIPTION, description);
        putValue(LARGE_ICON_KEY, largeIcon == null ? null : new ImageIcon(largeIcon));
        putValue(MNEMONIC_KEY, mnemonic);
        putValue(ACCELERATOR_KEY, accel);

        delegate = act;
    }

    protected GuiAction(String name, String description, Integer mnemonic,
            KeyStroke accel, BiConsumer<GuiAction, ActionEvent> act) {
        this(name, null, null, description, mnemonic, accel, act);
    }

    protected GuiAction(String name, String description,
            BiConsumer<GuiAction, ActionEvent> act) {
        this(name, null, null, description, null, null, act);
    }
    
    /**
     * Inherited from {@code AbstractAction}; default implementation invokes the
     * {@link BiConsumer} passed at construction time.
     *
     * @param event the event dispatched by the EDT
     * @see AbstractAction#actionPerformed(ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        delegate.accept(this, event);
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
     * Perform "save" operation on current file.
     */
    void save(ActionEvent event) {
        Main.getGUI().editTabbedPane.getSelectedComponent().save(false);
    }

    /**
     * Perform "save as" operation on current file.
     */
    void saveAs(ActionEvent event) {
        Main.getGUI().editTabbedPane.getSelectedComponent().save(true);
    }

    /**
     * Perform save operation on all open files.
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
        Main.getGUI().textSegment.toggleBreakpoints();
    }

    void toggleWarningsAreErrors(ActionEvent e) {
        BooleanSettings.WARNINGS_ARE_ERRORS.set(((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void togglePopupInput(ActionEvent e) {
        BooleanSettings.POPUP_SYSCALL_INPUT.set(((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleProgramArguments(ActionEvent e) {
        boolean selected = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        BooleanSettings.PROGRAM_ARGUMENTS.set(selected);
        if (selected)
            Main.getGUI().textSegment.addProgramArgumentsPanel();
        else
            Main.getGUI().textSegment.removeProgramArgumentsPanel();
    }

    void toggleSelfModifyingCode(ActionEvent e) {
        BooleanSettings.SELF_MODIFYING_CODE.set(((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleExtendedInstructionSet(ActionEvent e) {
        BooleanSettings.EXTENDED_ASSEMBLER.set(((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    /**
     * Show or hide the label window (symbol table). If visible, it is displayed
     * to the right of the text segment and the latter is shrunk accordingly.
     */
    void toggleLabelWindow(ActionEvent e) {
        boolean visibility = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Main.getGUI().labelValues.setVisible(visibility);
        BooleanSettings.LABEL_WINDOW_VISIBILITY.set(visibility);
    }

    void toggleStartAtMain(ActionEvent e) {
        BooleanSettings.START_AT_MAIN.set(((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleDelayedBranching(ActionEvent e) {
        BooleanSettings.DELAYED_BRANCHING.set(
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
        BooleanSettings.ASSEMBLE_ON_OPEN.set(((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleAssembleAll(ActionEvent e) {
        BooleanSettings.ASSEMBLE_ALL.set(((JCheckBoxMenuItem) e.getSource()).isSelected());
    }

    void toggleValueDisplayBase(ActionEvent e) {
        boolean isHex = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Main.getGUI().dataSegment.getValueDisplayBaseChooser().setSelected(isHex);
        BooleanSettings.DISPLAY_VALUES_IN_HEX.set(isHex);
    }

    void toggleAddressDisplayBase(ActionEvent e) {
        boolean isHex = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        Main.getGUI().dataSegment.getAddressDisplayBaseChooser().setSelected(isHex);
        BooleanSettings.DISPLAY_ADDRESSES_IN_HEX.set(isHex);
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
        HelpDialog.showDialog();
    }
}
