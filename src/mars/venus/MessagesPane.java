package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ArrayBlockingQueue;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.NavigationFilter;
import javax.swing.text.NavigationFilter.FilterBypass;
import javax.swing.text.Position.Bias;
import javax.swing.undo.UndoableEdit;
import mars.ErrorList;
import mars.Main;
import mars.simulator.Simulator;

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
 * Creates the message window at the bottom of the UI.
 *
 * @author Team JSpim
 *
 */
public class MessagesPane extends JTabbedPane {

    JTextArea assembleArea, runArea;
    public JPanel assembleTab, runTab;
    // These constants are designed to keep scrolled contents of the 
    // two message areas from becoming overwhelmingly large (which
    // seems to slow things down as new text is appended).  Once it
    // reaches MAXIMUM_SCROLLED_CHARACTERS in length then cut off 
    // the first NUMBER_OF_CHARACTERS_TO_CUT characters.  The latter
    // must obviously be smaller than the former.
    public static final int MAXIMUM_SCROLLED_CHARACTERS = Main.maximumMessageCharacters;
    public static final int NUMBER_OF_CHARACTERS_TO_CUT = Main.maximumMessageCharacters / 10; // 10%

    /**
     * Constructor for the class, sets up two fresh tabbed text areas for
     * program feedback.
     */
    public MessagesPane() {
        super();
        setMinimumSize(new Dimension(0, 0));
        assembleArea = new JTextArea();
        runArea = new JTextArea();
        assembleArea.setEditable(false);
        runArea.setEditable(false);
        // Set both text areas to mono font.  For assembleArea pane, will make
        // messages more readable.  For runArea pane, will allow properly aligned
        // "text graphics" - DPS 15 Dec 2008
        Font monoFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        assembleArea.setFont(monoFont);
        runArea.setFont(monoFont);

        JButton assembleTabClearButton = new JButton("Clear");
        assembleTabClearButton.setToolTipText("Clear the Mars Messages area");
        assembleTabClearButton.addActionListener((ActionEvent e) -> {
            assembleArea.setText("");
        });
        assembleTab = new JPanel(new BorderLayout());
        assembleTab.add(createBoxForButton(assembleTabClearButton), BorderLayout.WEST);
        assembleTab.add(new JScrollPane(assembleArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        assembleArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String text;
                int lineStart = 0;
                int lineEnd = 0;
                try {
                    int line = assembleArea.getLineOfOffset(assembleArea.viewToModel(e.getPoint()));
                    lineStart = assembleArea.getLineStartOffset(line);
                    lineEnd = assembleArea.getLineEndOffset(line);
                    text = assembleArea.getText(lineStart, lineEnd - lineStart);
                }
                catch (BadLocationException ble) {
                    text = "";
                }
                if (text.length() > 0)
                    // If error or warning, parse out the line and column number.
                    if (text.startsWith(ErrorList.ERROR_MESSAGE_PREFIX) || text.startsWith(ErrorList.WARNING_MESSAGE_PREFIX)) {
                        assembleArea.select(lineStart, lineEnd);
                        assembleArea.setSelectionColor(Color.YELLOW);
                        assembleArea.repaint();
                        int separatorPosition = text.indexOf(ErrorList.MESSAGE_SEPARATOR);
                        if (separatorPosition >= 0)
                            text = text.substring(0, separatorPosition);
                        String[] stringTokens = text.split("\\s"); // tokenize with whitespace delimiter
                        String lineToken = ErrorList.LINE_PREFIX.trim();
                        String columnToken = ErrorList.POSITION_PREFIX.trim();
                        String lineString = "";
                        String columnString = "";
                        for (int i = 0; i < stringTokens.length; i++) {
                            if (stringTokens[i].equals(lineToken) && i < stringTokens.length - 1)
                                lineString = stringTokens[i + 1];
                            if (stringTokens[i].equals(columnToken) && i < stringTokens.length - 1)
                                columnString = stringTokens[i + 1];
                        }
                        int line;
                        int column;
                        try {
                            line = Integer.parseInt(lineString);
                        }
                        catch (NumberFormatException nfe) {
                            line = 0;
                        }
                        try {
                            column = Integer.parseInt(columnString);
                        }
                        catch (NumberFormatException nfe) {
                            column = 0;
                        }
                        // everything between FILENAME_PREFIX and LINE_PREFIX is filename.
                        int fileNameStart = text.indexOf(ErrorList.FILENAME_PREFIX) + ErrorList.FILENAME_PREFIX.length();
                        int fileNameEnd = text.indexOf(ErrorList.LINE_PREFIX);
                        String fileName = "";
                        if (fileNameStart < fileNameEnd && fileNameStart >= ErrorList.FILENAME_PREFIX.length())
                            fileName = text.substring(fileNameStart, fileNameEnd).trim();
                        if (fileName != null && fileName.length() > 0) {
                            Main.getGUI().editTabbedPane.selectEditorTextLine(fileName, line, column);
                            selectErrorMessage(fileName, line, column);
                        }
                    }
            }
        });

