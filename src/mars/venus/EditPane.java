package mars.venus;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;
import mars.MIPSprogram;
import mars.Main;
import mars.ProcessingException;
import mars.Settings;
import mars.venus.editors.MARSTextEditingArea;
import mars.venus.editors.generic.GenericTextArea;
import mars.venus.editors.jeditsyntax.JEditBasedTextArea;

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
 * editor, and existing duties were split between EditPane and the new
 * EditTabbedPane class.
 *
 * @author Sanderson and Bumgarner
 */
public class EditPane extends JPanel implements Observer {

    private static final char newline = '\n';

    private Path file;
    private boolean edited;

    private final MARSTextEditingArea sourceCode;
    private final JLabel lineNumbers;
    private final JLabel caretPositionLabel;
    private final JCheckBox showLineNumbers;

    /**
     * Constructs a tab to display in editor mode with the contents of the
     * specified {@code source} file.
     *
     * @param source The source file to open in this tab
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public EditPane(Path source) {
        super(new BorderLayout());

        // Argument check
        if (source == null)
            throw new IllegalArgumentException("Passing null Path to tab constructor!");

        // We want to be notified of editor font changes! See update() below.
        Main.getSettings().addObserver(this);

        // Field init
        file = source;
        edited = false;

        lineNumbers = new JLabel();

        caretPositionLabel = new JLabel();
        caretPositionLabel.setToolTipText("Tracks the current position of the text editing cursor.");

        showLineNumbers = new JCheckBox("Show Line Numbers");
        showLineNumbers.setToolTipText("If checked, will display line number for each line of text.");
        showLineNumbers.setEnabled(false);
        // Show line numbers by default.
        showLineNumbers.setSelected(Settings.BooleanSettings.EDITOR_LINE_NUMBERS.isSet());

        // sourceCode uses caretPositionLabel
        sourceCode = Settings.BooleanSettings.GENERIC_TEXT_EDITOR.isSet()
                ? new GenericTextArea(this, lineNumbers)
                : new JEditBasedTextArea(this, lineNumbers);

        if (source.getRoot() != null) {
            Main.program = new MIPSprogram();
            try {
                Main.program.readSource(source.toString());
            }
            catch (ProcessingException pe) {
            }
        }

        sourceCode.setSourceCode(source.getRoot() == null
                ? ""
                : Main.program.getSourceList()
                .stream()
                .reduce((s, t) -> s + newline + t)
                .get(), true);

        // The above operation generates an undoable edit, setting the initial
        // text area contents, that should not be seen as undoable by the Undo
        // action.  Let's get rid of it.
        sourceCode.discardAllUndoableEdits();
        showLineNumbers.setEnabled(true);

        // If source code is modified, will update application status
        sourceCode.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent evt) {
                if (!edited) {
                    edited = true;
                    Main.getGUI().updateGUIState();
                }
                if (showLineNumbers.isSelected())
                    lineNumbers.setText(getLineNumbers());
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                insertUpdate(evt);
            }

            @Override
            public void changedUpdate(DocumentEvent evt) {
            }
        });

        lineNumbers.setFont(getLineNumberFont(sourceCode.getFont()));
        lineNumbers.setVerticalAlignment(JLabel.TOP);
        if (showLineNumbers.isSelected())
            lineNumbers.setText(getLineNumbers());
        lineNumbers.setVisible(true);

        // Listener fires when "Show Line Numbers" check box is clicked.
        showLineNumbers.addItemListener((event) -> {
            boolean isSelected = event.getStateChange() == ItemEvent.SELECTED;

            lineNumbers.setText(isSelected ? getLineNumbers() : "");
            lineNumbers.setVisible(isSelected);
            Settings.BooleanSettings.EDITOR_LINE_NUMBERS.set(isSelected);

            sourceCode.revalidate(); // added 16 Jan 2012 to assure label redrawn.
            // needed because caret disappears when checkbox clicked
            sourceCode.setCaretVisible(true);
            sourceCode.requestFocusInWindow();
        });

        JPanel editInfo = new JPanel(new BorderLayout());
        editInfo.add(caretPositionLabel, BorderLayout.WEST);
        editInfo.add(showLineNumbers, BorderLayout.CENTER);
        // sourceCode is responsible for its own scrolling
        add(sourceCode.getOuterComponent(), BorderLayout.CENTER);
        add(editInfo, BorderLayout.SOUTH);
    }

    /**
     * Form string with source code line numbers. Resulting string is in HTML,
     * for which JLabel will happily honor {@code <br />} instead of {@code \n}
     * to do multi-line label.<br />
     * The line number list is a JLabel with one line number per line.
     *
     * @return HTML formatted string representing the line numbers
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
     * Get file name with no path information. See java.io.File.getName()
     *
     * @return filename as a String
     */
    public String getFilename() {
        return file.getFileName().toString();
    }

    /**
     * Get full file pathname. See java.io.File.getPath()
     *
     * @return full pathname as a {@code String}
     */
    public String getPathname() {
        return file.toString();
    }

    /**
     * Get file parent pathname. See java.io.File.getParentDirectory()
     *
     * @return parent full pathname as a {@code String}
     */
    public String getParentDirectory() {
        return file.getParent().toString();
    }

