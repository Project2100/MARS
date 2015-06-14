package mars;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.Arrays;
import java.util.Observable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import mars.util.Binary;
import mars.venus.editors.jeditsyntax.SyntaxStyle;
import mars.venus.editors.jeditsyntax.SyntaxUtilities;

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
 * Contains various IDE settings. Persistent settings are maintained for the
 * current user and on the current machine using Java's {@code Preferences}
 * class. Failing that, default setting values come from Settings.properties
 * file. If both of those fail, default values come from static arrays defined
 * in this class. The latter can can be modified prior to instantiating Settings
 * object.<p/>
 *
 * If the Preference objects fail due to security exceptions, changes to
 * settings will not carry over from one MARS session to the next.<p/>
 *
 * Actual implementation of the Preference objects is platform-dependent. For
 * Windows, they are stored in Registry. To see, run {@code regedit} and browse
 * to: {@code HKEY_CURRENT_USER\Software\JavaSoft\Prefs\mars}
 *
 * @author Pete Sanderson
 * @see java.util.prefs.Preferences
 */
public class Settings extends Observable {

    // Properties file used to hold default settings
    private final String settingsFile = "/Settings.properties";
    // Preferences node - see documentation
    private static final Preferences preferences = Preferences.userNodeForPackage(Settings.class);

    /**
     * Enumeration of boolean application settings represented as
     * {@code String}/{@code boolean} couples.
     */
    public enum BooleanSettings {

        /**
         * Flag to determine whether or not program being assembled is limited
         * to basic MIPS instructions and formats.
         */
        EXTENDED_ASSEMBLER("ExtendedAssembler", true),
        /**
         * Flag to determine whether or not program being assembled is limited
         * to using register numbers instead of names. NOTE: Its default value
         * is false and the IDE provides no means to change it!
         */
        BARE_MACHINE("BareMachine", false),
        /**
         * Flag to determine whether or not a file is immediately and
         * automatically assembled upon opening. Handy when using external
         * editor like mipster.
         */
        ASSEMBLE_ON_OPEN("AssembleOnOpen", false),
        /**
         * Flag to determine whether only the current editor source file
         * (enabled false) or all files in its directory (enabled true) will be
         * assembled when assembly is selected.
         */
        ASSEMBLE_ALL("AssembleAll", false),
        /**
         * Default visibility of label window (symbol table). Default only,
         * dynamic status maintained by ExecutePane
         */
        LABEL_WINDOW_VISIBILITY("LabelWindowVisibility", false),
        /**
         * Default setting for displaying addresses in hexadecimal in the
         * Execute pane.
         */
        DISPLAY_ADDRESSES_IN_HEX("DisplayAddressesInHex", true),
        /**
         * Default setting for displaying values in hexadecimal in the Execute
         * pane.
         */
        DISPLAY_VALUES_IN_HEX("DisplayValuesInHex", true),
        /**
         * Flag to determine whether the currently selected exception handler
         * source file will be included in each assembly operation.
         */
        EXCEPTION_HANDLER("LoadExceptionHandler", false),
        /**
         * Flag to determine whether or not delayed branching is in effect at
         * MIPS execution. This means we simulate the pipeline and statement
         * FOLLOWING a successful branch is executed before branch is taken. DPS
         * 14 June 2007.
         */
        DELAYED_BRANCHING("DelayedBranching", false),
        /**
         * Flag to determine whether or not the editor will display line
         * numbers.
         */
        EDITOR_LINE_NUMBERS("EditorLineNumbersDisplayed", true),
        /**
         * Flag to determine whether or not assembler warnings are considered
         * errors.
         */
        WARNINGS_ARE_ERRORS("WarningsAreErrors", false),
        /**
         * Flag to determine whether or not to display and use program arguments
         */
        PROGRAM_ARGUMENTS("ProgramArguments", false),
        /**
         * Flag to control whether or not highlighting is applied to data
         * segment window
         */
        DATA_SEGMENT_HIGHLIGHTING("DataSegmentHighlighting", true),
        /**
         * Flag to control whether or not highlighting is applied to register
         * windows
         */
        REGISTERS_HIGHLIGHTING("RegistersHighlighting", true),
        /**
         * Flag to control whether or not assembler automatically initializes
         * program counter to 'main's address
         */
        START_AT_MAIN("StartAtMain", false),
        /**
         * Flag to control whether or not editor will highlight the line
         * currently being edited
         */
        EDITOR_CURRENT_LINE_HIGHLIGHTING("EditorCurrentLineHighlighting", true),
        /**
         * Flag to control whether or not editor will provide pop-up instruction
         * guidance while typing
         */
        POPUP_INSTRUCTION_GUIDANCE("PopupInstructionGuidance", true),
        /**
         * Flag to control whether or not simulator will use pop-up dialog for
         * input syscalls
         */
        POPUP_SYSCALL_INPUT("PopupSyscallInput", false),
        /**
         * Flag to control whether or not to use generic text editor instead of
         * language-aware styled editor
         */
        GENERIC_TEXT_EDITOR("GenericTextEditor", false),
        /**
         * Flag to control whether or not language-aware editor will use
         * auto-indent feature
         */
        AUTO_INDENT("AutoIndent", true),
        /**
         * Flag to determine whether a program can write binary code to the text
         * or data segment and execute that code.
         */
        SELF_MODIFYING_CODE("SelfModifyingCode", false);

