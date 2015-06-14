package mars.venus;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
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
import mars.Settings;
import mars.mips.dump.DumpFormatLoader;

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
 *
 * @author Sanderson and Team JSpim
 */
/* Heavily modified by Pete Sanderson, July 2004, to incorporate JSPIMMenu and JSPIMToolbar
 * not as subclasses of JMenuBar and JToolBar, but as instances of them.  They are both
 * here primarily so both can share the Action objects.
 */
public final class VenusUI {

    final String baseTitle;

    public final JFrame mainFrame;
    final JMenuBar menuBar;
    final JToolBar toolBar;
    private final JPanel mainPane;
    final EditTabbedPane editTabbedPane;
    public final ExecutePane executePane;
    final JTabbedPane registersPane;
    final RegistersWindow registersTab;
    public final Coprocessor1Window coprocessor1Tab;
    final Coprocessor0Window coprocessor0Tab;
    public final MessagesPane messagesPane;

    // components of the menubar
    private final JMenu file, run, help, edit, settings;
    private final JMenuItem fileNew, fileOpen, fileClose, fileCloseAll, fileSave, fileSaveAs, fileSaveAll, fileDumpMemory, filePrint, fileExit;
    private final JMenuItem editUndo, editRedo, editCut, editCopy, editPaste, editFindReplace, editSelectAll;
    private final JMenuItem runGo, runStep, runBackstep, runReset, runStop, runPause, runClearBreakpoints, runToggleBreakpoints;
    private final JCheckBoxMenuItem settingsLabel, settingsPopupInput, settingsValueDisplayBase, settingsAddressDisplayBase,
            settingsExtended, settingsAssembleOnOpen, settingsAssembleAll, settingsWarningsAreErrors, settingsStartAtMain,
            settingsDelayedBranching, settingsProgramArguments, settingsSelfModifyingCode;
    private final JMenuItem settingsExceptionHandler, settingsEditor, settingsHighlighting, settingsMemoryConfiguration;
    private final JMenuItem helpHelp;

    // components of the toolbar
    private final JButton marsMode;
    private final JButton Undo, Redo, Cut, Copy, Paste, FindReplace, SelectAll;
    private final JButton New, Open, Save, SaveAs, /*SaveAll,*/ Print;
    private final JButton Run, Reset, Step, Backstep, Stop, Pause;
    private final JButton adjustInternalFrames, DumpMemory;
    private final JButton Help;

    // The "action" objects, which include action listeners.  One of each will be created then
    // shared between a menu item and its corresponding toolbar button.  This is a very cool
    // technique because it relates the button and menu item so closely
    private GuiAction fileNewAction, fileOpenAction, fileCloseAction, fileCloseAllAction, fileSaveAction,
            fileSaveAsAction, fileSaveAllAction, filePrintAction, fileExitAction;

    private GuiAction editCutAction, editCopyAction, editPasteAction, editUndoAction, editRedoAction,
            editFindReplaceAction, editSelectAllAction;

    private Action runGoAction, runStepAction, runBackstepAction, runResetAction,
            runStopAction, runPauseAction, runClearBreakpointsAction, runToggleBreakpointsAction, fileDumpMemoryAction;
    private Action settingsLabelAction, settingsPopupInputAction, settingsValueDisplayBaseAction, settingsAddressDisplayBaseAction,
            settingsExtendedAction, settingsAssembleOnOpenAction, settingsAssembleAllAction,
            settingsWarningsAreErrorsAction, settingsStartAtMainAction, settingsProgramArgumentsAction,
            settingsDelayedBranchingAction, settingsExceptionHandlerAction, settingsEditorAction,
            settingsHighlightingAction, settingsMemoryConfigurationAction, settingsSelfModifyingCodeAction;
    private Action helpHelpAction;

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
        // I want to keep it large, with enough room for user to getStatus handles
        //this.setSize((int)(screenWidth*.8),(int)(screenHeight*.8));
        mainFrame = new JFrame(baseTitle = "MARS " + Main.version);

