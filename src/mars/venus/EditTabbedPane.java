package mars.venus;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;
import mars.Main;
import mars.Settings;
import mars.mips.hardware.RegisterFile;
import mars.util.FilenameFinder;

/*
 Copyright (c) 2003-2010,  Pete Sanderson and Kenneth Vollmar

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
 * Tabbed pane for the editor. Each of its tabs represents an open file.
 *
 * @author Sanderson
 */
public final class EditTabbedPane extends JTabbedPane {

    // number of times File->New has been selected.  Used to generate
    // default filename until first Save or Save As.
    private int newUsageCount;

    // Current Directory for Open operation, same for Save operation
    // Values will mainly be setStatusMenu by the EditTabbedPane as Open/Save operations occur.
    private final String defaultOpenDirectory, defaultSaveDirectory;
    private String currentOpenDirectory, currentSaveDirectory;

    private File mostRecentlyOpenedFile;
    private JFileChooser fileChooser;

    private int fileFilterCount;
    private ArrayList<FileFilter> fileFilters;
    private PropertyChangeListener listenForUserAddedFileFilter;

    /**
     * Constructor for the editor pane
     *
     */
    public EditTabbedPane() {
        super();

        newUsageCount = 0;
        defaultOpenDirectory = System.getProperty("user.dir");
        defaultSaveDirectory = System.getProperty("user.dir");
        currentOpenDirectory = defaultOpenDirectory;
        currentSaveDirectory = defaultSaveDirectory;

        mostRecentlyOpenedFile = null;
        fileChooser = new JFileChooser();

        //////////////////////////////////////////////////////////////////////////////////
        //  Private inner class for special property change listener.  DPS 9 July 2008.
        //  If user adds a file filter, e.g. by typing *.txt into the file text field then pressing
        //  Enter, then it is automatically added to the array of choosable file filters.  BUT, unless you
        //  Cancel out of the Open dialog, it is then REMOVED from the list automatically also. Here
        //  we will achieve a sort of persistence at least through the current activation of MARS.
        listenForUserAddedFileFilter = (event) -> {
            if (event.getPropertyName() == JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY) {
                FileFilter[] newFilters = (FileFilter[]) event.getNewValue();
                if (newFilters.length > fileFilters.size())
                    // new filter added, so add to end of master list.
                    fileFilters.add(newFilters[newFilters.length - 1]);
            }
        };

        fileChooser.addPropertyChangeListener(listenForUserAddedFileFilter);

        // Note: add sequence is significant - last one added becomes default.
        fileFilters = new ArrayList<>();
        fileFilters.add(FilenameFinder.getFileFilter(Main.fileExtensions, "Assembler Files"));
        fileFilters.add(FilenameFinder.getFileFilter(new ArrayList<>(Arrays.asList(".txt", ".in")), "Text/input Files"));
        fileFilters.add(fileChooser.getAcceptAllFileFilter());
        fileFilterCount = 0; // this will trigger fileChooser file filter load in next line
        setChoosableFileFilters();

        addChangeListener((event) -> {
            EditPane tab = getSelectedComponent();
            if (tab != null) {
                // New IF statement to permit free traversal of edit panes w/o invalidating
                // assembly if assemble-all is selected.  DPS 9-Aug-2011
                //20150520 - Modes implicitly invalid assembled status on swap
//                if (Main.getSettings().getBool(mars.Settings.ASSEMBLE_ALL))
//                    updateTitles(tab);
//                else {
//                tab.updateTitlesAndMenuState();
                Main.getGUI().updateGUIState();
//                    Main.getGUI().executePane.clearPane();
//                }
                tab.tellEditingComponentToRequestFocusInWindow();
            }
        });
    }

    /**
     * Gets the tab currently shown in this pane.
     * <p/>
     * Overridden for return type cast
     *
     * @return the current editor tab, or null if this container is empty
     * @see JTabbedPane#getSelectedComponent()
     */
    @Override
    public EditPane getSelectedComponent() {
        return (EditPane) super.getSelectedComponent();
    }