        private final String identifier;
        private boolean state;

        private BooleanSettings(String identifier, boolean state) {
            this.identifier = identifier;
            this.state = state;
        }

        /**
         * Fetch value of this boolean setting.
         *
         * @return the current state of this setting
         */
        public boolean isSet() {
            return state;
        }

        /**
         * Sets the state of this setting
         *
         * @param newState the new state to store
         */
        public void set(boolean newState) {
            boolean oldState = state;
            state = newState;
            if (Main.getSettings() != null)
                if (state != oldState) {
                    Settings.preferences.putBoolean(identifier, state);
                    try {
                        Settings.preferences.flush();
                    }
                    catch (SecurityException | BackingStoreException e) {
                        Main.logger.log(Level.SEVERE, "Unable to save boolean setting: " + identifier, e);
                    }
                    Main.getSettings().setChanged();
                    Main.getSettings().notifyObservers();
                }
        }

        /**
         * Temporarily establish boolean setting. This setting will NOT be
         * written to persistent store! Currently this is used only when running
         * MARS from the command line
         *
         * @param value True to enable the setting, false otherwise.
         */
        public void setNoPersist(boolean value) {
            state = value;
        }

    };

    public enum StringSettings {

        /**
         * Current specified exception handler file (a MIPS assembly source
         * file)
         */
        EXCEPTION_HANDLER_FILE("ExceptionHandler", ""),
        /**
         * Identifier of current memory configuration
         */
        MEMORY_CONFIGURATION("MemoryConfiguration", ""),
        /**
         * Order of text segment table columns
         */
        TEXT_COLUMN_ORDER("TextColumnOrder", "0 1 2 3 4");

        private final String identifier;
        private final String vDefault;
        private String value;

        private StringSettings(String identifier, String value) {
            this.identifier = identifier;
            vDefault = value;
            this.value = vDefault;
        }

        public String get() {
            return value;
        }

        public String getDefault() {
            return vDefault;
        }

        public void set(String value) {
            this.value = value;
            if (Main.getSettings() != null) {
                Settings.preferences.put(identifier, value);
                try {
                    Settings.preferences.flush();
                }
                catch (SecurityException | BackingStoreException e) {
                    Main.logger.log(Level.SEVERE, "Unable to save string setting: " + identifier, e);
                }
                Main.getSettings().setChanged();
                Main.getSettings().notifyObservers();
            }
        }

        /**
         * Order of text segment display columns (there are 5, numbered 0 to 4).
         *
         * @return Array of integers indicating the column order. Original order
         * is {@code "0 1 2 3 4"}.
         */
        public static int[] getTextColumnOrder() {
            return Arrays.stream(TEXT_COLUMN_ORDER.value.split(" "))
                    .mapToInt(Integer::parseInt)
                    .toArray();
        }

        /**
         * Store the current order of Text Segment window table columns, so the
         * ordering can be preserved and restored.
         *
         * @param columnOrder An array of int indicating column order.
         */
        public static void setTextColumnOrder(int[] columnOrder) {
            TEXT_COLUMN_ORDER.set(Arrays.stream(columnOrder)
                    .mapToObj(Integer::toString)
                    .reduce((String a, String b) -> a + " " + b)
                    .get());
        }
    }