        //  image courtesy of NASA/JPL.
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
        //         a. main pane, with 2 alternating panels (edit, execute)
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
        registersPane.setToolTipTextAt(1, "Coprocessor 1 (floating point unit) registers");
        registersPane.setToolTipTextAt(2, "Coprocessor 0 (exceptions and interrupts) registers");
        registersPane.setPreferredSize(registersPanePreferredSize);

        editTabbedPane = new EditTabbedPane();
        editTabbedPane.setPreferredSize(mainPanePreferredSize);

        executePane = new ExecutePane();
        executePane.setPreferredSize(mainPanePreferredSize);

        mainPane = new JPanel();
        mainPane.setPreferredSize(mainPanePreferredSize);
        mainPane.add(editTabbedPane);

        /* Listener has one specific purpose: when Execute tab is selected for the 
         * first time, set the bounds of its internal frames by invoking the
         * setWindowsBounds() method.  Once this occurs, listener removes itself!
         * We do NOT want to reset bounds each time Execute tab is selected.
         * See ExecutePane.setWindowsBounds documentation for more details.
         */
        mainPane.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                if (e.getChild() instanceof ExecutePane) {
                    executePane.setWindowBounds();
                    mainPane.removeContainerListener(this);
                }
            }
        });

        messagesPane = new MessagesPane();
        messagesPane.setPreferredSize(messagesPanePreferredSize);
        
        JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPane, messagesPane);
        splitter.setOneTouchExpandable(true);
        splitter.resetToPreferredSizes();
        
        JSplitPane horizonSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitter, registersPane);
        horizonSplitter.setOneTouchExpandable(true);
        horizonSplitter.resetToPreferredSizes();

        //<editor-fold defaultstate="collapsed" desc="Action objects">
        /*
         * Action objects are used instead of action listeners because one can be easily shared between
         * a menu item and a toolbar button.  Does nice things like disable both if the action is
         * disabled, etc.
         */
        // due to dependencies, do not setStatusMenu up menu/toolbar until now.
        try {
            fileNewAction = new GuiAction("New",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "New22.png"))),
                    "Create a new file for editing", KeyEvent.VK_N,
                    KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::newFile);
            fileOpenAction = new GuiAction("Open ...",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Open22.png"))),
                    "Open a file for editing", KeyEvent.VK_O,
                    KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::open);
            fileCloseAction = new GuiAction("Close", null,
                    "Close the current file", KeyEvent.VK_C,
                    KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::close);
            fileCloseAllAction = new GuiAction("Close All", null,
                    "Close all open files", KeyEvent.VK_L,
                    null, GuiAction::closeAll);
            fileSaveAction = new GuiAction("Save",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Save22.png"))),
                    "Save the current file", KeyEvent.VK_S,
                    KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::save);
            fileSaveAsAction = new GuiAction("Save as ...",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "SaveAs22.png"))),
                    "Save current file with different name", KeyEvent.VK_A,
                    null, GuiAction::saveAs);
            fileSaveAllAction = new GuiAction("Save All", null,
                    "Save all open files", KeyEvent.VK_V,
                    null, GuiAction::saveAll);
            filePrintAction = new GuiAction("Print ...",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Print22.gif"))),
                    "Print current file", KeyEvent.VK_P,
                    null, GuiAction::print);
            fileExitAction = new GuiAction("Exit", null,
                    "Exit Mars", KeyEvent.VK_X,
                    null, GuiAction::exit);

            editUndoAction = new GuiAction("Undo",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Undo22.png"))),
                    "Undo last edit", KeyEvent.VK_U,
                    KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::undo);
            editRedoAction = new GuiAction("Redo",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Redo22.png"))),
                    "Redo last edit", KeyEvent.VK_R,
                    KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::redo);
            editCutAction = new GuiAction("Cut",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Cut22.gif"))),
                    "Cut", KeyEvent.VK_C,
                    KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::cut);
            editCopyAction = new GuiAction("Copy",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Copy22.png"))),
                    "Copy", KeyEvent.VK_O,
                    KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::copy);
            editPasteAction = new GuiAction("Paste",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Paste22.png"))),
                    "Paste", KeyEvent.VK_P,
                    KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::paste);
            editFindReplaceAction = new GuiAction("Find/Replace",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Find22.png"))),
                    "Find/Replace", KeyEvent.VK_F,
                    KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::findAndReplace);
            editSelectAllAction = new GuiAction("Select All",
                    null, //new ImageIcon(tk.getImage(cs.getResource(Main.imagesPath+"Find22.png"))),
                    "Select All", KeyEvent.VK_A,
                    KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::selectAll);

            runGoAction = new RunGoAction("Go",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Play22.png"))),
                    "Run the current program", KeyEvent.VK_G,
                    KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                    this);
            runStepAction = new RunStepAction("Step",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "StepForward22.png"))),
                    "Run one step at a time", KeyEvent.VK_T,
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0),
                    this);
            runBackstepAction = new RunBackstepAction("Backstep",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "StepBack22.png"))),
                    "Undo the last step", KeyEvent.VK_B,
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0),
                    this);
            runPauseAction = new RunPauseAction("Pause",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Pause22.png"))),
                    "Pause the currently running program", KeyEvent.VK_P,
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0),
                    this);
            runStopAction = new RunStopAction("Stop",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Stop22.png"))),
                    "Stop the currently running program", KeyEvent.VK_S,
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0),
                    this);
            runResetAction = new ExecuteAction("Reset",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Reset22.png"))),
                    "Reset MIPS memory and registers", KeyEvent.VK_R,
                    KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0),
                    this);
            runClearBreakpointsAction = new RunClearBreakpointsAction("Clear all breakpoints",
                    null,
                    "Clears all execution breakpoints set since the last assemble.", KeyEvent.VK_K,
                    KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    this);
            runToggleBreakpointsAction = new GuiAction("Toggle all breakpoints",
                    null,
                    "Disable/enable all breakpoints without clearing (can also click Bkpt column header)", KeyEvent.VK_T,
                    KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::toggleBreakpoints);

            fileDumpMemoryAction = new GuiAction("Dump Memory ...",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Dump22.png"))),
                    "Dump machine code or data in an available format", KeyEvent.VK_D,
                    KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    GuiAction::dumpMemory);

            settingsLabelAction = new GuiAction("Show Labels Window (symbol table)",
                    null,
                    "Toggle visibility of Labels window (symbol table) in the Execute tab",
                    null, null,
                    GuiAction::toggleLabelWindow);
            settingsPopupInputAction = new GuiAction("Popup dialog for input syscalls (5,6,7,8,12)",
                    null,
                    "If set, use popup dialog for input syscalls (5,6,7,8,12) instead of cursor in Run I/O window",
                    null, null,
                    GuiAction::togglePopupInput);
            settingsValueDisplayBaseAction = new GuiAction("Values displayed in hexadecimal",
                    null,
                    "Toggle between hexadecimal and decimal display of memory/register values",
                    null, null,
                    GuiAction::toggleValueDisplayBase);
            settingsAddressDisplayBaseAction = new GuiAction("Addresses displayed in hexadecimal",
                    null,
                    "Toggle between hexadecimal and decimal display of memory addresses",
                    null, null,
                    GuiAction::toggleAddressDisplayBase);
            settingsExtendedAction = new GuiAction("Permit extended (pseudo) instructions and formats",
                    null,
                    "If set, MIPS extended (pseudo) instructions are formats are permitted.",
                    null, null,
                    GuiAction::toggleExtendedInstructionSet);
            settingsAssembleOnOpenAction = new GuiAction("Assemble file upon opening",
                    null,
                    "If set, a file will be automatically assembled as soon as it is opened.  File Open dialog will show most recently opened file.",
                    null, null,
                    GuiAction::toggleAssembleOnOpen);
            settingsAssembleAllAction = new GuiAction("Assemble all files in directory",
                    null,
                    "If set, all files in current directory will be assembled when Assemble operation is selected.",
                    null, null,
                    GuiAction::toggleAssembleAll);
            settingsWarningsAreErrorsAction = new GuiAction("Assembler warnings are considered errors",
                    null,
                    "If set, assembler warnings will be interpreted as errors and prevent successful assembly.",
                    null, null,
                    GuiAction::toggleWarningsAreErrors);
            settingsStartAtMainAction = new GuiAction("Initialize Program Counter to global 'main' if defined",
                    null,
                    "If set, assembler will initialize Program Counter to text address globally labeled 'main', if defined.",
                    null, null,
                    GuiAction::toggleStartAtMain);
            settingsProgramArgumentsAction = new GuiAction("Program arguments provided to MIPS program",
                    null,
                    "If set, program arguments for MIPS program can be entered in border of Text Segment window.",
                    null, null,
                    GuiAction::toggleProgramArguments);
            settingsDelayedBranchingAction = new GuiAction("Delayed branching",
                    null,
                    "If set, delayed branching will occur during MIPS execution.",
                    null, null,
                    GuiAction::toggleDelayedBranching);
            settingsSelfModifyingCodeAction = new GuiAction("Self-modifying code",
                    null,
                    "If set, the MIPS program can write and branch to both text and data segments.",
                    null, null,
                    GuiAction::toggleSelfModifyingCode);

            settingsEditorAction = new GuiAction("Editor...",
                    null,
                    "View and modify text editor settings.",
                    null, null,
                    GuiAction::editorSettings);
            settingsHighlightingAction = new GuiAction("Highlighting...",
                    null,
                    "View and modify Execute Tab highlighting colors",
                    null, null,
                    GuiAction::highlightingSettings);
            settingsExceptionHandlerAction = new GuiAction("Exception Handler...",
                    null,
                    "If set, the specified exception handler file will be included in all Assemble operations.",
                    null, null,
                    GuiAction::exceptionHandlerSettings);
            settingsMemoryConfigurationAction = new GuiAction("Memory Configuration...",
                    null,
                    "View and modify memory segment base addresses for simulated MIPS.",
                    null, null,
                    GuiAction::memoryConfigurationSettings);

            helpHelpAction = new GuiAction("Help",
                    new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Help22.png"))),
                    "Help", KeyEvent.VK_H,
                    KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
                    GuiAction::help);
        }
        catch (NullPointerException e) {
            Main.logger.log(Level.SEVERE, "Internal Error: images folder not found, or other null pointer exception while creating Action objects", e);
            System.exit(1);
        }
