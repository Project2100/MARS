package mars.venus;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ItemEvent;
import mars.venus.editors.MARSTextEditingArea;
import mars.venus.editors.generic.GenericTextArea;
import mars.venus.editors.jeditsyntax.JEditBasedTextArea;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Observable;
import java.util.Observer;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import mars.Main;
import mars.Settings;

/*
 Copyright (c) 2003-2011,  Pete Sanderson and Kenneth Vollmar

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
 * Represents one file opened for editing. Maintains required internal
 * structures. Before Mars 4.0, there was only one editor pane, a tab, and only
 * one file could be open at a time. With 4.0 came the multifile (pane, tab)
 * editor, and existing duties were split between EditTab and the new EditPane
 * class.
 *
 * @author Sanderson and Bumgarner
 */
public class EditTab extends JPanel implements Observer {

    private Path file;
    private int status;

    private MARSTextEditingArea sourceCode;
    private JLabel caretPositionLabel;

    final TabTitleComponent titleComponent;
    JLabel titleLabel;

    private JCheckBox showLineNumbers;
    private JLabel lineNumbers;
    private boolean isCompoundEdit = false;
    private CompoundEdit compoundEdit;

    /**
     * Constructor for the EditPane class.
     *
     * @param content the source code to display in the tab
     * @param file
     */
    public EditTab(String content, Path file) {
        super(new BorderLayout());

        // We want to be notified of editor font changes! See simUpdate() below.
        Main.getSettings().addObserver(this);

        // Initialized as null
        this.file = file;
        status = file == null ? VenusUI.NEW_NOT_EDITED : VenusUI.NOT_EDITED;

        lineNumbers = new JLabel();

        caretPositionLabel = new JLabel();
        caretPositionLabel.setToolTipText("Tracks the current position of the text editing cursor.");

        // sourceCode uses caretPositionLabel
        sourceCode = Main.getSettings().getBool(Settings.GENERIC_TEXT_EDITOR)
                ? new GenericTextArea(this, lineNumbers)
                : new JEditBasedTextArea(this, lineNumbers);
        sourceCode.setSourceCode(content, true);

        // sourceCode is responsible for its own scrolling
        add(sourceCode.getOuterComponent(), BorderLayout.CENTER);

        // If source code is modified, will setStatus flag to trigger/request file save.
        sourceCode.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent evt) {
                // IF statement added DPS 9-Aug-2011
                // This method is triggered when file contents added to document
                // upon opening, even though not edited by user.  The IF
                // statement will sense this situation and immediately return.
                if (VenusUI.getStatus() == VenusUI.OPENING) {
                    status = VenusUI.NOT_EDITED;
                    VenusUI.setStatus(VenusUI.NOT_EDITED);
                    if (showingLineNumbers())
                        lineNumbers.setText(getLineNumbers());
                    return;
                }
                // End of 9-Aug-2011 modification.                    
                if (getFileStatus() == VenusUI.NEW_NOT_EDITED)
                    setFileStatus(VenusUI.NEW_EDITED);
                if (getFileStatus() == VenusUI.NOT_EDITED)
                    setFileStatus(VenusUI.EDITED);
                if (getFileStatus() == VenusUI.NEW_EDITED)
                    Main.getGUI().editTabbedPane.setTitle("", getFilename(), getFileStatus());
                else
                    Main.getGUI().editTabbedPane.setTitle(getPathname(), getFilename(), getFileStatus());

                VenusUI.setEdited(true);
                switch (VenusUI.getStatus()) {
                    case VenusUI.NEW_NOT_EDITED:
                        VenusUI.setStatus(VenusUI.NEW_EDITED);
                        break;
                    case VenusUI.NEW_EDITED:
                        break;
                    default:
                        VenusUI.setStatus(VenusUI.EDITED);
                }

                Main.getGUI().executeTab.clearPane(); // DPS 9-Aug-2011

                if (showingLineNumbers())
                    lineNumbers.setText(getLineNumbers());
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                this.insertUpdate(evt);
            }

