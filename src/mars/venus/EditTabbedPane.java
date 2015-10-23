package mars.venus;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;
import mars.Main;
import mars.mips.hardware.RegisterFile;
import mars.settings.BooleanSettings;
import mars.settings.StringSettings;
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
public final class EditTabbedPane extends JTabbedPane implements Iterable<EditPane> {

    // number of times File->New has been selected.  Used to generate
    // default filenames
    private int newFileCount;
    private Path mostRecentlyOpenedFile;
    private final JFileChooser fileChooser;
    private int fileFilterCount;
    private ArrayList<FileFilter> fileFilters;
    private final PropertyChangeListener listenForUserAddedFileFilter;

    /**
     * Constructor for the editor tabbed pane.
     */
    public EditTabbedPane() {
        super();

        newFileCount = 0;
        mostRecentlyOpenedFile = null;
        fileChooser = new JFileChooser();

        //////////////////////////////////////////////////////////////////////////////////
        //  Private inner class for special property change listener.  DPS 9 July 2008.
        //  If user adds a file filter, e.g. by typing *.txt into the file text field then pressing
        //  Enter, then it is automatically added to the array of choosable file filters.  BUT, unless you
        //  Cancel out of the Open dialog, it is then REMOVED from the list automatically also. Here
        //  we will achieve a sort of persistence at least through the current activation of MARS.
        fileChooser.addPropertyChangeListener(listenForUserAddedFileFilter = (event) -> {
            if (event.getPropertyName().equals(JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY)) {
                FileFilter[] newFilters = (FileFilter[]) event.getNewValue();
                if (newFilters.length > fileFilters.size())
                    // new filter added, so add to end of master list.
                    fileFilters.add(newFilters[newFilters.length - 1]);
            }
        });

        // Note: add sequence is significant - last one added becomes default.
        fileFilters = new ArrayList<>();
        fileFilters.add(FilenameFinder.getFileFilter(Main.fileExtensions, "Assembler Files"));
        fileFilters.add(FilenameFinder.getFileFilter(new ArrayList<>(Arrays.asList(".txt", ".in")), "Text/input Files"));
        fileFilters.add(fileChooser.getAcceptAllFileFilter());
        fileFilterCount = 0; // this will trigger fileChooser file filter load in next line
        setChoosableFileFilters();

        addChangeListener((event) -> Main.getGUI().updateGUIState());
    }
    