    public enum IntegerSettings {

        /**
         * Caret blink rate in milliseconds, 0 means don't blink.
         */
        CARET_BLINK_RATE("CaretBlinkRate", 500),
        /**
         * Editor tab size in characters.
         */
        EDITOR_TAB_SIZE("EditorTabSize", 8),
        /**
         * State for sorting label window display (can sort by either label or
         * address and either ascending or descending order). Default state is
         * 0, by ascending addresses.
         *
         * There are 8 possible states, ranging from 0 to 7, as described in
         * LabelsWindow.java
         */
        LABEL_SORT_STATE("LabelSortState", 0),
        /**
         * Number of letters to be matched by editor's instruction guide before
         * pop-up generated (if pop-up enabled). Should be 1 or 2. If 1, the
         * pop-up will be generated after first letter typed, based on all
         * matches; if 2, the pop-up will be generated after second letter
         * typed.
         */
        EDITOR_POPUP_PREFIX_LENGTH("EditorPopupPrefixLength", 2);

        private final String identifier;
        private final int vDefault;
        private int value;

        private IntegerSettings(String identifier, int value) {
            this.identifier = identifier;
            vDefault = value;
            this.value = vDefault;
        }

        public int get() {
            return value;
        }

        public int getDefault() {
            return vDefault;
        }

        public void set(int value) {
            this.value = value;

            if (Main.getSettings() != null) {
                Settings.preferences.putInt(identifier, value);
                try {
                    Settings.preferences.flush();
                }
                catch (SecurityException | BackingStoreException e) {
                    Main.logger.log(Level.SEVERE, "Unable to save integer setting: " + identifier, e);
                }
                Main.getSettings().setChanged();
                Main.getSettings().notifyObservers();
            }
        }
    }

    // DPS 3-Oct-2012
    // Changed default font family from "Courier New" to "Monospaced" after receiving reports that Mac were not
    // correctly rendering the left parenthesis character in the editor or text segment display.
    // See http://www.mirthcorp.com/community/issues/browse/MIRTH-1921?page=com.atlassian.jira.plugin.system.issuetabpanels:all-tabpanel
    public enum FontSettings {

        /**
         * Font for the text editor
         */
        EDITOR_FONT("EditorFont", "Monospaced", Font.PLAIN, 12),
        /**
         * Font for table even row (text, data, register displays)
         */
        EVEN_ROW_FONT("EvenRowFont", "Monospaced", Font.PLAIN, 12),
        /**
         * Font for table odd row (text, data, register displays)
         */
        ODD_ROW_FONT("OddRowFont", "Monospaced", Font.PLAIN, 12),
        /**
         * Font for table odd row (text, data, register displays)
         */
        TEXTSEGMENT_HIGHLIGHT_FONT("TextSegmentHighlightFont", "Monospaced", Font.PLAIN, 12),
        /**
         * Font for text segment delay slot highlighted
         */
        TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FONT("TextSegmentDelaySlotHighlightFont", "Monospaced", Font.PLAIN, 12),
        /**
         * Font for text segment highlighted
         */
        DATASEGMENT_HIGHLIGHT_FONT("DataSegmentHighlightFont", "Monospaced", Font.PLAIN, 12),
        /**
         * Font for register highlighted
         */
        REGISTER_HIGHLIGHT_FONT("RegisterHighlightFont", "Monospaced", Font.PLAIN, 12);

        private final String identifier;
        private final Font fDefault;
        private Font font;

        private FontSettings(String identifier, String type, int style, int size) {
            this.identifier = identifier;
            font = fDefault = new Font(type, style, size);
        }

        /**
         * Retrieve a Font setting
         *
         * @return the Font object for this item
         */
        public Font get() {
            return font;
        }

        /**
         * Retrieve a default Font setting
         *
         * @return the default Font object for this item
         */
        public Font getDefault() {
            return fDefault;
        }

