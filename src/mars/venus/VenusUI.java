package mars.venus;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import mars.Main;
import mars.mips.dump.DumpFormatLoader;
import mars.settings.BooleanSettings;

/*
 Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

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
 * Top level container for Venus GUI.
 * <p/>
 * Heavily modified by Pete Sanderson, July 2004, to incorporate JSPIMMenu and
 * JSPIMToolbar not as subclasses of JMenuBar and JToolBar, but as instances of
 * them. They are both here primarily so both can share the Action objects.
 *
 * @author Sanderson and Team JSpim
 */
public final class VenusUI {

    final String baseTitle;

    /**
     * Reference of GUI main window
     */
    public final JFrame mainFrame;

    private final JToolBar toolbar;
    private final JSplitPane leftPane;
    final EditTabbedPane editTabbedPane;
    public final DataSegmentWindow dataSegment;
    public final TextSegmentWindow textSegment;
    final LabelsWindow labelValues;
    public final JDesktopPane executePane;
    final JTabbedPane registersPane;
    final RegistersWindow registersTab;
    public final Coprocessor1Window coprocessor1Tab;
    final Coprocessor0Window coprocessor0Tab;
    public final MessagesPane messagesPane;

    // components of the toolbar
    private final JButton marsMode;
    private final JButton New, Open, Save, SaveAs, Print;
    private final JButton Undo, Redo, Cut, Copy, Paste, FindReplace, SelectAll;
    private final JButton Run, Reset, Step, Backstep, Stop, Pause;
    private final JButton adjustInternalFrames, DumpMemory;
    private final JButton Help;

    // The "action" objects, which include action listeners.  One of each will be created then
    // shared between a menu item and its corresponding toolbar button.  This is a very cool
    // technique because it relates the button and menu item so closely
    private final GuiAction fileNewAction, fileOpenAction, fileCloseAction, fileCloseAllAction, fileSaveAction,
            fileSaveAsAction, fileSaveAllAction, filePrintAction, fileExitAction;
    private final GuiAction editCutAction, editCopyAction, editPasteAction, editUndoAction, editRedoAction,
            editFindReplaceAction, editSelectAllAction;
    private final AbstractAction runGoAction, runStepAction, runBackstepAction, runResetAction,
            runStopAction, runPauseAction, runToggleBreakpointsAction, runDumpMemoryAction;
    private final GuiAction settingsLabelAction, settingsPopupInputAction, settingsValueDisplayBaseAction, settingsAddressDisplayBaseAction,
            settingsExtendedAction, settingsAssembleOnOpenAction, settingsAssembleAllAction,
            settingsWarningsAreErrorsAction, settingsStartAtMainAction, settingsProgramArgumentsAction,
            settingsDelayedBranchingAction, settingsExceptionHandlerAction, settingsEditorAction,
            settingsHighlightingAction, settingsMemoryConfigurationAction, settingsSelfModifyingCodeAction;
    private final GuiAction helpHelpAction;
    private final RunClearBreakpointsAction runClearBreakpointsAction;