    /**
     * Gets the tab at the specified index.
     * <p/>
     * Overridden for return type cast
     *
     * @param index the position of the requested tab
     * @return the tab
     * @throws IndexOutOfBoundsException if {@code index} is out of bounds
     * @see JTabbedPane#getComponentAt(int index)
     */
    @Override
    public EditPane getComponentAt(int index) {
        return (EditPane) super.getComponentAt(index);
    }

    void setTitleAtComponent(String title, EditPane tab) {
        if (getTabComponentAt(indexOfComponent(tab)) == null)
            setTabComponentAt(indexOfComponent(tab), new TabTitleComponent(tab));
        else
            ((TabTitleComponent) getTabComponentAt(indexOfComponent(tab))).titleLabel.setText(title);
    }

    /**
     * Set name of current directory for Open operation. The contents of this
     * directory will be displayed when Open dialog is launched.
     *
     * @param currentOpenDirectory String containing pathname for current Open
     * directory. If it does not exist or is not a directory, the default (MARS
     * launch directory) will be used.
     */
    void setCurrentOpenDirectory(String currentOpenDirectory) {
        File file = new File(currentOpenDirectory);
        if (!file.exists() || !file.isDirectory())
            this.currentOpenDirectory = defaultOpenDirectory;
        else
            this.currentOpenDirectory = currentOpenDirectory;
    }

    /**
     * Get name of current directory for Save or Save As operation.
     *
     * @return String containing directory pathname. Returns null if there is no
     * EditTabbedPane. Returns default, directory MARS is launched from, if no
     * Save or Save As operations have been performed.
     */
    public String getCurrentSaveDirectory() {
        return currentSaveDirectory;
    }

    /**
     * Set name of current directory for Save operation. The contents of this
     * directory will be displayed when Save dialog is launched.
     *
     * @param currentSaveDirectory String containing pathname for current Save
     * directory. If it does not exist or is not a directory, the default (MARS
     * launch directory) will be used.
     */
    void setCurrentSaveDirectory(String currentSaveDirectory) {
        File file = new File(currentSaveDirectory);
        if (!file.exists() || !file.isDirectory())
            this.currentSaveDirectory = defaultSaveDirectory;
        else
            this.currentSaveDirectory = currentSaveDirectory;
    }

    /**
     * Carries out all necessary operations to implement the New operation from
     * the File menu.
     */
    public void newFile() {

        String name = "Untitled" + (++newUsageCount) + ".asm";
        EditPane tab = new EditPane(Paths.get(name));

        addTab(name, tab);

        VenusUI.setReset(true);
        RegisterFile.resetRegisters();
        tab.displayCaretPosition(0);
        setSelectedComponent(tab);
        tab.tellEditingComponentToRequestFocusInWindow();
    }

    /**
     * Carries out all necessary operations to implement the Open operation from
     * the File menu. This begins with an Open File dialog.
     *
     * @return true if file was opened, false otherwise.
     */
    public boolean openFile() {

        // The fileChooser's list may be rebuilt from the master ArrayList if a new filter
        // has been added by the user.
        setChoosableFileFilters();
        // getStatus name of file to be opened and load contents into text editing area.
        fileChooser.setCurrentDirectory(new File(currentOpenDirectory));
        // Set default to previous file opened, if any.  This is useful in conjunction
        // with option to assemble file automatically upon opening.  File likely to have
        // been edited externally (e.g. by Mipster).
        if (Settings.BooleanSettings.ASSEMBLE_ON_OPEN.isSet() && mostRecentlyOpenedFile != null)
            fileChooser.setSelectedFile(mostRecentlyOpenedFile);

        if (fileChooser.showOpenDialog(Main.getGUI().mainFrame) == JFileChooser.APPROVE_OPTION) {
            File theFile = fileChooser.getSelectedFile();
            setCurrentOpenDirectory(theFile.getParent());
            //setCurrentSaveDirectory(theFile.getParentDirectory());// 13-July-2011 DPS.
            if (!openFile(theFile))
                return false;

            // possibly send this file right through to the assembler by switching mode
            if (theFile.canRead() && Settings.BooleanSettings.ASSEMBLE_ON_OPEN.isSet())
                Main.getGUI().toggleGUIMode();
        }
        return true;

    }

