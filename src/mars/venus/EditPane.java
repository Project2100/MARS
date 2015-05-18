package mars.venus;

import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;
import mars.MIPSprogram;
import mars.Main;
import mars.ProcessingException;
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
public final class EditPane extends JTabbedPane {

    private final String mainUIbaseTitle;
    
    // number of times File->New has been selected.  Used to generate
    // default filename until first Save or Save As.
    private int newUsageCount;

    // Current Directory for Open operation, same for Save operation
    // Values will mainly be setStatus by the EditPane as Open/Save operations occur.
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
     * @param frameTitle
     */
    public EditPane(String frameTitle) {
        super();

        mainUIbaseTitle = frameTitle;
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
        fileFilters.add(fileChooser.getAcceptAllFileFilter());
        fileFilters.add(FilenameFinder.getFileFilter(Main.fileExtensions, "Assembler Files", true));
        fileFilterCount = 0; // this will trigger fileChooser file filter load in next line
        setChoosableFileFilters();

        addChangeListener((event) -> {
            EditTab tab = getSelectedComponent();
            if (tab != null) {
                // New IF statement to permit free traversal of edit panes w/o invalidating
                // assembly if assemble-all is selected.  DPS 9-Aug-2011
                if (Main.getSettings().getBool(mars.Settings.ASSEMBLE_ALL_ENABLED))
                    updateTitles(tab);
                else {
                    updateTitlesAndMenuState(tab);
                    Main.getGUI().executeTab.clearPane();
                }
                tab.tellEditingComponentToRequestFocusInWindow();
            }
        });
    }

    /**
     * The current EditTab representing a file. Returns null if no files open.
     * <br/><br/>Overridden for return type cast
     *
     * @return the current editor pane
     */
    @Override
    public EditTab getSelectedComponent() {
        return (EditTab) super.getSelectedComponent();
    }

    /**
     * If the given file is open in the tabbed pane, make it the current tab. If
     * not opened, open it in a new tab and make it the current tab. If file is
     * unable to be opened, leave current tab as is.
     *
     * @param file File object for the desired file.
     * @return EditTab for the specified file, or null if file is unable to be
     * opened in an EditTab
     */
    public EditTab getCurrentEditTabForFile(File file) {
        EditTab result = null;
        EditTab tab = getEditPaneForFile(file.getPath());
        if (tab != null) {
            if (tab != getSelectedComponent()) this.setSelectedComponent(tab);
            return tab;
        }
        // If no return yet, then file is not open.  Try to open it.
        if (openFile(file))
            result = getSelectedComponent();
        return result;
    }