//</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Menu bar">
        /*
         * build the menus and connect them to action objects (which serve as action listeners
         * shared between menu item and corresponding toolbar icon).
         */
        menuBar = new JMenuBar();

        file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        edit = new JMenu("Edit");
        edit.setMnemonic(KeyEvent.VK_E);
        run = new JMenu("Run");
        run.setMnemonic(KeyEvent.VK_R);
        settings = new JMenu("Settings");
        settings.setMnemonic(KeyEvent.VK_S);
        help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        // slight bug: user typing alt-H activates help menu item directly, not help menu

        fileNew = new JMenuItem(fileNewAction);
        fileNew.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "New16.png"))));
        fileOpen = new JMenuItem(fileOpenAction);
        fileOpen.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Open16.png"))));
        fileClose = new JMenuItem(fileCloseAction);
        fileClose.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "MyBlank16.gif"))));
        fileCloseAll = new JMenuItem(fileCloseAllAction);
        fileCloseAll.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "MyBlank16.gif"))));
        fileSave = new JMenuItem(fileSaveAction);
        fileSave.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Save16.png"))));
        fileSaveAs = new JMenuItem(fileSaveAsAction);
        fileSaveAs.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "SaveAs16.png"))));
        fileSaveAll = new JMenuItem(fileSaveAllAction);
        fileSaveAll.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "MyBlank16.gif"))));
        fileDumpMemory = new JMenuItem(fileDumpMemoryAction);
        fileDumpMemory.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Dump16.png"))));
        filePrint = new JMenuItem(filePrintAction);
        filePrint.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Print16.gif"))));
        fileExit = new JMenuItem(fileExitAction);
        fileExit.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "MyBlank16.gif"))));
        file.add(fileNew);
        file.add(fileOpen);
        file.add(fileClose);
        file.add(fileCloseAll);
        file.addSeparator();
        file.add(fileSave);
        file.add(fileSaveAs);
        file.add(fileSaveAll);
        if (new DumpFormatLoader().loadDumpFormats().size() > 0)
            file.add(fileDumpMemory);
        file.addSeparator();
        file.add(filePrint);
        file.addSeparator();
        file.add(fileExit);

        editUndo = new JMenuItem(editUndoAction);
        editUndo.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Undo16.png"))));//"Undo16.gif"))));
        editRedo = new JMenuItem(editRedoAction);
        editRedo.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Redo16.png"))));//"Redo16.gif"))));
        editCut = new JMenuItem(editCutAction);
        editCut.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Cut16.gif"))));
        editCopy = new JMenuItem(editCopyAction);
        editCopy.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Copy16.png"))));//"Copy16.gif"))));
        editPaste = new JMenuItem(editPasteAction);
        editPaste.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Paste16.png"))));//"Paste16.gif"))));
        editFindReplace = new JMenuItem(editFindReplaceAction);
        editFindReplace.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Find16.png"))));//"Paste16.gif"))));
        editSelectAll = new JMenuItem(editSelectAllAction);
        editSelectAll.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "MyBlank16.gif"))));
        edit.add(editUndo);
        edit.add(editRedo);
        edit.addSeparator();
        edit.add(editCut);
        edit.add(editCopy);
        edit.add(editPaste);
        edit.addSeparator();
        edit.add(editFindReplace);
        edit.add(editSelectAll);

        runGo = new JMenuItem(runGoAction);
        runGo.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Play16.png"))));
        runStep = new JMenuItem(runStepAction);
        runStep.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "StepForward16.png"))));
        runBackstep = new JMenuItem(runBackstepAction);
        runBackstep.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "StepBack16.png"))));
        runReset = new JMenuItem(runResetAction);
        runReset.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Reset16.png"))));
        runStop = new JMenuItem(runStopAction);
        runStop.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Stop16.png"))));
        runPause = new JMenuItem(runPauseAction);
        runPause.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Pause16.png"))));
        runClearBreakpoints = new JMenuItem(runClearBreakpointsAction);
        runClearBreakpoints.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "MyBlank16.gif"))));
        runToggleBreakpoints = new JMenuItem(runToggleBreakpointsAction);
        runToggleBreakpoints.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "MyBlank16.gif"))));

        //run.add(runAssemble);
        run.add(runGo);
        run.add(runStep);
        run.add(runBackstep);
        run.add(runPause);
        run.add(runStop);
        run.add(runReset);
        run.addSeparator();
        run.add(runClearBreakpoints);
        run.add(runToggleBreakpoints);

        settingsLabel = new JCheckBoxMenuItem(settingsLabelAction);
        settingsLabel.setSelected(Settings.BooleanSettings.LABEL_WINDOW_VISIBILITY.isSet());