        /**
         * Set this font setting to the specified Font object and write it to
         * persistent storage.
         *
         * @param font The font to set this item to
         */
        public void set(Font font) {
            this.font = font;

            if (Main.getSettings() != null) {
                Settings.preferences.put(identifier + "_TYPE", font.getFamily());
                Settings.preferences.putInt(identifier + "_STYLE", font.getStyle());
                Settings.preferences.putInt(identifier + "_SIZE", font.getSize());
                try {
                    Settings.preferences.flush();
                }
                catch (SecurityException | BackingStoreException e) {
                    Main.logger.log(Level.SEVERE, "Unable to save font setting: " + identifier + " (background)", e);
                }
                Main.getSettings().setChanged();
                Main.getSettings().notifyObservers();
            }
        }

    }

    public enum ColorSettings {

        /**
         * RGB color for table even row background/foreground (text, data,
         * register displays)
         */
        EVEN_ROW("EvenRowColors", "0x00e0e0e0", "0"),
        /**
         * RGB color for table odd row background/foreground (text, data,
         * register displays)
         */
        ODD_ROW("OddRowColors", "0x00ffffff", "0"),
        /**
         * RGB color for text segment highlighted background/foreground
         */
        TEXTSEGMENT_HIGHLIGHT("TextSegmentHighlightColors", "0x00ffff99", "0"),
        /**
         * RGB color for text segment delay slot highlighted
         * background/foreground
         */
        TEXTSEGMENT_DELAYSLOT_HIGHLIGHT("TextSegmentDelaySlotHighlightColors", "0x0033ff00", "0"),
        /**
         * RGB color for text segment highlighted background/foreground
         */
        DATASEGMENT_HIGHLIGHT("DataSegmentHighlightColors", "0x0099ccff", "0"),
        /**
         * RGB color for register highlighted background/foreground
         */
        REGISTER_HIGHLIGHT("RegisterHighlightColors", "0x0099cc55", "0");

        private final String identifier;
        private final String bDefault;
        private final String fDefault;
        private String background;
        private String foreground;

        private ColorSettings(String identifier, String bDefault, String fDefault) {
            this.identifier = identifier;
            this.bDefault = bDefault;
            background = bDefault;
            this.fDefault = fDefault;
            foreground = fDefault;
        }

        /**
         * Get the background color for this color setting.
         *
         * @return the background color
         */
        public Color getBackground() {
            return Color.decode(background);

        }

        /**
         * Get the default background color for this color setting.
         *
         * @return the default background color
         */
        public Color getDefaultBackground() {
            return Color.decode(bDefault);
        }

        /**
         * * Get foreground color for this color setting.
         *
         * @return the foreground color
         */
        public Color getForeground() {
            return Color.decode(foreground);

        }

        /**
         * Get default foreground color for this color setting.
         *
         * @return the default foreground color
         */
        public Color getDefaultForeground() {
            return Color.decode(fDefault);
        }

        /**
         * Set background color for this color setting.
         *
         * @param color the color to save
         */
        public void setBackground(Color color) {
            background = Binary.intToHexString(color.getRed() << 16 | color.getGreen() << 8 | color.getBlue());

            if (Main.getSettings() != null) try {
                Settings.preferences.put(identifier + "_BG", background);
                Settings.preferences.flush();
            }
            catch (SecurityException | BackingStoreException e) {
                Main.logger.log(Level.SEVERE, "Unable to save color setting: " + identifier + " (background)", e);
            }

        }

        /**
         * Set foreground color for this color setting.
         *
         * @param color the color to save
         */
        public void setForeground(Color color) {
            foreground = Binary.intToHexString(color.getRed() << 16 | color.getGreen() << 8 | color.getBlue());

            if (Main.getSettings() != null) try {
                Settings.preferences.put(identifier + "_FG", foreground);
                Settings.preferences.flush();
            }
            catch (SecurityException | BackingStoreException e) {
                Main.logger.log(Level.SEVERE, "Unable to save color setting: " + identifier + " (foreground)", e);
            }

        }

    }