    /**
     * Carries out all necessary operations to implement the New operation from
     * the File menu.
     */
    public void newFile() {
        
        String name = "Untitled" + (++newUsageCount) + ".asm";
        EditTab tab = new EditTab("", null);

        tab.setShowLineNumbersEnabled(true);
        tab.setFileStatus(VenusUI.NEW_NOT_EDITED);
        tab.setPathname(name);

        addTab(name, tab);
        setTabComponentAt(indexOfComponent(tab), tab.titleComponent);

        VenusUI.reset();
        VenusUI.setName(name);
        VenusUI.setStatus(VenusUI.NEW_NOT_EDITED);

        RegisterFile.resetRegisters();
        VenusUI.setReset(true);

        tab.displayCaretPosition(0);
        setSelectedComponent(tab);
        updateTitlesAndMenuState(tab);
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
        if (Main.getSettings().getAssembleOnOpenEnabled() && mostRecentlyOpenedFile != null)
            fileChooser.setSelectedFile(mostRecentlyOpenedFile);

        if (fileChooser.showOpenDialog(Main.getGUI().mainFrame) == JFileChooser.APPROVE_OPTION) {
            File theFile = fileChooser.getSelectedFile();
            setCurrentOpenDirectory(theFile.getParent());
            //setCurrentSaveDirectory(theFile.getParentDirectory());// 13-July-2011 DPS.
            if (!openFile(theFile))
                return false;

            // possibly send this file right through to the assembler by firing Run->Assemble's
            // actionPerformed() method.
            if (theFile.canRead() && Main.getSettings().getAssembleOnOpenEnabled())
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
        EditTab tab = getEditPaneForFile(currentFilePath);
        if (tab != null) {
            setSelectedComponent(tab);
            updateTitles(tab);
            return false;
        }

        //FileStatus.reset();
        VenusUI.setName(currentFilePath);
        VenusUI.setFile(file);
        VenusUI.setStatus(VenusUI.OPENING);// DPS 9-Aug-2011
        if (file.canRead()) {
            Main.program = new MIPSprogram();
            try {
                Main.program.readSource(currentFilePath);
            }
            catch (ProcessingException pe) {
            }
            // DPS 1 Nov 2006.  Defined a StringBuffer to receive all file contents, 
            // one line at a time, before adding to the Edit pane with one setText.
            // StringBuffer is preallocated to full filelength to eliminate dynamic
            // expansion as lines are added to it. Previously, each line was appended 
            // to the Edit pane as it was read, way slower due to dynamic string alloc.  
            StringBuilder fileContents = new StringBuilder((int) file.length());
            int lineNumber = 1;
            String line = Main.program.getSourceLine(lineNumber++);
            while (line != null) {
                fileContents.append(line).append('\n');
                line = Main.program.getSourceLine(lineNumber++);
            }
            tab = new EditTab(fileContents.toString(), file.toPath());
            tab.setPathname(currentFilePath);

            // The above operation generates an undoable edit, setting the initial
            // text area contents, that should not be seen as undoable by the Undo
            // action.  Let's getStatus rid of it.
            tab.discardAllUndoableEdits();
            tab.setShowLineNumbersEnabled(true);
            tab.setFileStatus(VenusUI.NOT_EDITED);

            addTab(tab.getFilename(), tab);
            setTabComponentAt(indexOfComponent(tab), tab.titleComponent);

            setToolTipTextAt(indexOfComponent(tab), tab.getPathname());
            setSelectedComponent(tab);
            VenusUI.setSaved(true);
            VenusUI.setEdited(false);
            VenusUI.setStatus(VenusUI.NOT_EDITED);

            // If assemble-all, then allow opening of any file w/o invalidating assembly.
            // DPS 9-Aug-2011
            if (Main.getSettings().getBool(mars.Settings.ASSEMBLE_ALL_ENABLED))
                updateTitles(tab);
            else {// this was the original code...
                updateTitlesAndMenuState(tab);
                Main.getGUI().executeTab.clearPane();
            }

            tab.tellEditingComponentToRequestFocusInWindow();
            mostRecentlyOpenedFile = file;
        }
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
        EditTab tab = getSelectedComponent();
        if (tab != null)
            if (editsSavedOrAbandoned()) {
                this.remove(tab);
                Main.getGUI().executeTab.clearPane();
            }
            else
                return false;
        return true;
    }

    /**
     * Carries out all necessary operations to implement the Close All operation
     * from the File menu.
     *
     * @return true if files closed, false otherwise.
     */
    public boolean closeAllFiles() {
        boolean result = true;
        boolean unsavedChanges = false;
        int tabCount = getTabCount();
        if (tabCount > 0) {
            Main.getGUI().executeTab.clearPane();

            EditTab[] tabs = new EditTab[tabCount];
            for (int i = 0; i < tabCount; i++) {
                tabs[i] = (EditTab) getComponentAt(i);
                if (tabs[i].hasUnsavedEdits())
                    unsavedChanges = true;
            }
            if (unsavedChanges)
                switch (confirm("one or more files")) {
                    case JOptionPane.YES_OPTION:
                        boolean removedAll = true;
                        for (int i = 0; i < tabCount; i++)
                            if (tabs[i].hasUnsavedEdits()) {
                                setSelectedComponent(tabs[i]);
                                boolean saved = saveCurrentFile();
                                if (saved)
                                    this.remove(tabs[i]);
                                else
                                    removedAll = false;
                            }
                            else
                                this.remove(tabs[i]);
                        return removedAll;
                    case JOptionPane.NO_OPTION:
                        for (int i = 0; i < tabCount; i++)
                            this.remove(tabs[i]);
                        return true;
                    case JOptionPane.CANCEL_OPTION:
                        return false;
                    default: // should never occur
                        return false;
                }
            else
                for (int i = 0; i < tabCount; i++)
                    this.remove(tabs[i]);
        }
        return result;
    }

    /**
     * Saves file under existing name. If no name, will invoke Save As.
     *
     * @return true if the file was actually saved.
     */
    public boolean saveCurrentFile() {
        EditTab tab = getSelectedComponent();
        if (saveFile(tab)) {
            VenusUI.setSaved(true);
            VenusUI.setEdited(false);
            VenusUI.setStatus(VenusUI.NOT_EDITED);
            tab.setFileStatus(VenusUI.NOT_EDITED);
            updateTitlesAndMenuState(tab);
            return true;
        }
        return false;
    }

    // Save file associatd with specified edit pane.
    // Returns true if save operation worked, else false.
    private boolean saveFile(EditTab tab) {
        if (tab != null) {
            if (tab.isNew()) {
                File theFile = saveAsFile(tab);
                if (theFile != null)
                    tab.setPathname(theFile.getPath());
                return (theFile != null);
            }
            File theFile = new File(tab.getPathname());

//            tab.save();
            
            try (BufferedWriter outFileStream = new BufferedWriter(new FileWriter(theFile))) {
                outFileStream.write(tab.getSource(), 0, tab.getSource().length());
            }
            catch (java.io.IOException c) {
                JOptionPane.showMessageDialog(null, "Save operation could not be completed due to an error:\n" + c,
                        "Save Operation Failed", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Pops up a dialog box to do "Save As" operation. If necessary an
     * additional overwrite dialog is performed.
     *
     * @return true if the file was actually saved.
     */
    public boolean saveAsCurrentFile() {
        EditTab tab = getSelectedComponent();
        File theFile = saveAsFile(tab);
        if (theFile != null) {
            VenusUI.setFile(theFile);
            VenusUI.setName(theFile.getPath());
            VenusUI.setSaved(true);
            VenusUI.setEdited(false);
            VenusUI.setStatus(VenusUI.NOT_EDITED);
            setCurrentSaveDirectory(theFile.getParent());
            tab.setPathname(theFile.getPath());
            tab.setFileStatus(VenusUI.NOT_EDITED);
            updateTitlesAndMenuState(tab);
            return true;
        }
        return false;
    }

    // perform Save As for selected edit pane.  If the save is performed,
    // return its File object.  Otherwise return null.
    private File saveAsFile(EditTab tab) {
        File theFile = null;
        if (tab != null) {
            JFileChooser saveDialog;
            boolean operationOK = false;
            while (!operationOK) {
                // Set Save As dialog directory in a logical way.  If file in
                // edit pane had been previously saved, default to its directory.  
                // If a new file (mipsN.asm), default to current save directory.
                // DPS 13-July-2011
                if (tab.isNew())
                    saveDialog = new JFileChooser(currentSaveDirectory);
                else if (tab.getPathname() != null)
                    saveDialog = new JFileChooser(new File(tab.getPathname()).getParent());
                else
                    saveDialog = new JFileChooser(currentSaveDirectory);
                String paneFile = tab.getFilename();
                if (paneFile != null)
                    saveDialog.setSelectedFile(new File(paneFile));
                // end of 13-July-2011 code.
                saveDialog.setDialogTitle("Save As");

                int decision = saveDialog.showSaveDialog(Main.getGUI().mainFrame);
                if (decision != JFileChooser.APPROVE_OPTION)
                    return null;
                theFile = saveDialog.getSelectedFile();
                operationOK = true;
                if (theFile.exists()) {
                    int overwrite = JOptionPane.showConfirmDialog(Main.getGUI().mainFrame,
                            "File " + theFile.getName() + " already exists.  Do you wish to overwrite it?",
                            "Overwrite existing file?",
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                    switch (overwrite) {
                        case JOptionPane.YES_OPTION:
                            operationOK = true;
                            break;
                        case JOptionPane.NO_OPTION:
                            operationOK = false;
                            break;
                        case JOptionPane.CANCEL_OPTION:
                            return null;
                        default: // should never occur
                            return null;
                    }
                }
            }
            // Either file with selected name does not exist or user wants to 
            // overwrite it, so go for it!
            try (BufferedWriter outFileStream = new BufferedWriter(new FileWriter(theFile))) {
                outFileStream.write(tab.getSource(), 0, tab.getSource().length());

            }
            catch (java.io.IOException c) {
                JOptionPane.showMessageDialog(null, "Save As operation could not be completed due to an error:\n" + c,
                        "Save As Operation Failed", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        return theFile;
    }

    /**
     * Saves all files currently open in the editor.
     *
     * @return true if operation succeeded otherwise false.
     */
    public boolean saveAllFiles() {
        boolean result = false;
        int tabCount = getTabCount();
        if (tabCount > 0) {

            result = true;
            EditTab[] tabs = new EditTab[tabCount];
            EditTab savedPane = getSelectedComponent();
            for (int i = 0; i < tabCount; i++) {
                tabs[i] = (EditTab) getComponentAt(i);
                if (tabs[i].hasUnsavedEdits()) {
                    this.setSelectedComponent(tabs[i]);
                    if (saveFile(tabs[i])) {
                        tabs[i].setFileStatus(VenusUI.NOT_EDITED);
                        setTitle(tabs[i].getPathname(), tabs[i].getFilename(), tabs[i].getFileStatus());
                    }
                    else
                        result = false;
                }
            }
            this.setSelectedComponent(savedPane);
            if (result) {
                EditTab tab = getSelectedComponent();
                VenusUI.setSaved(true);
                VenusUI.setEdited(false);
                VenusUI.setStatus(VenusUI.NOT_EDITED);
                tab.setFileStatus(VenusUI.NOT_EDITED);
                updateTitlesAndMenuState(tab);
            }
        }
        return result;
    }

    /**
     * Remove the pane and simUpdate menu status
     *
     * @param tab
     */
    public void remove(EditTab tab) {
        super.remove(tab);
        tab = getSelectedComponent(); // is now next tab or null
        if (tab == null) {
            VenusUI.setStatus(VenusUI.NO_FILE);
            setTitle("", "", VenusUI.NO_FILE);
            Main.getGUI().setMenuState(VenusUI.NO_FILE);
        }
        else {
            VenusUI.setStatus(tab.getFileStatus());
            updateTitlesAndMenuState(tab);
        }
        // When last file is closed, menu is unable to respond to mnemonics
        // and accelerators.  Let's have it request focus so it may do so.
        if (getTabCount() == 0) Main.getGUI().menuBar.requestFocus();
    }

    // Handy little utility to simUpdate the titleLabel on the current tab and the frame titleLabel bar
    // and also to simUpdate the MARS menu state (controls which actions are enabled).
    private void updateTitlesAndMenuState(EditTab tab) {
        setTitle(tab.getPathname(), tab.getFilename(), tab.getFileStatus());
        tab.updateStaticFileStatus(); //  for legacy code that depends on the static FileStatus (pre 4.0)
        Main.getGUI().setMenuState(tab.getFileStatus());
    }

    // Handy little utility to simUpdate the titleLabel on the current tab and the frame titleLabel bar
    // and also to simUpdate the MARS menu state (controls which actions are enabled).
    // DPS 9-Aug-2011
    private void updateTitles(EditTab tab) {
        setTitle(tab.getPathname(), tab.getFilename(), tab.getFileStatus());
        boolean assembled = VenusUI.isAssembled();
        tab.updateStaticFileStatus(); //  for legacy code that depends on the static FileStatus (pre 4.0)
        VenusUI.setAssembled(assembled);
    }

    /**
     * If there is an EditTab for the given file pathname, return it else return
     * null.
     *
     * @param pathname Pathname for desired file
     * @return the EditTab for this file if it is open in the editor, or null if
     * not.
     */
    public EditTab getEditPaneForFile(String pathname) {
        EditTab openPane = null;
        for (int i = 0; i < getTabCount(); i++) {
            EditTab pane = (EditTab) getComponentAt(i);
            if (pane.getPathname().equals(pathname)) {
                openPane = pane;
                break;
            }
        }
        return openPane;
    }

    /**
     * Check whether file has unsaved edits and, if so, check with user about
     * saving them. Specifically: if there is a current file open for editing
     * and its modify flag is true, then give user a dialog box with choice to
     * save, discard edits, or cancel and carry out the decision. This applies
     * to File->New, File->Open, File->Close, and File->Exit.
     *
     * @return true if no unsaved edits or if user chooses to save them or not;
     * false if there are unsaved edits and user cancels the operation.
     */
    public boolean editsSavedOrAbandoned() {
        return editsSavedOrAbandoned(getSelectedComponent());
    }

    boolean editsSavedOrAbandoned(EditTab currentPane) {
        if (currentPane != null && currentPane.hasUnsavedEdits())
            switch (confirm(currentPane.getFilename())) {
                case JOptionPane.YES_OPTION:
                    return saveCurrentFile();
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
     * EditPane. Returns default, directory MARS is launched from, if no Save or
     * Save As operations have been performed.
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
     * Places name of file currently being edited into its edit tab and the
     * application's titleLabel bar. The edit tab will contain only the
     * filename, the titleLabel bar will contain full pathname. If file has been
     * modified since created, opened or saved, as indicated by value of the
     * status parameter, the name and path will be followed with an '*'. If
     * newly-created file has not yet been saved, the titleLabel bar will show
     * (temporary) file name but not path.
     *
     * @param path Full pathname for file
     * @param name Name of file (last component of path)
     * @param status Edit status of file. See FileStatus static constants.
     */
    public void setTitle(String path, String name, int status) {
        if (status == VenusUI.NO_FILE || name == null || name.length() == 0)
            Main.getGUI().mainFrame.setTitle(mainUIbaseTitle);
        else {
            String edited = (status == VenusUI.NEW_EDITED || status == VenusUI.EDITED) ? "* " : "  ";
            String titleName = (status == VenusUI.NEW_EDITED || status == VenusUI.NEW_NOT_EDITED) ? name : path;
            Main.getGUI().mainFrame.setTitle(titleName + edited + " - " + mainUIbaseTitle);

            getSelectedComponent().titleLabel.setText(name + edited);
        }
    }

}