    /**
     * Carries out all necessary operations to open the specified file in the
     * editor.
     *
     * @param file
     * @return true if file was opened, false otherwise.
     */
    public boolean openFile(File file) {

        try {
            file = file.getCanonicalFile();
        }
        catch (IOException ioe) {
            // nothing to do, theFile will keep current value
        }
        String currentFilePath = file.getPath();
        // If this file is currently already open, then simply select its tab
        EditPane tab = getTabForFile(currentFilePath);
        if (tab != null) {
            setSelectedComponent(tab);
            return false;
        }

        if (file.canRead()) {

            tab = new EditPane(file.toPath());

            addTab(tab.getFilename(), tab);

            setToolTipTextAt(indexOfComponent(tab), tab.getPathname());
            setSelectedComponent(tab);

            tab.tellEditingComponentToRequestFocusInWindow();
            mostRecentlyOpenedFile = file;
        }
        return true;
    }

    /**
     * If there is an EditPane for the given file pathname, return it else
     * return null.
     *
     * @param pathname Pathname for desired file
     * @return the EditPane for this file if it is open in the editor, or null
     * if not.
     */
    public EditPane getTabForFile(String pathname) {
        EditPane openPane = null;
        for (int i = 0; i < getTabCount(); i++) {
            EditPane pane = getComponentAt(i);
            if (pathname.equals(pane.getPathname())) {
                openPane = pane;
                break;
            }
        }
        return openPane;
    }

    /**
     * Saves all files currently open in the editor.
     *
     * @return true if operation succeeded otherwise false.
     */
    public boolean saveAllFiles() {
        int tabCount = getTabCount();
        if (tabCount == 0)
            throw new IllegalStateException("No tabs found on SaveAll action!");

        EditPane savedPane = getSelectedComponent();
        EditPane tab;
        for (int i = 0; i < tabCount; i++) {
            tab = getComponentAt(i);
            if (tab.hasUnsavedEdits()) {
                setSelectedComponent(tab);
                if (!tab.save(false))
                    return false;
            }

        }
        setSelectedComponent(savedPane);
        return true;
    }

    /**
     * Carries out all necessary operations to implement the Close operation
     * from the File menu. May return false, for instance when file has unsaved
     * changes and user selects Cancel from the warning dialog.
     *
     * @return true if file was closed, false otherwise.
     */
    public boolean closeCurrentFile() {
        EditPane tab = getSelectedComponent();
        if (tab != null && editsSavedOrAbandoned(tab)) {
            remove(tab);
            return true;
        }
        return false;
    }

    /**
     * Carries out all necessary operations to implement the Close All operation
     * from the File menu.
     *
     * @return true if files closed, false otherwise.
     */
    public boolean closeAllFiles() {
        boolean result = true;

        int tabCount = getTabCount();
        if (tabCount > 0) {

            // Build tab array
            boolean unsavedChanges = false;
            EditPane[] tabs = new EditPane[tabCount];
            for (int i = 0; i < tabCount; i++) {
                tabs[i] = getComponentAt(i);
                if (tabs[i].hasUnsavedEdits())
                    unsavedChanges = true;
            }

            if (unsavedChanges) switch (confirm("one or more files")) {
                case JOptionPane.YES_OPTION:
                    boolean removedAll = true;
                    for (int i = 0; i < tabCount; i++)
                        if (tabs[i].hasUnsavedEdits()) {
                            setSelectedComponent(tabs[i]);
                            boolean saved = tabs[i].save(false);
                            if (saved)
                                remove(tabs[i]);
                            else
                                removedAll = false;
                        }
                        else
                            remove(tabs[i]);
                    return removedAll;
                case JOptionPane.NO_OPTION:
                    removeAll();
                    return true;
                case JOptionPane.CANCEL_OPTION:
                    return false;
                default: // should never occur
                    throw new IllegalStateException("Unexpected return value while closing all files!");
            }
            else
                removeAll();
        }
        return result;
    }