        JButton runTabClearButton = new JButton("Clear");
        runTabClearButton.setToolTipText("Clear the Run I/O area");
        runTabClearButton.addActionListener((ActionEvent e) -> {
            runArea.setText("");
        });
        runTab = new JPanel(new BorderLayout());
        runTab.add(createBoxForButton(runTabClearButton), BorderLayout.WEST);
        runTab.add(new JScrollPane(runArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        this.addTab("Mars Messages", assembleTab);
        this.addTab("Run I/O", runTab);
        this.setToolTipTextAt(0, "Messages produced by Run menu. Click on assemble error message to select erroneous line");
        this.setToolTipTextAt(1, "Simulated MIPS console input and output");
    }

    // Center given button in a box, centered vertically and 6 pixels on left and right
    private Box createBoxForButton(JButton button) {
        Box buttonRow = Box.createHorizontalBox();
        buttonRow.add(Box.createHorizontalStrut(6));
        buttonRow.add(button);
        buttonRow.add(Box.createHorizontalStrut(6));
        Box buttonBox = Box.createVerticalBox();
        buttonBox.add(Box.createVerticalGlue());
        buttonBox.add(buttonRow);
        buttonBox.add(Box.createVerticalGlue());
        return buttonBox;
    }

    /**
     * Will select the Mars Messages tab error message that matches the given
     * specifications, if it is found. Matching is done by constructing a string
     * using the parameter values and searching the text area for the last
     * occurrence of that string.
     *
     * @param fileName A String containing the file path name.
     * @param line Line number for error message
     * @param column Column number for error message
     */
    public void selectErrorMessage(String fileName, int line, int column) {
        String errorReportSubstring = new java.io.File(fileName).getName() + ErrorList.LINE_PREFIX + line + ErrorList.POSITION_PREFIX + column;
        int textPosition = assembleArea.getText().lastIndexOf(errorReportSubstring);
        if (textPosition >= 0) {
            int textLine;
            int lineStart;
            int lineEnd;
            try {
                textLine = assembleArea.getLineOfOffset(textPosition);
                lineStart = assembleArea.getLineStartOffset(textLine);
                lineEnd = assembleArea.getLineEndOffset(textLine);
                assembleArea.setSelectionColor(Color.YELLOW);
                assembleArea.select(lineStart, lineEnd);
                assembleArea.getCaret().setSelectionVisible(true);
                assembleArea.repaint();
            }
            catch (BadLocationException ble) {
                // If there is a problem, simply skip the selection
            }
        }
    }

    /**
     * Post a message to the assembler display
     *
     * @param message String to append to assembler display text
     */
    public void postMarsMessage(String message) {
        assembleArea.append(message);
        // can do some crude cutting here.  If the document gets "very large", 
        // let's cut off the oldest text. This will limit scrolling but the limit 
        // can be set reasonably high.
        if (assembleArea.getDocument().getLength() > MAXIMUM_SCROLLED_CHARACTERS)
            try {
                assembleArea.getDocument().remove(0, NUMBER_OF_CHARACTERS_TO_CUT);
            }
            catch (BadLocationException ble) {
                // only if NUMBER_OF_CHARACTERS_TO_CUT > MAXIMUM_SCROLLED_CHARACTERS
            }
        assembleArea.setCaretPosition(assembleArea.getDocument().getLength());
        setSelectedComponent(assembleTab);
    }

    /**
     * Post a message to the runtime display
     *
     * @param message String to append to runtime display text
     */
    // The work of this method is done by "invokeLater" because
    // its JTextArea is maintained by the main event thread
    // but also used, via this method, by the execution thread for 
    // "print" syscalls. "invokeLater" schedules the code to be
    // runArea under the event-processing thread no matter what.
    // DPS, 23 Aug 2005.
    public void postRunMessage(final String message) {
        SwingUtilities.invokeLater(() -> {
            setSelectedComponent(runTab);
            runArea.append(message);
            // can do some crude cutting here.  If the document gets "very large",
            // let's cut off the oldest text. This will limit scrolling but the limit
            // can be set reasonably high.
            if (runArea.getDocument().getLength() > MAXIMUM_SCROLLED_CHARACTERS)
                try {
                    runArea.getDocument().remove(0, NUMBER_OF_CHARACTERS_TO_CUT);
                }
                catch (BadLocationException ble) {
                    // only if NUMBER_OF_CHARACTERS_TO_CUT > MAXIMUM_SCROLLED_CHARACTERS
                }
        });
    }

    /**
     * Make the assembler message tab current (up front)
     */
    public void selectMarsMessageTab() {
        setSelectedComponent(assembleTab);
    }

    /**
     * Make the runtime message tab current (up front)
     */
    public void selectRunMessageTab() {
        setSelectedComponent(runTab);
    }

    /**
     * Method used by the SystemIO class to get interactive user input requested
     * by a running MIPS program (e.g. syscall #5 to read an integer). SystemIO
     * knows whether simulator is being runArea at command line by the user, or
     * by the GUI. If runArea at command line, it gets input from System.in
     * rather than here.
     *
     * This is an overloaded method. This version, with the String parameter, is
     * used to get input from a popup dialog.
     *
     * @param prompt Prompt to display to the user.
     * @return User input.
     */
    public String getInputString(String prompt) {
        String input;
        JOptionPane pane = new JOptionPane(prompt, JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION);
        pane.setWantsInput(true);
        JDialog dialog = pane.createDialog(Main.getGUI().mainFrame, "MIPS Keyboard Input");
        dialog.setVisible(true);
        input = (String) pane.getInputValue();
        this.postRunMessage(Main.userInputAlert + input + "\n");
        return input;
    }

    /**
     * Method used by the SystemIO class to get interactive user input requested
     * by a running MIPS program (e.g. syscall #5 to read an integer). SystemIO
     * knows whether simulator is being runArea at command line by the user, or
     * by the GUI. If runArea at command line, it gets input from System.in
     * rather than here.
     *
     * This is an overloaded method. This version, with the int parameter, is
     * used to get input from the MARS Run I/O window.
     *
     * @param maxLen: maximum length of input. This method returns when maxLen
     * characters have been read. Use -1 for no length restrictions.
     * @return User input.
     */
    public String getInputString(int maxLen) {
        Asker asker = new Asker(maxLen); // Asker defined immediately below.
        return asker.response();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Thread class for obtaining user input in the Run I/O window (MessagesPane)
    // Written by Ricardo Fernández Pascual [rfernandez@ditec.um.es] December 2009.
    class Asker implements Runnable {

        ArrayBlockingQueue<String> resultQueue = new ArrayBlockingQueue<>(1);
        int initialPos;
        int maxLen;

        Asker(int maxLen) {
            this.maxLen = maxLen;
            // initialPos will be set in runArea()
        }
        final DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                EventQueue.invokeLater(() -> {
                    try {
                        String inserted = e.getDocument().getText(e.getOffset(), e.getLength());
                        int i = inserted.indexOf('\n');
                        if (i >= 0) {
                            int offset = e.getOffset() + i;
                            if (offset + 1 == e.getDocument().getLength())
                                returnResponse();
                            else {
                                // remove the '\n' and put it at the end
                                e.getDocument().remove(offset, 1);
                                e.getDocument().insertString(e.getDocument().getLength(), "\n", null);
                                // insertUpdate will be called again, since we have inserted the '\n' at the end
                            }
                        }
                        else if (maxLen >= 0 && e.getDocument().getLength() - initialPos >= maxLen)
                            returnResponse();
                    }
                    catch (BadLocationException ex) {
                        returnResponse();
                    }
                });
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                EventQueue.invokeLater(() -> {
                    if ((e.getDocument().getLength() < initialPos || e.getOffset() < initialPos) && e instanceof UndoableEdit) {
                        ((UndoableEdit) e).undo();
                        runArea.setCaretPosition(e.getOffset() + e.getLength());
                    }
                });
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };
        final NavigationFilter navigationFilter = new NavigationFilter() {
            @Override
            public void moveDot(FilterBypass fb, int dot, Bias bias) {
                if (dot < initialPos)
                    dot = Math.min(initialPos, runArea.getDocument().getLength());
                fb.moveDot(dot, bias);
            }

            @Override
            public void setDot(FilterBypass fb, int dot, Bias bias) {
                if (dot < initialPos)
                    dot = Math.min(initialPos, runArea.getDocument().getLength());
                fb.setDot(dot, bias);
            }
        };
        final Simulator.StopListener stopListener = (Simulator s) -> {
            returnResponse();
        };

        @Override
        public void run() { // must be invoked from the GUI thread
            setSelectedComponent(runTab);
            runArea.setEditable(true);
            runArea.requestFocusInWindow();
            runArea.setCaretPosition(runArea.getDocument().getLength());
            initialPos = runArea.getCaretPosition();
            runArea.setNavigationFilter(navigationFilter);
            runArea.getDocument().addDocumentListener(listener);
            Simulator.getInstance().addStopListener(stopListener);
        }

        void returnResponse() {
            try {
                int p = Math.min(initialPos, runArea.getDocument().getLength());
                int l = Math.min(runArea.getDocument().getLength() - p, maxLen >= 0 ? maxLen : Integer.MAX_VALUE);
                resultQueue.offer(runArea.getText(p, l));
            }
            catch (BadLocationException ex) {
                // this cannot happen
                resultQueue.offer("");
            }
        }

        String response() {
            EventQueue.invokeLater(this);
            try {
                return resultQueue.take();
            }
            catch (InterruptedException ex) {
                return null;
            }
            finally {
                EventQueue.invokeLater(() -> {
                    runArea.getDocument().removeDocumentListener(listener);
                    runArea.setEditable(false);
                    runArea.setNavigationFilter(null);
                    runArea.setCaretPosition(runArea.getDocument().getLength());
                    Simulator.getInstance().removeStopListener(stopListener);
                });
            }
        }
    }
    // Asker class
    ////////////////////////////////////////////////////////////////////////////
}