    /**
     * Determine if file has been modified since last save or, if not yet saved,
     * since being created using New or Open.
     *
     * @return true if file has been modified since save or creation, false
     * otherwise.
     */
    public boolean hasUnsavedEdits() {
        return edited;
    }

    /**
     * Determine if file is "new", which means created using New but not yet
     * saved. If created using Open, it is not new.
     *
     * @return true if file was created using New and has not yet been saved,
     * false otherwise.
     */
    public boolean isNew() {
        return file.getRoot() == null;
    }

    /**
     * Delegates to text area's requestFocusInWindow method.
     */
    public void tellEditingComponentToRequestFocusInWindow() {
        sourceCode.requestFocusInWindow();
    }

    /**
     * Get the manager in charge of Undo and Redo operations
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
     The "set visible" operations are used because clicking on the toolbar
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
        Main.getGUI().updateUndoManager();
    }

    /**
     * Redo previous edit
     */
    public void redo() {
        sourceCode.redo();
        Main.getGUI().updateUndoManager();
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
     * @param position position of character
     * @return column and line coordinates as a {@link Point}
     */
    private Point convertStreamPositionToLineColumn(int position) {
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
    private int convertLineColumnToStreamPosition(int line, int column) {
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
        sourceCode.setFont(Settings.FontSettings.EDITOR_FONT.get());
        sourceCode.setLineHighlightEnabled(Settings.BooleanSettings.EDITOR_CURRENT_LINE_HIGHLIGHTING.isSet());
        sourceCode.setCaretBlinkRate(Settings.IntegerSettings.CARET_BLINK_RATE.get());
        sourceCode.setTabSize(Settings.IntegerSettings.EDITOR_TAB_SIZE.get());
        sourceCode.updateSyntaxStyles();
        sourceCode.revalidate();
        // We want line numbers to be displayed same size but always PLAIN style.
        // Easiest way to get same pixel height as source code is to set to same
        // font family as the source code! It can get a bit complicated otherwise
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
     * Saves the contents of this pane into its file.<p/>
     * If the latter doesn't exist, or a "Save As" behavior is requested, the
     * user will be asked for a valid pathname by means of a
     * {@link JFileChooser}.
     *
     * @param doRename if true, will force "Save As" behavior
     * @return true if the file has been successfully written, false otherwise
     */
    boolean save(boolean doRename) {

        if (isNew() || doRename) {

            //Setting up file chooser
            JFileChooser saveDialog = new JFileChooser(isNew()
                    ? Main.getGUI().editTabbedPane.getCurrentSaveDirectory()
                    : file.getParent().toString());
            saveDialog.setDialogTitle("Save As");
            if (!isNew())
                saveDialog.setSelectedFile(file.getFileName().toFile());

            boolean doSave = false;
            while (!doSave) {
                if (saveDialog.showSaveDialog(Main.getGUI().mainFrame) != JFileChooser.APPROVE_OPTION)
                    return false;

                Path newFilename = saveDialog.getSelectedFile().toPath();

                if (!Files.exists(newFilename)) {
                    file = newFilename;
                    doSave = true;
                }
                else switch (JOptionPane.showConfirmDialog(
                            Main.getGUI().mainFrame,
                            "File " + newFilename.getFileName() + " already exists.  Do you wish to overwrite it?",
                            "Overwrite existing file?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE)) {
                        case JOptionPane.YES_OPTION:
                            file = newFilename;
                            doSave = true;
                            break;
                        case JOptionPane.NO_OPTION:
                            break;
                        case JOptionPane.CANCEL_OPTION:
                            return false;
                        default:
                            throw new IllegalStateException("Unexpected exception: Illegal case on confirm dialog!");
                    }
            }
        }

        try {
            Files.write(file, sourceCode.getText().getBytes());
            edited = false;
        }
        catch (IOException ex) {
            Main.logger.log(Level.SEVERE, "Exception while writing file: " + file.toString(), ex);
            return false;
        }

        Main.getGUI().updateGUIState();
        return true;
    }

    /**
     * Uses the HardcopyWriter class developed by David Flanagan for the book
     * "Java Examples in a Nutshell". It will do basic printing of multi-page
     * text documents. It displays a print dialog but does not act on any
     * changes the user may have specified there, such as number of copies.
     */
    void print() {
        try (HardcopyWriter printer = new HardcopyWriter(Main.getGUI().mainFrame, getFilename(),
                10, .5, .5, .5, .5)) {

            UnaryOperator<String> mapper;
            if (showLineNumbers.isSelected()) {
                Iterator<String> lineNumber = Stream.iterate(1, n -> n + 1)
                        .limit(getSourceLineCount())
                        .map(n -> Integer.toString(n) + ": ")
                        .iterator();
                mapper = line -> lineNumber.next() + line + newline;
            }
            else mapper = line -> line + newline;

            Arrays.stream(getSource().split(newline + "", -1))
                    .sequential()
                    .map(mapper)
                    .forEach(line -> printer.write(line.toCharArray(), 0, line.length()));
        }
        catch (HardcopyWriter.PrintCanceledException ex) {
            // TODO
        }
    }
}