    /**
     * Remove tab from the editor
     *
     * @param tab the tab to remove
     */
    public void remove(EditPane tab) {
        super.remove(tab);

        if (getTabCount() == 0) {
            // Apparently, removing the last tab doesn't fire a changeEvent to this pane,
            // so I'm leaving this here
            // 20150525 - Andrea Proietto
            Main.getGUI().updateGUIState();
            // When last file is closed, menu is unable to respond to mnemonics
            // and accelerators.  Let's have it request focus so it may do so.
            Main.getGUI().menuBar.requestFocus();
        }

    }

    /**
     * Check whether file has unsaved edits and, if so, check with user about
     * saving them.<p/>
     *
     * Specifically: if there is a current file open for editing and its modify
     * flag is true, then give user a dialog box with choice to save, discard
     * edits, or cancel and carry out the decision. This applies to File->New,
     * File->Open, File->Close, and File->Exit.
     *
     * @return true if no unsaved edits or if user chooses to save them or not;
     * false if there are unsaved edits and user cancels the operation.
     */
    boolean editsSavedOrAbandoned(EditPane currentPane) {
        if (currentPane != null && currentPane.hasUnsavedEdits())
            switch (confirm(currentPane.getFilename())) {
                case JOptionPane.YES_OPTION:
                    return currentPane.save(false);
                case JOptionPane.NO_OPTION:
                    return true;
                case JOptionPane.CANCEL_OPTION:
                    return false;
                default: // should never occur
                    return false;
            }
        else
            return true;
    }

    private int confirm(String name) {
        return JOptionPane.showConfirmDialog(Main.getGUI().mainFrame,
                "Changes to " + name + " will be lost unless you save.  Do you wish to save all changes now?",
                "Save program changes?",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
    }

    // Private method to generate the file chooser's list of choosable file filters.
    // It is called when the file chooser is created, and called again each time the Open
    // dialog is activated.  We do this because the user may have added a new filter 
    // during the previous dialog.  This can be done by entering e.g. *.txt in the file
    // name text field.  Java is funny, however, in that if the user does this then
    // cancels the dialog, the new filter will remain in the list BUT if the user does
    // this then ACCEPTS the dialog, the new filter will NOT remain in the list.  However
    // the act of entering it causes a property change event to occur, and we have a
    // handler that will add the new filter to our internal filter list and "restore" it
    // the next time this method is called.  Strangely, if the user then similarly
    // adds yet another new filter, the new one becomes simply a description change
    // to the previous one, the previous object is modified AND NO PROPERTY CHANGE EVENT 
    // IS FIRED!  I could obviously deal with this situation if I wanted to, but enough
    // is enough.  The limit will be one alternative filter at a time.
    // DPS... 9 July 2008
    private void setChoosableFileFilters() {
        // See if a new filter has been added to the master list.  If so,
        // regenerate the fileChooser list from the master list.
        if (fileFilterCount < fileFilters.size()
                || fileFilters.size() != fileChooser.getChoosableFileFilters().length) {
            fileFilterCount = fileFilters.size();
            // First, "deactivate" the listener, because our addChoosableFileFilter
            // calls would otherwise activate it!  We want it to be triggered only
            // by MARS user action.
            boolean activeListener = false;
            if (fileChooser.getPropertyChangeListeners().length > 0) {
                fileChooser.removePropertyChangeListener(listenForUserAddedFileFilter);
                activeListener = true;  // we'll note this, for re-activation later
            }
            // clear out the list and populate from our own ArrayList.
            // Last one added becomes the default.
            fileChooser.resetChoosableFileFilters();
            for (FileFilter fileFilter : fileFilters)
                fileChooser.addChoosableFileFilter(fileFilter);
            // Restore listener.
            if (activeListener)
                fileChooser.addPropertyChangeListener(listenForUserAddedFileFilter);
        }
    }

    private class TabTitleComponent extends JPanel {

        private final JLabel titleLabel;

        TabTitleComponent(EditPane tab) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            titleLabel = new JLabel(tab.getFilename() + "  ");

            setOpaque(false);
            add(titleLabel);

            JButton closeButton = new JButton("×");
            closeButton.setMargin(new Insets(0, 2, 0, 2));
            closeButton.addActionListener((event) -> {
                if (editsSavedOrAbandoned(tab))
                    EditTabbedPane.this.remove(tab);
            });
            add(closeButton);
        }
    }

}