    /**
     * Constructor for the Class. Sets up a window object for the UI
     */
    public VenusUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Main.logger.log(Level.WARNING, "Could not set system LAF", ex);
        }

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Class<VenusUI> c = VenusUI.class;

        double screenWidth = toolkit.getScreenSize().getWidth();
        double screenHeight = toolkit.getScreenSize().getHeight();
        // basically give up some screen space if running at 800 x 600
        double messageWidthPct = (screenWidth < 1000.0) ? 0.67 : 0.73;
        double messageHeightPct = (screenWidth < 1000.0) ? 0.12 : 0.15;
        double mainWidthPct = (screenWidth < 1000.0) ? 0.67 : 0.73;
        double mainHeightPct = (screenWidth < 1000.0) ? 0.60 : 0.65;
        double registersWidthPct = (screenWidth < 1000.0) ? 0.18 : 0.22;
        double registersHeightPct = (screenWidth < 1000.0) ? 0.72 : 0.80;

        Dimension messagesPanePreferredSize = new Dimension((int) (screenWidth * messageWidthPct), (int) (screenHeight * messageHeightPct));
        Dimension mainPanePreferredSize = new Dimension((int) (screenWidth * mainWidthPct), (int) (screenHeight * mainHeightPct));
        Dimension registersPanePreferredSize = new Dimension((int) (screenWidth * registersWidthPct), (int) (screenHeight * registersHeightPct));

        // the "restore" size (window control button that toggles with maximize)
        // I want to keep it large, with enough room for user to get handles
        //this.setSize((int)(screenWidth*.8),(int)(screenHeight*.8));
        mainFrame = new JFrame(baseTitle = "MARS " + Main.VERSION);

        // image courtesy of NASA/JPL.
        Image icon = toolkit.getImage(c.getResource(
                Main.imagesPath + "Mars_Icon_2_512x512x32.png"));
        mainFrame.setIconImage(icon);

        // Everything in frame will be arranged on JPanel "center", which is only frame component.
        // "center" has BorderLayout and 2 major components:
        //   -- panel (jp) on North with 2 components
        //      1. toolbar
        //      2. run speed slider.
        //   -- split pane (horizonSplitter) in center with 2 components side-by-side
        //      1. split pane (splitter) with 2 components stacked
        //         a. 2 alternating containers (edit, execute)
        //         b. messages pane with 2 tabs (mars, run I/O)
        //      2. registers pane with 3 tabs (register file, coproc 0, coproc 1)
        // I should probably run this breakdown out to full detail.  The components are created
        // roughly in bottom-up order; some are created in component constructors and thus are
        // not visible here.
        registersTab = new RegistersWindow();
        coprocessor1Tab = new Coprocessor1Window();
        coprocessor0Tab = new Coprocessor0Window();

        registersPane = new JTabbedPane();
        registersPane.addTab("Registers", registersTab);
        registersPane.addTab("Coproc 1", coprocessor1Tab);
        registersPane.addTab("Coproc 0", coprocessor0Tab);
        registersPane.setToolTipTextAt(0, "CPU registers");
        registersPane.setToolTipTextAt(1, "Coprocessor 1 registers (floating point unit)");
        registersPane.setToolTipTextAt(2, "Coprocessor 0 registers (exceptions and interrupts)");
        registersPane.setPreferredSize(registersPanePreferredSize);

        editTabbedPane = new EditTabbedPane();
        editTabbedPane.setPreferredSize(mainPanePreferredSize);

        textSegment = new TextSegmentWindow();
        textSegment.pack();
        textSegment.setVisible(true);

        dataSegment = new DataSegmentWindow();
        dataSegment.pack();
        dataSegment.setVisible(true);

        labelValues = new LabelsWindow();
        labelValues.pack();
        labelValues.setVisible(BooleanSettings.LABEL_WINDOW_VISIBILITY.isSet());

        executePane = new JDesktopPane();
        executePane.add(textSegment);  // these 3 LOC moved up.  DPS 3-Sept-2014
        executePane.add(dataSegment);
        executePane.add(labelValues);
        executePane.setPreferredSize(mainPanePreferredSize);
        setExecuteTabBounds();

        messagesPane = new MessagesPane();
        messagesPane.setPreferredSize(messagesPanePreferredSize);

        leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editTabbedPane, messagesPane);
        leftPane.setOneTouchExpandable(true);
        leftPane.resetToPreferredSizes();

        JSplitPane horizonSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, registersPane);
        horizonSplitter.setOneTouchExpandable(true);
        horizonSplitter.resetToPreferredSizes();

        //<editor-fold defaultstate="collapsed" desc="Action objects">
        /**
         * Action objects are used instead of action listeners because one can
         * be easily shared between a menu item and a toolbar button. They must
         * be constructed before the associated components.
         */
        fileNewAction = new GuiAction("New",
                c.getResource(Main.imagesPath + "New16.png"),
                c.getResource(Main.imagesPath + "New22.png"),
                "Create a new file for editing",
                KeyEvent.VK_N,
                KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::newFile);
        fileOpenAction = new GuiAction("Open...",
                c.getResource(Main.imagesPath + "Open16.png"),
                c.getResource(Main.imagesPath + "Open22.png"),
                "Open a file for editing",
                KeyEvent.VK_O,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::open);
        fileCloseAction = new GuiAction("Close",
                "Close the current file",
                KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::close);
        fileCloseAllAction = new GuiAction("Close all",
                "Close all open files",
                KeyEvent.VK_L,
                null,
                GuiAction::closeAll);
        fileSaveAction = new GuiAction("Save",
                c.getResource(Main.imagesPath + "Save16.png"),
                c.getResource(Main.imagesPath + "Save22.png"),
                "Save the current file",
                KeyEvent.VK_S,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::save);
        fileSaveAsAction = new GuiAction("Save as...",
                c.getResource(Main.imagesPath + "SaveAs16.png"),
                c.getResource(Main.imagesPath + "SaveAs22.png"),
                "Save current file with different name",
                KeyEvent.VK_A,
                null,
                GuiAction::saveAs);
        fileSaveAllAction = new GuiAction("Save all",
                "Save all open files",
                KeyEvent.VK_V,
                null,
                GuiAction::saveAll);
        filePrintAction = new GuiAction("Print...",
                c.getResource(Main.imagesPath + "Print16.gif"),
                c.getResource(Main.imagesPath + "Print22.gif"),
                "Print current file",
                KeyEvent.VK_P,
                null,
                GuiAction::print);
        fileExitAction = new GuiAction("Exit",
                "Exit Mars",
                KeyEvent.VK_X,
                null,
                GuiAction::exit);

        editUndoAction = new GuiAction("Undo",
                c.getResource(Main.imagesPath + "Undo16.png"),
                c.getResource(Main.imagesPath + "Undo22.png"),
                "Undo last edit",
                KeyEvent.VK_U,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::undo);
        editRedoAction = new GuiAction("Redo",
                c.getResource(Main.imagesPath + "Redo16.png"),
                c.getResource(Main.imagesPath + "Redo22.png"),
                "Redo last edit",
                KeyEvent.VK_R,
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::redo);
        editCutAction = new GuiAction("Cut",
                c.getResource(Main.imagesPath + "Cut16.gif"),
                c.getResource(Main.imagesPath + "Cut22.gif"),
                "Cut",
                KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::cut);
        editCopyAction = new GuiAction("Copy",
                c.getResource(Main.imagesPath + "Copy16.png"),
                c.getResource(Main.imagesPath + "Copy22.png"),
                "Copy",
                KeyEvent.VK_O,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::copy);
        editPasteAction = new GuiAction("Paste",
                c.getResource(Main.imagesPath + "Paste16.png"),
                c.getResource(Main.imagesPath + "Paste22.png"),
                "Paste",
                KeyEvent.VK_P,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::paste);
        editFindReplaceAction = new GuiAction("Find/Replace",
                c.getResource(Main.imagesPath + "Find16.png"),
                c.getResource(Main.imagesPath + "Find22.png"),
                "Find/Replace",
                KeyEvent.VK_F,
                KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::findAndReplace);
        editSelectAllAction = new GuiAction("Select all",
                "Select All",
                KeyEvent.VK_A,
                KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::selectAll);

        runGoAction = new RunGoAction();
        runStepAction = new RunStepAction();
        runBackstepAction = new RunBackstepAction();
        runPauseAction = new RunPauseAction();
        runStopAction = new RunStopAction();
        runResetAction = new ExecuteAction();
        runClearBreakpointsAction = new RunClearBreakpointsAction();
        runToggleBreakpointsAction = new GuiAction("Toggle all breakpoints",
                "Disable/enable all breakpoints without clearing (can also click Bkpt column header)",
                KeyEvent.VK_T,
                KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::toggleBreakpoints);

        runDumpMemoryAction = new GuiAction("Dump Memory...",
                c.getResource(Main.imagesPath + "Dump16.png"),
                c.getResource(Main.imagesPath + "Dump22.png"),
                "Dump machine code or data in an available format",
                KeyEvent.VK_D,
                KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                GuiAction::dumpMemory);

        settingsLabelAction = new GuiAction("Show labels window (symbol table)",
                "Toggle visibility of Labels window (symbol table) in the Execute tab",
                GuiAction::toggleLabelWindow);
        settingsPopupInputAction = new GuiAction("Popup dialog for input syscalls (5,6,7,8,12)",
                "If set, use popup dialog for input syscalls (5,6,7,8,12) instead of cursor in Run I/O window",
                GuiAction::togglePopupInput);
        settingsValueDisplayBaseAction = new GuiAction("Values displayed in hexadecimal",
                "Toggle between hexadecimal and decimal display of memory/register values",
                GuiAction::toggleValueDisplayBase);
        settingsAddressDisplayBaseAction = new GuiAction("Addresses displayed in hexadecimal",
                "Toggle between hexadecimal and decimal display of memory addresses",
                GuiAction::toggleAddressDisplayBase);
        settingsExtendedAction = new GuiAction("Permit extended (pseudo) instructions and formats",
                "If set, MIPS extended (pseudo) instructions are formats are permitted.",
                GuiAction::toggleExtendedInstructionSet);
        settingsAssembleOnOpenAction = new GuiAction("Assemble file upon opening",
                "If set, a file will be automatically assembled as soon as it is opened.  File Open dialog will show most recently opened file.",
                GuiAction::toggleAssembleOnOpen);
        settingsAssembleAllAction = new GuiAction("Assemble all files in directory",
                "If set, all files in current directory will be assembled when Assemble operation is selected.",
                GuiAction::toggleAssembleAll);
        settingsWarningsAreErrorsAction = new GuiAction("Assembler warnings are considered errors",
                "If set, assembler warnings will be interpreted as errors and prevent successful assembly.",
                GuiAction::toggleWarningsAreErrors);
        settingsStartAtMainAction = new GuiAction("Initialize program counter to global 'main' if defined",
                "If set, assembler will initialize Program Counter to text address globally labeled 'main', if defined.",
                GuiAction::toggleStartAtMain);
        settingsProgramArgumentsAction = new GuiAction("Program arguments provided to MIPS program",
                "If set, program arguments for MIPS program can be entered in border of Text Segment window.",
                GuiAction::toggleProgramArguments);
        settingsDelayedBranchingAction = new GuiAction("Delayed branching",
                "If set, delayed branching will occur during MIPS execution.",
                GuiAction::toggleDelayedBranching);
        settingsSelfModifyingCodeAction = new GuiAction("Self-modifying code",
                "If set, the MIPS program can write and branch to both text and data segments.",
                GuiAction::toggleSelfModifyingCode);

        settingsEditorAction = new GuiAction("Editor...",
                "View and modify text editor settings.",
                GuiAction::editorSettings);
        settingsHighlightingAction = new GuiAction("Highlighting...",
                "View and modify Execute Tab highlighting colors",
                GuiAction::highlightingSettings);
        settingsExceptionHandlerAction = new GuiAction("Exception handler...",
                "If set, the specified exception handler file will be included in all Assemble operations.",
                GuiAction::exceptionHandlerSettings);
        settingsMemoryConfigurationAction = new GuiAction("Memory configuration...",
                "View and modify memory segment base addresses for simulated MIPS.",
                GuiAction::memoryConfigurationSettings);

        helpHelpAction = new GuiAction("Help",
                c.getResource(Main.imagesPath + "Help16.png"),
                c.getResource(Main.imagesPath + "Help22.png"),
                "Help",
                KeyEvent.VK_H,
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
                GuiAction::help);
        //</editor-fold>

        textSegment.registerTableModelListener(runClearBreakpointsAction);

        //<editor-fold defaultstate="collapsed" desc="Menu bar">
        /*
         * build the menus and connect them to action objects (which serve as action listeners
         * shared between menu item and corresponding toolbar icon).
         */
        JMenuBar menuBar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        JMenu edit = new JMenu("Edit");
        edit.setMnemonic(KeyEvent.VK_E);
        JMenu run = new JMenu("Run");
        run.setMnemonic(KeyEvent.VK_R);
        JMenu settings = new JMenu("Settings");
        settings.setMnemonic(KeyEvent.VK_S);
        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        // slight bug: user typing alt-H activates help menu item directly, not help menu

        file.add(new JMenuItem(fileNewAction));
        file.add(new JMenuItem(fileOpenAction));
        file.add(new JMenuItem(fileCloseAction));
        file.add(new JMenuItem(fileCloseAllAction));
        file.addSeparator();
        file.add(new JMenuItem(fileSaveAction));
        file.add(new JMenuItem(fileSaveAsAction));
        file.add(new JMenuItem(fileSaveAllAction));
        if (new DumpFormatLoader().loadDumpFormats().size() > 0)
            file.add(new JMenuItem(runDumpMemoryAction));
        file.addSeparator();
        file.add(new JMenuItem(filePrintAction));
        file.addSeparator();
        file.add(new JMenuItem(fileExitAction));

        edit.add(new JMenuItem(editUndoAction));
        edit.add(new JMenuItem(editRedoAction));
        edit.addSeparator();
        edit.add(new JMenuItem(editCutAction));
        edit.add(new JMenuItem(editCopyAction));
        edit.add(new JMenuItem(editPasteAction));
        edit.addSeparator();
        edit.add(new JMenuItem(editFindReplaceAction));
        edit.add(new JMenuItem(editSelectAllAction));

        run.add(new JMenuItem(runGoAction));
        run.add(new JMenuItem(runStepAction));
        run.add(new JMenuItem(runBackstepAction));
        run.add(new JMenuItem(runPauseAction));
        run.add(new JMenuItem(runStopAction));
        run.add(new JMenuItem(runResetAction));
        run.addSeparator();
        run.add(new JMenuItem(runClearBreakpointsAction));
        run.add(new JMenuItem(runToggleBreakpointsAction));

        JCheckBoxMenuItem settingsLabel = new JCheckBoxMenuItem(settingsLabelAction);
        settingsLabel.setSelected(BooleanSettings.LABEL_WINDOW_VISIBILITY.isSet());
        JCheckBoxMenuItem settingsPopupInput = new JCheckBoxMenuItem(settingsPopupInputAction);
        settingsPopupInput.setSelected(BooleanSettings.POPUP_SYSCALL_INPUT.isSet());
        JCheckBoxMenuItem settingsValueDisplayBase = new JCheckBoxMenuItem(settingsValueDisplayBaseAction);
        settingsValueDisplayBase.setSelected(BooleanSettings.DISPLAY_VALUES_IN_HEX.isSet());
        JCheckBoxMenuItem settingsAddressDisplayBase = new JCheckBoxMenuItem(settingsAddressDisplayBaseAction);
        settingsAddressDisplayBase.setSelected(BooleanSettings.DISPLAY_ADDRESSES_IN_HEX.isSet());
        JCheckBoxMenuItem settingsExtended = new JCheckBoxMenuItem(settingsExtendedAction);
        settingsExtended.setSelected(BooleanSettings.EXTENDED_ASSEMBLER.isSet());
        JCheckBoxMenuItem settingsDelayedBranching = new JCheckBoxMenuItem(settingsDelayedBranchingAction);
        settingsDelayedBranching.setSelected(BooleanSettings.DELAYED_BRANCHING.isSet());
        JCheckBoxMenuItem settingsSelfModifyingCode = new JCheckBoxMenuItem(settingsSelfModifyingCodeAction);
        settingsSelfModifyingCode.setSelected(BooleanSettings.SELF_MODIFYING_CODE.isSet());
        JCheckBoxMenuItem settingsAssembleOnOpen = new JCheckBoxMenuItem(settingsAssembleOnOpenAction);
        settingsAssembleOnOpen.setSelected(BooleanSettings.ASSEMBLE_ON_OPEN.isSet());
        JCheckBoxMenuItem settingsAssembleAll = new JCheckBoxMenuItem(settingsAssembleAllAction);
        settingsAssembleAll.setSelected(BooleanSettings.ASSEMBLE_ALL.isSet());
        JCheckBoxMenuItem settingsWarningsAreErrors = new JCheckBoxMenuItem(settingsWarningsAreErrorsAction);
        settingsWarningsAreErrors.setSelected(BooleanSettings.WARNINGS_ARE_ERRORS.isSet());
        JCheckBoxMenuItem settingsStartAtMain = new JCheckBoxMenuItem(settingsStartAtMainAction);
        settingsStartAtMain.setSelected(BooleanSettings.START_AT_MAIN.isSet());
        JCheckBoxMenuItem settingsProgramArguments = new JCheckBoxMenuItem(settingsProgramArgumentsAction);
        settingsProgramArguments.setSelected(BooleanSettings.PROGRAM_ARGUMENTS.isSet());

        settings.add(settingsLabel);
        settings.add(settingsProgramArguments);
        settings.add(settingsPopupInput);
        settings.add(settingsAddressDisplayBase);
        settings.add(settingsValueDisplayBase);
        settings.addSeparator();
        settings.add(settingsAssembleOnOpen);
        settings.add(settingsAssembleAll);
        settings.add(settingsWarningsAreErrors);
        settings.add(settingsStartAtMain);
        settings.addSeparator();
        settings.add(settingsExtended);
        settings.add(settingsDelayedBranching);
        settings.add(settingsSelfModifyingCode);
        settings.addSeparator();
        settings.add(new JMenuItem(settingsEditorAction));
        settings.add(new JMenuItem(settingsHighlightingAction));
        settings.add(new JMenuItem(settingsExceptionHandlerAction));
        settings.add(new JMenuItem(settingsMemoryConfigurationAction));

        JMenuItem aboutItem = new JMenuItem("About...");
        aboutItem.setToolTipText("Information about MARS");
        aboutItem.addActionListener((event) -> JOptionPane.showMessageDialog(mainFrame,
                "MARS " + Main.VERSION + "    Copyright " + Main.COPYRIGHT_YEARS + "\n"
                + Main.COPYRIGHT_HOLDERS + "\n"
                + "MARS is the Mips Assembler and Runtime Simulator.\n\n"
                + "Mars image courtesy of NASA/JPL.\n"
                + "Application icon taken from [PLACEHOLDER]\n"
                + "Toolbar and menu icons are from:\n"
                + "  *  Tango Desktop Project (tango.freedesktop.org),\n"
                + "  *  glyFX (www.glyfx.com) Common Toolbar Set,\n"
                + "  *  KDE-Look (www.kde-look.org) crystalline-blue-0.1,\n"
                + "  *  Icon-King (www.icon-king.com) Nuvola 1.0.\n"
                + "Print feature adapted from HardcopyWriter class in David Flanagan's\n"
                + "Java Examples in a Nutshell 3rd Edition, O'Reilly, ISBN 0-596-00620-9.",
                "About Mars",
                JOptionPane.INFORMATION_MESSAGE,
                icon == null ? null : new ImageIcon(icon)));

        help.add(new JMenuItem(helpHelpAction));
        help.addSeparator();
        help.add(aboutItem);

        menuBar.add(file);
        menuBar.add(edit);
        menuBar.add(run);
        menuBar.add(settings);
        JMenu toolMenu = new ToolLoader().buildToolsMenu();
        if (toolMenu != null) menuBar.add(toolMenu);
        menuBar.add(help);

        // experiment with popup menu for settings. 3 Aug 2006 PS
        //setupPopupMenu();
        mainFrame.setJMenuBar(menuBar);
        //</editor-fold>

        // Settings are defined beforehand - safe call
        dataSegment.getValueDisplayBaseChooser().setSettingsMenuItem(settingsValueDisplayBase);
        dataSegment.getAddressDisplayBaseChooser().setSettingsMenuItem(settingsAddressDisplayBase);

        //<editor-fold defaultstate="collapsed" desc="Tool bar">
        /*
         * build the toolbar and connect items to action objects (which serve as action listeners
         * shared between toolbar icon and corresponding menu item).
         */
        toolbar = new JToolBar();

        New = new JButton(fileNewAction);
        New.setText("");
        Open = new JButton(fileOpenAction);
        Open.setText("");
        Save = new JButton(fileSaveAction);
        Save.setText("");
        SaveAs = new JButton(fileSaveAsAction);
        SaveAs.setText("");
        Print = new JButton(filePrintAction);
        Print.setText("");

        Undo = new JButton(editUndoAction);
        Undo.setText("");
        Redo = new JButton(editRedoAction);
        Redo.setText("");
        Cut = new JButton(editCutAction);
        Cut.setText("");
        Copy = new JButton(editCopyAction);
        Copy.setText("");
        Paste = new JButton(editPasteAction);
        Paste.setText("");
        FindReplace = new JButton(editFindReplaceAction);
        FindReplace.setText("");
        SelectAll = new JButton(editSelectAllAction);
        SelectAll.setText("");

        Run = new JButton(runGoAction);
        Run.setText("");
        Step = new JButton(runStepAction);
        Step.setText("");
        Backstep = new JButton(runBackstepAction);
        Backstep.setText("");
        Reset = new JButton(runResetAction);
        Reset.setText("");
        Stop = new JButton(runStopAction);
        Stop.setText("");
        Pause = new JButton(runPauseAction);
        Pause.setText("");
        DumpMemory = new JButton(runDumpMemoryAction);
        DumpMemory.setText("");
        Help = new JButton(helpHelpAction);
        Help.setText("");

        adjustInternalFrames = new JButton("Adjust");
        adjustInternalFrames.addActionListener((event) -> setExecuteTabBounds());

        URL assembleIcon = c.getResource(Main.imagesPath + "Assemble22.png");
        marsMode = new JButton("Assemble", assembleIcon == null
                ? null
                : new ImageIcon(assembleIcon));
        marsMode.setToolTipText("View and control program execution");
        marsMode.addActionListener((event) -> toggleGUIMode());

        toolbar.add(marsMode);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(New);
        toolbar.add(Open);
        toolbar.add(Save);
        toolbar.add(SaveAs);
        toolbar.add(Print);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(Undo);
        toolbar.add(Redo);
        toolbar.add(Cut);
        toolbar.add(Copy);
        toolbar.add(Paste);
        toolbar.add(FindReplace);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(Help);

        toolbar.setFloatable(false);
        //</editor-fold>

        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jp.add(toolbar);

        mainFrame.getContentPane().add(jp, BorderLayout.NORTH);
        mainFrame.getContentPane().add(horizonSplitter);

        mainFrame.addWindowListener(new WindowAdapter() {
            // This is invoked when exiting the app through the X icon.  It will in turn
            // check for unsaved edits before exiting.
            @Override
            public void windowClosing(WindowEvent event) {
                if (editTabbedPane.closeAllFiles())
                    mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }
        });

        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        mainFrame.pack();
        updateGUIState();

        mainFrame.setVisible(true);
    }

    private void disableEditMenu() {
        editCutAction.setEnabled(false);
        editCopyAction.setEnabled(false);
        editPasteAction.setEnabled(false);
        editFindReplaceAction.setEnabled(false);
        editSelectAllAction.setEnabled(false);
        editRedoAction.setEnabled(false);
        editUndoAction.setEnabled(false);
    }

    private void disableRunMenu() {
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runClearBreakpointsAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(false);
        runDumpMemoryAction.setEnabled(false);
    }

    /* Use this upon successful assemble or reset
     */
    void setMenuStateRunnable() {
        marsMode.setEnabled(true);
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        runDumpMemoryAction.setEnabled(true);
        filePrintAction.setEnabled(true);
        fileExitAction.setEnabled(true);
        disableEditMenu();
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runGoAction.setEnabled(true);
        runStepAction.setEnabled(true);
        runBackstepAction.setEnabled(
                Main.isBackSteppingEnabled() && !Main.program.getBackStepper().empty()
        );
        runResetAction.setEnabled(true);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(true);
    }

    /* Use this while program is running
     */
    void setMenuStateRunning() {
        if (!executePane.isShowing())
            throw new IllegalStateException("Running MIPS Program from editor mode!");
        marsMode.setEnabled(false);
        fileNewAction.setEnabled(false);
        fileOpenAction.setEnabled(false);
        fileCloseAction.setEnabled(false);
        fileCloseAllAction.setEnabled(false);
        fileSaveAction.setEnabled(false);
        fileSaveAsAction.setEnabled(false);
        fileSaveAllAction.setEnabled(false);
        runDumpMemoryAction.setEnabled(false);
        filePrintAction.setEnabled(false);
        fileExitAction.setEnabled(false);
        settingsDelayedBranchingAction.setEnabled(false); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(false); // added 21 July 2009
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(true);
        runPauseAction.setEnabled(true);
        runToggleBreakpointsAction.setEnabled(false);
    }

    /* Use this upon completion of execution
     */
    void setMenuStateTerminated() {
        if (!executePane.isShowing())
            throw new IllegalStateException("Running MIPS Program from editor mode!");
        marsMode.setEnabled(true);
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        runDumpMemoryAction.setEnabled(true);
        filePrintAction.setEnabled(true);
        fileExitAction.setEnabled(true);
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(
                Main.isBackSteppingEnabled() && !Main.program.getBackStepper().empty()
        );
        runResetAction.setEnabled(true);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(true);
    }

    /**
     * Toggles between "Edit" and "Execute" mode, changing the toolbar buttons
     * and the main panel shown inside the GUI
     * <p/>
     * Going from "Edit" to "Execute" triggers an assemble action; if it fails,
     * then the mode isn't swapped
     */
    void toggleGUIMode() {
        if (leftPane.getTopComponent().equals(executePane)) {
            marsMode.setText("Assemble");
            marsMode.setToolTipText("View and control program execution");

            toolbar.removeAll();
            toolbar.add(marsMode);
            toolbar.add(new JToolBar.Separator());
            toolbar.add(New);
            toolbar.add(Open);
            toolbar.add(Save);
            toolbar.add(SaveAs);
            toolbar.add(Print);
            toolbar.add(new JToolBar.Separator());
            toolbar.add(Undo);
            toolbar.add(Redo);
            toolbar.add(Cut);
            toolbar.add(Copy);
            toolbar.add(Paste);
            toolbar.add(FindReplace);
            toolbar.add(new JToolBar.Separator());
            toolbar.add(Help);

            leftPane.setTopComponent(editTabbedPane);
        }
        else if (ExecuteAction.assemble()) {
            marsMode.setText("Edit");
            marsMode.setToolTipText("Edit MIPS program");

            toolbar.removeAll();
            toolbar.add(marsMode);
            toolbar.add(new JToolBar.Separator());
            toolbar.add(Run);
            toolbar.add(Step);
            toolbar.add(Backstep);
            toolbar.add(Pause);
            toolbar.add(Stop);
            toolbar.add(Reset);
            toolbar.add(new JToolBar.Separator());
            toolbar.add(adjustInternalFrames);
            toolbar.add(DumpMemory);
            toolbar.add(Help);
            toolbar.add(new JToolBar.Separator());
            toolbar.add(RunSpeedPanel.getInstance());

            leftPane.setTopComponent(executePane);
        }
        mainFrame.validate();
        updateGUIState();
    }

    /**
     * Send keyboard event to menu for possible processing. DPS 5-4-10
     *
     * @param evt KeyEvent for menu component to consider for processing.
     */
    public void dispatchEventToMenu(KeyEvent evt) {
        mainFrame.getMenuBar().dispatchEvent(evt);
    }

    /**
     * Updates the GUI according to the current simulation values
     */
    public final void simUpdate() {
        if (registersPane.getSelectedComponent() == registersTab)
            registersTab.updateRegisters();
        else
            coprocessor1Tab.updateRegisters();
        dataSegment.updateValues();
        textSegment.setCodeHighlighting(true);
        textSegment.highlightStepAtPC();
    }

    /**
     * Updates the whole GUI, including titles and actions, according to its
     * current state.
     * <p/>
     * Places name of file currently being edited into its edit tab and the
     * application's title bar. The edit tab will contain only the filename, the
     * title bar will contain full pathname. If file has been modified since
     * created, opened or saved, as indicated by value of the status parameter,
     * the name and path will be followed with an '*'. If newly-created file has
     * not yet been saved, the title bar will show only the (temporary) file
     * name.
     */
    void updateGUIState() {
        if (leftPane.getTopComponent().equals(editTabbedPane)) {

            fileNewAction.setEnabled(true);
            fileOpenAction.setEnabled(true);
            fileExitAction.setEnabled(true);
            settingsDelayedBranchingAction.setEnabled(true);
            settingsMemoryConfigurationAction.setEnabled(true);

            EditPane tab = editTabbedPane.getSelectedComponent();
            if (tab == null) {
                mainFrame.setTitle(baseTitle);

                // Manage GUI Components
                marsMode.setEnabled(false);
                fileCloseAction.setEnabled(false);
                fileCloseAllAction.setEnabled(false);
                fileSaveAction.setEnabled(false);
                fileSaveAsAction.setEnabled(false);
                fileSaveAllAction.setEnabled(false);
                filePrintAction.setEnabled(false);
                disableEditMenu();
                disableRunMenu();
            }
            else {
                String editMark = tab.hasUnsavedEdits() ? "* " : "  ";
                mainFrame.setTitle(tab.getPath() + editMark + " - " + baseTitle);
                editTabbedPane.setTitleAtComponent(tab.getPath().getFileName().toString() + editMark, tab);

                // Manage GUI Components
                marsMode.setEnabled(true);
                fileCloseAction.setEnabled(true);
                fileCloseAllAction.setEnabled(true);
                fileSaveAction.setEnabled(true);
                fileSaveAsAction.setEnabled(true);
                fileSaveAllAction.setEnabled(true);
                filePrintAction.setEnabled(true);
                editCutAction.setEnabled(true);
                editCopyAction.setEnabled(true);
                editPasteAction.setEnabled(true);
                editFindReplaceAction.setEnabled(true);
                editSelectAllAction.setEnabled(true);
                updateUndoManager();
                // If assemble-all, allow previous Run menu settings to remain.
                // Otherwise, clear them out.  DPS 9-Aug-2011
                if (tab.isNew() || tab.hasUnsavedEdits() || !BooleanSettings.ASSEMBLE_ALL.isSet())
                    disableRunMenu();
            }
        }
        else {
            //ExecutePane is showing
        }
    }

    public void updateUndoManager() {
        if (executePane.isShowing()) {
            editUndoAction.setEnabled(false);
            editRedoAction.setEnabled(false);
            return;
        }
        EditPane pane = editTabbedPane.getSelectedComponent();
        if (pane != null) {
            editUndoAction.setEnabled(pane.getUndoManager().canUndo());
            editRedoAction.setEnabled(pane.getUndoManager().canRedo());
        }
    }

    /**
     * This method will set the bounds of this JDesktopPane's internal windows
     * relative to the current size of this JDesktopPane. Such an operation
     * cannot be adequately done at constructor time because the actual size of
     * the desktop pane window is not yet established. Layout manager is not a
     * good option here because JDesktopPane does not work well with them (the
     * whole idea of using JDesktopPane with internal frames is to have
     * mini-frames that you can resize, move around, minimize, etc). This method
     * should be invoked only once: the first time the Execute tab is selected
     * (a change listener invokes it). We do not want it invoked on subsequent
     * tab selections; otherwise, user manipulations of the internal frames
     * would be lost the next time execute tab is selected.
     */
    public void setExecuteTabBounds() {
        Dimension d = executePane.getPreferredSize();
        Insets i = executePane.getInsets();

        int fullWidth = d.width - i.left - i.right;
        int fullHeight = d.height - i.top - i.bottom;
        int halfHeight = fullHeight / 2;
        dataSegment.setBounds(0, halfHeight + 1, fullWidth, halfHeight);
        if (labelValues.isVisible()) {
            textSegment.setBounds(0, 0, fullWidth * 3 / 4, halfHeight);
            labelValues.setBounds(fullWidth * 3 / 4 + 1, 0, fullWidth / 4, halfHeight);
        }
        else textSegment.setBounds(0, 0, fullWidth, halfHeight);
    }

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    // PLEASE PUT THESE TWO (& THEIR METHODS) SOMEWHERE THEY BELONG, NOT HERE
    private static boolean reset = true; // registers/memory reset for execution
    private static boolean started = false;  // started execution

    /**
     * To setStatusMenu whether the register values are reset.
     *
     * @param b BooleanSettings true if the register values have been reset.
     *
     */
    public static void setReset(boolean b) {
        reset = b;
    }

    /**
     * To setStatusMenu whether MIPS program execution has started.
     *
     * @param b true if the MIPS program execution has started.
     *
     */
    public static void setStarted(boolean b) {
        started = b;
    }

    /**
     * To find out whether the register values are reset.
     *
     * @return BooleanSettings true if the register values have been reset.
     *
     */
    public static boolean getReset() {
        return reset;
    }

    /**
     * To find out whether MIPS program is currently executing.
     *
     * @return true if MIPS program is currently executing.
     *
     */
    public static boolean getStarted() {
        return started;
    }

    // pop up menu experiment 3 Aug 2006.  Keep for possible later revival.
    private void setupPopupMenu() {
        JPopupMenu popup;
        popup = new JPopupMenu();
        // cannot put the same menu item object on two different menus.
        // If you want to duplicate functionality, need a different item.
        // Should be able to share listeners, but if both menu items are
        // JCheckBoxMenuItem, how to keep their checked status in synch?
        // If you popup this menu and check the box, the right action occurs
        // but its counterpart on the regular menu is not checked.
        popup.add(new JCheckBoxMenuItem(settingsLabelAction));
        //Add listener to components that can bring up popup menus. 
        mainFrame.addMouseListener(new PopupListener(popup));
    }
}