    @Override
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        super.insertTab(title, icon, component, tip, index);
        // We want to be notified of editor font changes! See update() below.
        Main.getSettings().addObserver((EditPane) component);
    }

    /**
     * Standard iterator, {@link Iterator#remove()} is implemented.
     *
     * @return a new iterator over this tabbed pane's tabs
     */
    @Override
    public Iterator<EditPane> iterator() {
        return new Iterator<EditPane>() {
            private int index = 0;
            private boolean canRemove = false;

            @Override
            public boolean hasNext() {
                return index < getTabCount();
            }

            @Override
            public EditPane next() {
                if (!hasNext()) throw new NoSuchElementException();
                canRemove = true;
                return getComponentAt(index++);
            }

            @Override
            public void remove() {
                if (canRemove) {
                    EditTabbedPane.super.remove(getComponentAt(--index));
                    canRemove = false;
                }
                else throw new ConcurrentModificationException();
            }
        };
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
     * Carries out all necessary operations to implement the New operation from
     * the File menu.
     */
    public void newFile() {

        String name = "Untitled" + (++newFileCount) + ".asm";
        EditPane tab = new EditPane(Paths.get(name));

        addTab(name, tab);

        VenusUI.setReset(true);
        RegisterFile.resetRegisters();
        tab.displayCaretPosition(0);
        setSelectedComponent(tab);
    }

    /**
     * Carries out all necessary operations to implement the Open operation from
     * the File menu. This begins with an Open File dialog.
     */
    public void openFile() {

        // The fileChooser's list may be rebuilt from the master ArrayList if a new filter
        // has been added by the user.
        setChoosableFileFilters();
        // get name of file to be opened and load contents into text editing area.
        fileChooser.setCurrentDirectory(new File(StringSettings.OPEN_DIRECTORY.get()));
        // Set default to previous file opened, if any.  This is useful in conjunction
        // with option to assemble file automatically upon opening.  File likely to have
        // been edited externally (e.g. by Mipster).
        if (BooleanSettings.ASSEMBLE_ON_OPEN.isSet() && mostRecentlyOpenedFile != null)
            fileChooser.setSelectedFile(mostRecentlyOpenedFile.toFile());

        if (fileChooser.showOpenDialog(Main.getGUI().mainFrame) == JFileChooser.APPROVE_OPTION) {
            Path path = fileChooser.getSelectedFile().toPath();
            StringSettings.OPEN_DIRECTORY.set(path.getParent().toString());
            if (!openFile(path))
                return;

            // possibly send this file right through to the assembler by switching mode
            if (BooleanSettings.ASSEMBLE_ON_OPEN.isSet())
                Main.getGUI().toggleGUIMode();
        }

    }

    /**
     * Carries out all necessary operations to open the specified file in the
     * editor.
     *
     * Specifically, if the file is already open, then its tab will be selected,
     * otherwise a new tab containing the file content will be created and
     * selected.
     *
     * @param path a {@code Path} object defining the file to open
     * @return true if file was already opened or has been successfully opened,
     * false otherwise.
     */
    private boolean openFile(Path path) {
        try {
            path = path.toRealPath();
        }
        catch (IOException ioe) {
            Main.logger.log(Level.SEVERE, "Cannot open file: " + path, ioe);
            return false;
        }

        EditPane tab = getTabForFile(path);

        // If the specified file isn't open yet and is readable, open it
        if (tab == null && Files.isReadable(path)) {
            tab = new EditPane(path);
            addTab(path.getFileName().toString(), tab);
            setToolTipTextAt(indexOfComponent(tab), path.toString());
            setSelectedComponent(tab);
            mostRecentlyOpenedFile = path;
        }

        setSelectedComponent(tab);
        return true;
    }

    /**
     * If there is an EditPane for the given file pathname, return it else
     * return null.
     *
     * @param target Pathname for desired file
     * @return the EditPane for this file if it is open in the editor, or null
     * if not.
     */
    public EditPane getTabForFile(Path target) {
        for (EditPane pane : this)
            if (target.equals(pane.getPath())) return pane;
        return null;
    }

    /**
     * Saves all files currently open in the editor.
     *
     * @return true if operation succeeded otherwise false.
     */
    public boolean saveAllFiles() {

        EditPane savedPane = getSelectedComponent();
        for (EditPane tab : this)
            if (tab.hasUnsavedEdits()) {
                setSelectedComponent(tab);
                if (!tab.save(false))
                    return false;
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

        Iterator<EditPane> itr = iterator();

        // Close unedited tabs
        while (itr.hasNext())
            if (!itr.next().hasUnsavedEdits()) itr.remove();

        if (getTabCount() != 0) switch (confirm("one or more files")) {
            case JOptionPane.YES_OPTION:
                itr = iterator();
                EditPane unsavedTab;
                while (itr.hasNext()) {
                    unsavedTab = itr.next();
                    if (unsavedTab.save(false))
                        itr.remove();
                    else {
                        setSelectedComponent(unsavedTab);
                        JOptionPane.showMessageDialog(
                                Main.getGUI().mainFrame,
                                "Unable to save " + unsavedTab.getPath(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
                return getTabCount() == 0;
            case JOptionPane.NO_OPTION:
                break;
            case JOptionPane.CANCEL_OPTION:
                return false;
            default: // should never occur
                throw new IllegalStateException("Unexpected return value while closing all files!");
        }

        removeAll();
        return true;
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
            switch (confirm(currentPane.getPath().getFileName().toString())) {
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

    /**
     * Will select the specified line in an editor tab. If the file is open but
     * not current, its tab will be made current. If the file is not open, it
     * will be opened in a new tab and made current, however the line will not
     * be selected (apparent problem with JEditTextArea).
     *
     * @param fileName A String containing the file path name.
     * @param line Line number for error message
     * @param column Column number for error message
     */
    public void selectEditorTextLine(String fileName, int line, int column) {
        if (openFile(Paths.get(fileName)))
            getSelectedComponent().selectLine(line, column);
    }

    private class TabTitleComponent extends JPanel {

        private final JLabel titleLabel;

        TabTitleComponent(EditPane tab) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            titleLabel = new JLabel(tab.getPath().getFileName().toString() + "  ");

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