            @Override
            public void changedUpdate(DocumentEvent evt) {
                this.insertUpdate(evt);
            }
        });

        titleLabel = new JLabel(getFilename());
        titleComponent = new TabTitleComponent();

        showLineNumbers = new JCheckBox("Show Line Numbers");
        showLineNumbers.setToolTipText("If checked, will display line number for each line of text.");
        showLineNumbers.setEnabled(false);
        // Show line numbers by default.
        showLineNumbers.setSelected(Main.getSettings().getEditorLineNumbersDisplayed());

        lineNumbers.setFont(getLineNumberFont(sourceCode.getFont()));
        lineNumbers.setVerticalAlignment(JLabel.TOP);
        lineNumbers.setText("1");
        lineNumbers.setVisible(true);

        // Listener fires when "Show Line Numbers" check box is clicked.
        showLineNumbers.addItemListener((ItemEvent e) -> {
            if (showLineNumbers.isSelected()) {
                lineNumbers.setText(getLineNumbers());
                lineNumbers.setVisible(true);
            }
            else {
                lineNumbers.setText("");
                lineNumbers.setVisible(false);
            }
            sourceCode.revalidate(); // added 16 Jan 2012 to assure label redrawn.
            Main.getSettings().setEditorLineNumbersDisplayed(showLineNumbers.isSelected());
            // needed because caret disappears when checkbox clicked
            sourceCode.setCaretVisible(true);
            sourceCode.requestFocusInWindow();
        });

        JPanel editInfo = new JPanel(new BorderLayout());
        editInfo.add(caretPositionLabel, BorderLayout.WEST);
        editInfo.add(showLineNumbers, BorderLayout.CENTER);
        add(editInfo, BorderLayout.SOUTH);
    }

    /**
     * For initializing the source code when opening an ASM file
     *
     * @param s String containing text
     * @param editable setStatus true if code is editable else false
     */
    public void setSourceCode(String s, boolean editable) {
        sourceCode.setSourceCode(s, editable);
    }

    /**
     * Get rid of any accumulated undoable edits. It is useful to call this
     * method after opening a file into the text area. The act of setting its
     * text content upon reading the file will generate an undoable edit.
     * Normally you don't want a freshly-opened file to appear with its Undo
     * action enabled. But it will unless you call this after setting the text.
     */
    public void discardAllUndoableEdits() {
        sourceCode.discardAllUndoableEdits();
    }

    /**
     * Form string with source code line numbers. Resulting string is HTML, for
     * which JLabel will happily honor {@code <br>} instead of {@code \n} 
     * to do multi-line label.
     * The line number list is a JLabel with one line number per line.
     *
     * @return
     */
    private String getLineNumbers() {
        return Stream.iterate(1, i -> i + 1)
                .limit(getSourceLineCount())
                .map((i) -> Integer.toString(i) + "&nbsp;<br/>")
                .reduce("<html><body style='text-align:right'>", String::concat)
                + "</body></html>";
    }

    /**
     * Gets th number of lines in source code text. Equivalent of
     * {@code sourceCode.getDocument().getDefaultRootElement().getElementCount()}
     *
     * @return line count of this document
     */
    public int getSourceLineCount() {
        return sourceCode.getDocument().getDefaultRootElement().getElementCount();
    }

    /**
     * Get source code text
     *
     * @return Sting containing source code
     */
    public String getSource() {
        return sourceCode.getText();
    }

    /**
     * Get the editing status for this EditTab's associated document. This will
     * be one of the constants from class FileStatus.
     *
     * @return current editing status. See FileStatus static constants.
     */
    public int getFileStatus() {
        return status;
    }

    /**
     * Set the editing status for this EditTab's associated document. For the
     * argument, use one of the constants from class FileStatus.
     *
     * @param fileStatus the status constant from class FileStatus
     */
    public void setFileStatus(int fileStatus) {
        //TODO ADD CHECKS FROM STATE TO STATE
        status = fileStatus;
    }

    /**
     * Get file name with no path information. See java.io.File.getName()
     *
     * @return filename as a String
     */
    public String getFilename() {
        return (file == null) ? null : file.getFileName().toString();
    }

    /**
     * Get full file pathname. See java.io.File.getPath()
     *
     * @return full pathname as a String. Null if
     */
    public String getPathname() {
        return (file == null) ? null : file.toString();
    }

    /**
     * Set full file pathname. See java.io.File(String pathname) for parameter
     * specs.
     *
     * @param pathname the new pathname. If no directory path,
     * getParentDirectory() will return null.
     */
    public void setPathname(String pathname) {
        file = Paths.get(pathname);
    }

    /**
     * Determine if file has been modified since last save or, if not yet saved,
     * since being created using New or Open.
     *
     * @return true if file has been modified since save or creation, false
     * otherwise.
     */
    public boolean hasUnsavedEdits() {
        return status == VenusUI.NEW_EDITED || status == VenusUI.EDITED;
    }

    /**
     * Determine if file is "new", which means created using New but not yet
     * saved. If created using Open, it is not new.
     *
     * @return true if file was created using New and has not yet been saved,
     * false otherwise.
     */
    public boolean isNew() {
        return status == VenusUI.NEW_NOT_EDITED || status == VenusUI.NEW_EDITED;
    }

    /**
     * Delegates to text area's requestFocusInWindow method.
     */
    public void tellEditingComponentToRequestFocusInWindow() {
        this.sourceCode.requestFocusInWindow();
    }

    /**
     * Delegates to corresponding FileStatus method
     */
    public void updateStaticFileStatus() {
        VenusUI.updateStaticFileStatus(file.toFile(), status);
    }

    /**
     * getStatus the manager in charge of Undo and Redo operations
     *
     * @return the UnDo manager
     */
    public UndoManager getUndoManager() {
        return sourceCode.getUndoManager();
    }

    /*       Note: these are invoked only when copy/cut/paste are used from the
     toolbar or menu or the defined menu Alt codes.  When
     Ctrl-C, Ctrl-X or Ctrl-V are used, this code is NOT invoked
     but the operation works correctly!
     The "setStatus visible" operations are used because clicking on the toolbar
     icon causes both the selection highlighting AND the blinking cursor
     to disappear!  This does not happen when using menu selection or 
     Ctrl-C/X/V
     */
    /**
     * copy currently-selected text into clipboard
     */
    public void copyText() {
        sourceCode.copy();
        sourceCode.setCaretVisible(true);
        sourceCode.setSelectionVisible(true);
    }

    /**
     * cut currently-selected text into clipboard
     */
    public void cutText() {
        sourceCode.cut();
        sourceCode.setCaretVisible(true);
    }

    /**
     * paste clipboard contents at cursor position
     */
    public void pasteText() {
        sourceCode.paste();
        sourceCode.setCaretVisible(true);
    }

    /**
     * select all text
     */
    public void selectAllText() {
        sourceCode.selectAll();
        sourceCode.setCaretVisible(true);
        sourceCode.setSelectionVisible(true);
    }

    /**
     * Undo previous edit
     */
    public void undo() {
        sourceCode.undo();
        updateUndoState();
    }

    /**
     * Redo previous edit
     */
    public void redo() {
        sourceCode.redo();
        updateUndoState();
    }

    public void updateUndoState() {
        Main.getGUI().updateUndoManager(this.getUndoManager());
    }

    /**
     * getStatus editor's line number display status
     *
     * @return true if editor is current displaying line numbers, false
     * otherwise.
     */
    public boolean showingLineNumbers() {
        return showLineNumbers.isSelected();
    }

    /**
     * enable or disable checkbox that controls display of line numbers
     *
     * @param enabled True to enable box, false to disable.
     */
    public void setShowLineNumbersEnabled(boolean enabled) {
        showLineNumbers.setEnabled(enabled);
        //showLineNumbers.setSelected(false); // setStatus off, whether closing or opening
    }

    /**
     * Update the caret position label on the editor's border to display the
     * current line and column. The position is given as text stream offset and
     * will be converted into line and column.
     *
     * @param pos Offset into the text stream of caret.
     */
    public void displayCaretPosition(int pos) {
        displayCaretPosition(convertStreamPositionToLineColumn(pos));
    }

    /**
     * Display cursor coordinates
     *
     * @param p Point object with x-y (column, line number) coordinates of
     * cursor
     */
    public void displayCaretPosition(Point p) {
        caretPositionLabel.setText("Line: " + p.y + " Column: " + p.x);
    }

    /**
     * Given byte stream position in text being edited, calculate its column and
     * line number coordinates.
     *
     * @param stream position of character
     * @return position Its column and line number coordinate as a Point.
     */
    private static final char newline = '\n';

    public Point convertStreamPositionToLineColumn(int position) {
        String textStream = sourceCode.getText();
        int line = 1;
        int column = 1;
        for (int i = 0; i < position; i++)
            if (textStream.charAt(i) == newline) {
                line++;
                column = 1;
            }
            else
                column++;
        return new Point(column, line);
    }

    /**
     * Given line and column (position in the line) numbers, calculate its byte
     * stream position in text being edited.
     *
     * @param line Line number in file (starts with 1)
     * @param column Position within that line (starts with 1)
     * @return corresponding stream position. Returns -1 if there is no
     * corresponding position.
     */
    public int convertLineColumnToStreamPosition(int line, int column) {
        String textStream = sourceCode.getText();
        int textLength = textStream.length();
        int textLine = 1;
        int textColumn = 1;
        for (int i = 0; i < textLength; i++) {
            if (textLine == line && textColumn == column)
                return i;
            if (textStream.charAt(i) == newline) {
                textLine++;
                textColumn = 1;
            }
            else
                textColumn++;
        }
        return -1;
    }

    /**
     * Select the specified editor text line. Lines are numbered starting with
     * 1, consistent with line numbers displayed by the editor.
     *
     * @param line The desired line number of this TextPane's text. Numbering
     * starts at 1, and nothing will happen if the parameter value is less than
     * 1
     */
    public void selectLine(int line) {
        if (line > 0) {
            int lineStartPosition = convertLineColumnToStreamPosition(line, 1);
            int lineEndPosition = convertLineColumnToStreamPosition(line + 1, 1) - 1;
            if (lineEndPosition < 0) // DPS 19 Sept 2012.  Happens if "line" is last line of file.

                lineEndPosition = sourceCode.getText().length() - 1;
            if (lineStartPosition >= 0) {
                sourceCode.select(lineStartPosition, lineEndPosition);
                sourceCode.setSelectionVisible(true);
            }
        }
    }

    /**
     * Select the specified editor text line. Lines are numbered starting with
     * 1, consistent with line numbers displayed by the editor.
     *
     * @param line The desired line number of this TextPane's text. Numbering
     * starts at 1, and nothing will happen if the parameter value is less than
     * 1
     * @param column Desired column at which to place the cursor.
     */
    public void selectLine(int line, int column) {
        selectLine(line);
        // Made one attempt at setting cursor; didn't work but here's the attempt
        // (imagine using it in the one-parameter overloaded method above)
        //sourceCode.setCaretPosition(lineStartPosition+column-1);        
    }

    /**
     * Finds next occurrence of text in a forward search of a string. Search
     * begins at the current cursor location, and wraps around when the end of
     * the string is reached.
     *
     * @param find the text to locate in the string
     * @param caseSensitive true if search is to be case-sensitive, false
     * otherwise
     * @return TEXT_FOUND or TEXT_NOT_FOUND, depending on the result.
     */
    public int doFindText(String find, boolean caseSensitive) {
        return sourceCode.doFindText(find, caseSensitive);
    }

    /**
     * Finds and replaces next occurrence of text in a string in a forward
     * search. If cursor is initially at end of matching selection, will
     * immediately replace then find and select the next occurrence if any.
     * Otherwise it performs a find operation. The replace can be undone with
     * one undo operation.
     *
     * @param find the text to locate in the string
     * @param replace the text to replace the find text with - if the find text
     * exists
     * @param caseSensitive true for case sensitive. false to ignore case
     * @return Returns TEXT_FOUND if not initially at end of selected match and
     * matching occurrence is found. Returns TEXT_NOT_FOUND if the text is not
     * matched. Returns TEXT_REPLACED_NOT_FOUND_NEXT if replacement is
     * successful but there are no additional matches. Returns
     * TEXT_REPLACED_FOUND_NEXT if reaplacement is successful and there is at
     * least one additional match.
     */
    public int doReplace(String find, String replace, boolean caseSensitive) {
        return sourceCode.doReplace(find, replace, caseSensitive);
    }

    /**
     * Finds and replaces <B>ALL</B> occurrences of text in a string in a
     * forward search. All replacements are bundled into one CompoundEdit, so
     * one Undo operation will undo all of them.
     *
     * @param find the text to locate in the string
     * @param replace the text to replace the find text with - if the find text
     * exists
     * @param caseSensitive true for case sensitive. false to ignore case
     * @return the number of occurrences that were matched and replaced.
     */
    public int doReplaceAll(String find, String replace, boolean caseSensitive) {
        return sourceCode.doReplaceAll(find, replace, caseSensitive);
    }

    /**
     * Update, if source code is visible, when Font setting changes. This method
     * is specified by the Observer interface.
     *
     * @param fontChanger
     */
    @Override
    public void update(Observable fontChanger, Object arg) {
        sourceCode.setFont(Main.getSettings().getEditorFont());
        sourceCode.setLineHighlightEnabled(Main.getSettings().getBool(Settings.EDITOR_CURRENT_LINE_HIGHLIGHTING));
        sourceCode.setCaretBlinkRate(Main.getSettings().getCaretBlinkRate());
        sourceCode.setTabSize(Main.getSettings().getEditorTabSize());
        sourceCode.updateSyntaxStyles();
        sourceCode.revalidate();
        // We want line numbers to be displayed same size but always PLAIN style.
        // Easiest way to getStatus same pixel height as source code is to setStatus to same
        // font family as the source code! It can getStatus a bit complicated otherwise
        // because different fonts will render the same font size in different
        // pixel heights.  This is a factor because the line numbers as displayed
        // in the editor form a separate column from the source code and if the
        // pixel height is not the same then the numbers will not line up with
        // the source lines.
        lineNumbers.setFont(getLineNumberFont(sourceCode.getFont()));
        lineNumbers.revalidate();
    }

    /* Private helper method.
     * Determine font to use for editor line number display, given current
     * font for source code.
     */
    private Font getLineNumberFont(Font sourceFont) {
        return (sourceCode.getFont().getStyle() == Font.PLAIN)
                ? sourceFont
                : new Font(sourceFont.getFamily(), Font.PLAIN, sourceFont.getSize());
    }

    /**
     * Get file parent pathname. See java.io.File.getParentDirectory()
     *
     * @return parent full pathname as a String
     */
    public String getParentDirectory() {
        return (file == null) ? null : file.getParent().toString();
    }

    /**
     * Set full file pathname. See java.io.File(String parent, String child) for
     * parameter specs.
     *
     * @param parent the parent directory of the file. If null,
     * getParentDirectory() will return null.
     * @param name the name of the file (no directory path)
     */
    public void setPathname(String parent, String name) {
        file = Paths.get(parent, name);
    }

    private class TabTitleComponent extends JPanel {

        public TabTitleComponent() {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));

            setOpaque(false);
            add(titleLabel);

            JButton closeButton = new JButton("×");
            closeButton.setMargin(new Insets(0, 2, 0, 2));
            closeButton.addActionListener((event) -> {
                // if for some reason the tab isn't found, an IOOBE is thrown
                if (Main.getGUI().editTabbedPane.editsSavedOrAbandoned(EditTab.this))
                    Main.getGUI().editTabbedPane.remove(EditTab.this);
            });
            add(closeButton);

        }

    }

}