    /**
     * Create Settings object and set to saved values. If saved values not
     * found, will set based on defaults stored in Settings.properties file. If
     * file problems, will set based on defaults stored in this class.
     */
    public Settings() {
        initializeEditorSyntaxStyles();

        Properties props = new Properties();
        try {
            props.load(Settings.class.getResourceAsStream(settingsFile));
        }
        catch (IOException e) {
            Main.logger.log(Level.WARNING, "Error while reading Settings.properties defaults. Using built-in defaults.", e);
        }

        String val;
        for (BooleanSettings b : BooleanSettings.values()) {
            //                System.out.println("current setting: "+b.identifier+" - Defualt: "+b.state);
            val = props.getProperty(b.identifier);
            //                System.out.println("value read from properties:"+val);
            b.set(preferences.getBoolean(b.identifier, (val != null
                    ? Boolean.parseBoolean(val)
                    : b.state)));
            //                System.out.println("final value: "+b.state);
        }
        for (StringSettings s : StringSettings.values()) {
            val = props.getProperty(s.identifier);
            s.set(preferences.get(s.identifier, (val != null
                    ? val
                    : s.vDefault)));
        }
        for (IntegerSettings i : IntegerSettings.values()) {
            val = props.getProperty(i.identifier);
            try {
                i.set(preferences.getInt(i.identifier, (val != null
                        ? Integer.parseInt(val)
                        : i.vDefault)));
            }
            catch (NumberFormatException e1) {
                i.set(i.vDefault);
            }
        }
        for (FontSettings f : FontSettings.values()) {
            String val1 = props.getProperty(f.identifier + "_TYPE");
            String val2 = props.getProperty(f.identifier + "_STYLE");
            String val3 = props.getProperty(f.identifier + "_SIZE");

            String type = preferences.get(f.identifier + "_TYPE", (val1 != null ? val1 : "Monospaced"));
            int style = preferences.getInt(f.identifier + "_STYLE", (val2 != null ? Integer.parseInt(val2) : Font.PLAIN));
            int size = preferences.getInt(f.identifier + "_SIZE", (val3 != null ? Integer.parseInt(val3) : 12));

            f.set(new Font(type, style, size));
        }
        for (ColorSettings c : ColorSettings.values()) {
            String val11 = props.getProperty(c.identifier + "_BG");
            String val21 = props.getProperty(c.identifier + "_FG");
            c.background = preferences.get(c.identifier + "_BG", val11 != null ? val11 : c.bDefault);
            c.foreground = preferences.get(c.identifier + "_FG", val21 != null ? val21 : c.fDefault);
        }
        getEditorSyntaxStyleSettingsFromPreferences();
    }

    /* **************************************************************************
     This section contains all code related to syntax highlighting styles settings.
     A style includes 3 components: color, bold (t/f), italic (t/f)
   
     The fallback defaults will come not from an array here, but from the
     existing static method SyntaxUtilities.getDefaultSyntaxStyles()
     in the mars.venus.editors.jeditsyntax package.  It returns an array
     of SyntaxStyle objects.
   
     */
    private String[] syntaxStyleColorSettingsValues;
    private boolean[] syntaxStyleBoldSettingsValues;
    private boolean[] syntaxStyleItalicSettingsValues;

    private static final String SYNTAX_STYLE_COLOR_PREFIX = "SyntaxStyleColor_";
    private static final String SYNTAX_STYLE_BOLD_PREFIX = "SyntaxStyleBold_";
    private static final String SYNTAX_STYLE_ITALIC_PREFIX = "SyntaxStyleItalic_";

    private static String[] syntaxStyleColorSettingsKeys, syntaxStyleBoldSettingsKeys, syntaxStyleItalicSettingsKeys;
    private static String[] defaultSyntaxStyleColorSettingsValues;
    private static boolean[] defaultSyntaxStyleBoldSettingsValues;
    private static boolean[] defaultSyntaxStyleItalicSettingsValues;

    /**
     *
     * @param index
     * @param syntaxStyle
     */
    public void setEditorSyntaxStyleByPosition(int index, SyntaxStyle syntaxStyle) {
        syntaxStyleColorSettingsValues[index] = syntaxStyle.getColorAsHexString();
        syntaxStyleItalicSettingsValues[index] = syntaxStyle.isItalic();
        syntaxStyleBoldSettingsValues[index] = syntaxStyle.isBold();
        saveEditorSyntaxStyle(index);
    }

    /**
     *
     * @param index
     * @return
     */
    public SyntaxStyle getEditorSyntaxStyleByPosition(int index) {
        return new SyntaxStyle(getColorValueByPosition(index, syntaxStyleColorSettingsValues),
                syntaxStyleItalicSettingsValues[index],
                syntaxStyleBoldSettingsValues[index]);
    }