//        settingsLabel.setSelected(Main.getSettings().getLabelWindowVisibility());
        settingsPopupInput = new JCheckBoxMenuItem(settingsPopupInputAction);
        settingsPopupInput.setSelected(Settings.BooleanSettings.POPUP_SYSCALL_INPUT.isSet());
//        settingsPopupInput.setSelected(Main.getSettings().getBool(Settings.POPUP_SYSCALL_INPUT));
        settingsValueDisplayBase = new JCheckBoxMenuItem(settingsValueDisplayBaseAction);
        settingsValueDisplayBase.setSelected(Settings.BooleanSettings.DISPLAY_VALUES_IN_HEX.isSet());
//        settingsValueDisplayBase.setSelected(Main.getSettings().getDisplayValuesInHex());//mainPane.getExecutePane().getValueDisplayBaseChooser().isSelected());
        // Tell the corresponding JCheckBox in the Execute Pane about me -- it has already been created.
        executePane.getValueDisplayBaseChooser().setSettingsMenuItem(settingsValueDisplayBase);
        settingsAddressDisplayBase = new JCheckBoxMenuItem(settingsAddressDisplayBaseAction);
        settingsAddressDisplayBase.setSelected(Settings.BooleanSettings.DISPLAY_ADDRESSES_IN_HEX.isSet());//mainPane.getExecutePane().getValueDisplayBaseChooser().isSelected());
        // Tell the corresponding JCheckBox in the Execute Pane about me -- it has already been created.
        executePane.getAddressDisplayBaseChooser().setSettingsMenuItem(settingsAddressDisplayBase);
        settingsExtended = new JCheckBoxMenuItem(settingsExtendedAction);
        settingsExtended.setSelected(Settings.BooleanSettings.EXTENDED_ASSEMBLER.isSet());
        settingsDelayedBranching = new JCheckBoxMenuItem(settingsDelayedBranchingAction);
        settingsDelayedBranching.setSelected(Settings.BooleanSettings.DELAYED_BRANCHING.isSet());
        settingsSelfModifyingCode = new JCheckBoxMenuItem(settingsSelfModifyingCodeAction);
        settingsSelfModifyingCode.setSelected(Settings.BooleanSettings.SELF_MODIFYING_CODE.isSet());
        settingsAssembleOnOpen = new JCheckBoxMenuItem(settingsAssembleOnOpenAction);
        settingsAssembleOnOpen.setSelected(Settings.BooleanSettings.ASSEMBLE_ON_OPEN.isSet());
        settingsAssembleAll = new JCheckBoxMenuItem(settingsAssembleAllAction);
        settingsAssembleAll.setSelected(Settings.BooleanSettings.ASSEMBLE_ALL.isSet());
        settingsWarningsAreErrors = new JCheckBoxMenuItem(settingsWarningsAreErrorsAction);
        settingsWarningsAreErrors.setSelected(Settings.BooleanSettings.WARNINGS_ARE_ERRORS.isSet());
        settingsStartAtMain = new JCheckBoxMenuItem(settingsStartAtMainAction);
        settingsStartAtMain.setSelected(Settings.BooleanSettings.START_AT_MAIN.isSet());
        settingsProgramArguments = new JCheckBoxMenuItem(settingsProgramArgumentsAction);
        settingsProgramArguments.setSelected(Settings.BooleanSettings.PROGRAM_ARGUMENTS.isSet());
        settingsEditor = new JMenuItem(settingsEditorAction);
        settingsHighlighting = new JMenuItem(settingsHighlightingAction);
        settingsExceptionHandler = new JMenuItem(settingsExceptionHandlerAction);
        settingsMemoryConfiguration = new JMenuItem(settingsMemoryConfigurationAction);

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
        settings.add(settingsEditor);
        settings.add(settingsHighlighting);
        settings.add(settingsExceptionHandler);
        settings.add(settingsMemoryConfiguration);

        helpHelp = new JMenuItem(helpHelpAction);
        helpHelp.setIcon(new ImageIcon(toolkit.getImage(c.getResource(Main.imagesPath + "Help16.png"))));
        JMenuItem helpAbout = new JMenuItem();
        helpAbout.setText("About...");
        helpAbout.setToolTipText("Information about MARS");
        helpAbout.addActionListener((event) -> JOptionPane.showMessageDialog(mainFrame,
                "MARS " + Main.version + "    Copyright " + Main.copyrightYears + "\n"
                + Main.copyrightHolders + "\n"
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
                new ImageIcon(icon))
        );

        help.add(helpHelp);
        help.addSeparator();
        help.add(helpAbout);

        menuBar.add(file);
        menuBar.add(edit);
        menuBar.add(run);
        menuBar.add(settings);
        JMenu toolMenu = new ToolLoader().buildToolsMenu();
        if (toolMenu != null) menuBar.add(toolMenu);
        menuBar.add(help);

        // experiment with popup menu for settings. 3 Aug 2006 PS
        //setupPopupMenu();
        //return menuBar;
        mainFrame.setJMenuBar(menuBar);
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Tool bar">
        /*
         * build the toolbar and connect items to action objects (which serve as action listeners
         * shared between toolbar icon and corresponding menu item).
         */
        toolBar = new JToolBar();

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
        DumpMemory = new JButton(fileDumpMemoryAction);
        DumpMemory.setText("");
        Help = new JButton(helpHelpAction);
        Help.setText("");

        adjustInternalFrames = new JButton("Adjust");
        adjustInternalFrames.addActionListener((event) -> executePane.setWindowBounds());

        marsMode = new JButton(new ImageIcon(Toolkit.getDefaultToolkit().getImage(c.getResource(Main.imagesPath + "Assemble22.png"))));
        marsMode.setText("Execute");
        marsMode.addActionListener((event) -> toggleGUIMode());

        toolBar.add(marsMode);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(New);
        toolBar.add(Open);
        toolBar.add(Save);
        toolBar.add(SaveAs);
        toolBar.add(Print);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(Undo);
        toolBar.add(Redo);
        toolBar.add(Cut);
        toolBar.add(Copy);
        toolBar.add(Paste);
        toolBar.add(FindReplace);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(Help);

        toolBar.setFloatable(false);
        //</editor-fold>

        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jp.add(toolBar);

        mainFrame.getContentPane().add(jp, BorderLayout.NORTH);
        mainFrame.getContentPane().add(horizonSplitter);
        
        mainFrame.addWindowListener(new WindowAdapter() {
            // This is invoked when exiting the app through the X icon.  It will in turn
            // check for unsaved edits before exiting.
            @Override
            public void windowClosing(WindowEvent e) {
                if (editTabbedPane.closeAllFiles())
                    mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }
        });

        // The following will handle the windowClosing event properly in the 
        // situation where user Cancels out of "save edits?" dialog.  By default,
        // the GUI frame will be hidden but I want it to do nothing.
        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        setMenuStateInitial();
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        mainFrame.pack();
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

    private void updateEditMenu() {
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        updateUndoManager();
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
        fileDumpMemoryAction.setEnabled(false);
    }

    /* Determine from FileStatus what the menu state (enabled/disabled)should 
     * be then call the appropriate method to setStatusMenu it.  Current states are:
     *
     * setMenuStateInitial: setStatusMenu upon startup and after File->Close
     * setMenuStateEditingNew: setStatusMenu upon File->New
     * setMenuStateEditing: setStatusMenu upon File->Open or File->Save or erroneous Run->Assemble
     * setMenuStateRunnable: setStatusMenu upon successful Run->Assemble
     * setMenuStateRunning: setStatusMenu upon Run->Go
     * setMenuStateTerminated: setStatusMenu upon completion of simulated execution
     */
    void setMenuStateInitial() {
        marsMode.setEnabled(false);

        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(false);
        fileCloseAllAction.setEnabled(false);
        fileSaveAction.setEnabled(false);
        fileSaveAsAction.setEnabled(false);
        fileSaveAllAction.setEnabled(false);
        filePrintAction.setEnabled(false);
        fileExitAction.setEnabled(true);
        disableEditMenu();
        disableRunMenu();
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
    }

    /* Added DPS 9-Aug-2011, for newly-opened files.  Retain
     existing Run menu state (except Assemble, which is always true).
     Thus if there was a valid assembly it is retained. */
    void setMenuStateNotEdited() {
        marsMode.setEnabled(true);
        /* Note: undo and redo are handled separately by the undo manager*/
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        filePrintAction.setEnabled(true);
        fileExitAction.setEnabled(true);
        updateEditMenu();
        settingsDelayedBranchingAction.setEnabled(true);
        settingsMemoryConfigurationAction.setEnabled(true);
        // If assemble-all, allow previous Run menu settings to remain.
        // Otherwise, clear them out.  DPS 9-Aug-2011
        if (!Settings.BooleanSettings.ASSEMBLE_ALL.isSet())
            disableRunMenu();
    }

    void setMenuStateEditing() {
        marsMode.setEnabled(true);
        /* Note: undo and redo are handled separately by the undo manager*/
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        filePrintAction.setEnabled(true);
        fileExitAction.setEnabled(true);
        updateEditMenu();
        disableRunMenu();
        settingsDelayedBranchingAction.setEnabled(true); // added 25 June 2007
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
    }

    /* Use this upon successful assemble or reset
     */
    void setMenuStateRunnable() {
        marsMode.setEnabled(true);
        /* Note: undo and redo are handled separately by the undo manager */
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        fileDumpMemoryAction.setEnabled(true);
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
        /* Note: undo and redo are handled separately by the undo manager */
        fileNewAction.setEnabled(false);
        fileOpenAction.setEnabled(false);
        fileCloseAction.setEnabled(false);
        fileCloseAllAction.setEnabled(false);
        fileSaveAction.setEnabled(false);
        fileSaveAsAction.setEnabled(false);
        fileSaveAllAction.setEnabled(false);
        fileDumpMemoryAction.setEnabled(false);
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
        /* Note: undo and redo are handled separately by the undo manager */
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        fileDumpMemoryAction.setEnabled(true);
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

    // Toggles between "Edit" and "Execute" mode, called inside constructor
    final void toggleGUIMode() {
        if (executePane.isShowing()) {
            marsMode.setText("Execute");
            marsMode.setToolTipText("View and control program execution");

            toolBar.removeAll();
            toolBar.add(marsMode);
            toolBar.add(new JToolBar.Separator());
            toolBar.add(New);
            toolBar.add(Open);
            toolBar.add(Save);
            toolBar.add(SaveAs);
            toolBar.add(Print);
            toolBar.add(new JToolBar.Separator());
            toolBar.add(Undo);
            toolBar.add(Redo);
            toolBar.add(Cut);
            toolBar.add(Copy);
            toolBar.add(Paste);
            toolBar.add(FindReplace);
            toolBar.add(new JToolBar.Separator());
            toolBar.add(Help);

            mainPane.removeAll();
            mainPane.add(editTabbedPane);
        }
        else if (ExecuteAction.assemble()) {
            marsMode.setText("Edit");
            marsMode.setToolTipText("Edit MIPS program");

            toolBar.removeAll();
            toolBar.add(marsMode);
            toolBar.add(new JToolBar.Separator());
            toolBar.add(Run);
            toolBar.add(Step);
            toolBar.add(Backstep);
            toolBar.add(Pause);
            toolBar.add(Stop);
            toolBar.add(Reset);
            toolBar.add(new JToolBar.Separator());
            toolBar.add(adjustInternalFrames);
            toolBar.add(DumpMemory);
            toolBar.add(Help);
            toolBar.add(new JToolBar.Separator());
            toolBar.add(RunSpeedPanel.getInstance());

            mainPane.removeAll();
            mainPane.add(executePane);
        }
        mainFrame.validate();
    }

    /**
     * Send keyboard event to menu for possible processing. DPS 5-4-10
     *
     * @param evt KeyEvent for menu component to consider for processing.
     */
    public void dispatchEventToMenu(KeyEvent evt) {
        this.menuBar.dispatchEvent(evt);
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
        MouseListener popupListener = new PopupListener(popup);
        mainFrame.addMouseListener(popupListener);
    }

    public final void simUpdate() {
        if (registersPane.getSelectedComponent() == registersTab)
            registersTab.updateRegisters();
        else
            coprocessor1Tab.updateRegisters();
        executePane.getDataSegmentWindow().updateValues();
        executePane.getTextSegmentWindow().setCodeHighlighting(true);
        executePane.getTextSegmentWindow().highlightStepAtPC();
    }

    /**
     * Places name of file currently being edited into its edit tab and the
     * application's titleLabel bar. The edit tab will contain only the
     * filename, the titleLabel bar will contain full pathname. If file has been
     * modified since created, opened or saved, as indicated by value of the
     * status parameter, the name and path will be followed with an '*'. If
     * newly-created file has not yet been saved, the titleLabel bar will show
     * (temporary) file name but not path.
     */
    void updateGUIState() {
        EditPane tab = editTabbedPane.getSelectedComponent();

        if (tab == null) {
            mainFrame.setTitle(baseTitle);
            setMenuStateInitial();
        }
        else {
            String editMark = tab.hasUnsavedEdits() ? "* " : "  ";
            mainFrame.setTitle(tab.getPathname() + editMark + " - " + baseTitle);
            editTabbedPane.setTitleAtComponent(tab.getFilename() + editMark, tab);

            if (tab.isNew() || tab.hasUnsavedEdits())
                setMenuStateEditing();
            else
                setMenuStateNotEdited();
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

}