    /**
     *
     * @param index
     * @return
     */
    public SyntaxStyle getDefaultEditorSyntaxStyleByPosition(int index) {
        return new SyntaxStyle(getColorValueByPosition(index, defaultSyntaxStyleColorSettingsValues),
                defaultSyntaxStyleItalicSettingsValues[index],
                defaultSyntaxStyleBoldSettingsValues[index]);
    }

    private void saveEditorSyntaxStyle(int index) {
        try {
            preferences.put(syntaxStyleColorSettingsKeys[index], syntaxStyleColorSettingsValues[index]);
            preferences.putBoolean(syntaxStyleBoldSettingsKeys[index], syntaxStyleBoldSettingsValues[index]);
            preferences.putBoolean(syntaxStyleItalicSettingsKeys[index], syntaxStyleItalicSettingsValues[index]);
            preferences.flush();
        }
        catch (SecurityException | BackingStoreException se) {
            // cannot write to persistent storage for security reasons
            // unable to communicate with persistent storage (strange days)
        }
    }

    // For syntax styles, need to initialize from SyntaxUtilities defaults.
    // Taking care not to explicitly create a Color object, since it may trigger
    // Swing initialization (that caused problems for UC Berkeley when we
    // created Font objects here).  It shouldn't, but then again Font shouldn't
    // either but they said it did.  (see HeadlessException)   
    // On the other hand, the first statement of this method causes Color objects
    // to be created!  It is possible but a real pain in the rear to avoid using 
    // Color objects totally.  Requires new methods for the SyntaxUtilities class.
    private void initializeEditorSyntaxStyles() {
        SyntaxStyle syntaxStyle[] = SyntaxUtilities.getDefaultSyntaxStyles();
        int tokens = syntaxStyle.length;
        syntaxStyleColorSettingsKeys = new String[tokens];
        syntaxStyleBoldSettingsKeys = new String[tokens];
        syntaxStyleItalicSettingsKeys = new String[tokens];
        defaultSyntaxStyleColorSettingsValues = new String[tokens];
        defaultSyntaxStyleBoldSettingsValues = new boolean[tokens];
        defaultSyntaxStyleItalicSettingsValues = new boolean[tokens];
        syntaxStyleColorSettingsValues = new String[tokens];
        syntaxStyleBoldSettingsValues = new boolean[tokens];
        syntaxStyleItalicSettingsValues = new boolean[tokens];
        for (int i = 0; i < tokens; i++) {
            syntaxStyleColorSettingsKeys[i] = SYNTAX_STYLE_COLOR_PREFIX + i;
            syntaxStyleBoldSettingsKeys[i] = SYNTAX_STYLE_BOLD_PREFIX + i;
            syntaxStyleItalicSettingsKeys[i] = SYNTAX_STYLE_ITALIC_PREFIX + i;
            syntaxStyleColorSettingsValues[i]
                    = defaultSyntaxStyleColorSettingsValues[i] = syntaxStyle[i].getColorAsHexString();
            syntaxStyleBoldSettingsValues[i]
                    = defaultSyntaxStyleBoldSettingsValues[i] = syntaxStyle[i].isBold();
            syntaxStyleItalicSettingsValues[i]
                    = defaultSyntaxStyleItalicSettingsValues[i] = syntaxStyle[i].isItalic();
        }
    }

    private void getEditorSyntaxStyleSettingsFromPreferences() {
        for (int i = 0; i < syntaxStyleColorSettingsKeys.length; i++) {
            syntaxStyleColorSettingsValues[i] = preferences.get(syntaxStyleColorSettingsKeys[i], syntaxStyleColorSettingsValues[i]);
            syntaxStyleBoldSettingsValues[i] = preferences.getBoolean(syntaxStyleBoldSettingsKeys[i], syntaxStyleBoldSettingsValues[i]);
            syntaxStyleItalicSettingsValues[i] = preferences.getBoolean(syntaxStyleItalicSettingsKeys[i], syntaxStyleItalicSettingsValues[i]);
        }
    }

    // Get Color object for this key array position. Get it from values array
    // provided as argument (could be either the current or the default settings array).	
    private Color getColorValueByPosition(int position, String[] values) {
        java.awt.Color color = null;
        if (position >= 0 && position < syntaxStyleColorSettingsKeys.length)
            try {
                color = java.awt.Color.decode(values[position]);
            }
            catch (NumberFormatException nfe) {
                color = null;
            }
        return color;
    }
}
