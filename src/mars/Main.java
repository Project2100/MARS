package mars;

import mars.settings.Settings;
import java.awt.EventQueue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mars.assembler.SymbolTable;
import mars.mips.hardware.Memory;
import mars.mips.instructions.InstructionSet;
import mars.mips.instructions.syscalls.SyscallNumberOverride;
import mars.settings.StringSettings;
//import mars.venus.MarsSplashScreen;
import mars.venus.VenusUI;

/*
 Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * Collection of globally-available data structures.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public class Main {

    /**
     * Application logger, central point for exception-debug information
     */
    public static final Logger logger = Logger.getLogger(Main.class.getName());

    // This lambda will be used as the default handler for uncaught exceptions
    private static final Thread.UncaughtExceptionHandler exHandler = (thread, exception)
            -> Main.logger.log(Level.SEVERE, "Uncaught exception in thread: " + thread.getName(), exception);

    // List these first because they are referenced by methods called at initialization.
    private static final String CONFIG_FILENAME = "Config";
    private static final String SYSCALL_FILENAME = "Syscall";
    public static final String SETTINGS_FILENAME = "Settings";
    
    /**
     * The status of implemented MIPS instructions.
     */
    public static InstructionSet instructionSet;
    /**
     * the program currently being worked with. Used by GUI only, not command
     * line.
     */
    public static MIPSprogram program;
    /**
     * Symbol table for file currently being assembled.
     */
    public static SymbolTable symbolTable;
    /**
     * Simulated MIPS memory component.
     */
    public static Memory memory = Memory.getInstance();
    /**
     * Lock variable used at head of synchronized block to guard MIPS memory and
     * registers
     */
    public static final Object memoryAndRegistersLock = new Object();
    /**
     * Flag to determine whether or not to produce internal debugging
     * information.
     */
    public static boolean debug = false;
    /**
     * Object that contains various settings that can be accessed modified
     * internally.
     */
    private static Settings settings;
    /**
     * String to GUI's RunI/O text area when echoing user input from pop-up
     * dialog.
     */
    public static String userInputAlert = "**** user input : ";
    /**
     * Path to folder that contains images. The leading "/" in file path
     * prevents package name from being pre-pended.
     */
    public static final String imagesPath = "/images/";
    /**
     * Path to folder that contains help text
     */
    public static final String helpPath = "/help/";
    /**
     * Flag that indicates whether or not instructionSet has been initialized.
     */
    private static boolean initialized = false;
    /**
     * The GUI being used (if any) with this simulator.
     */
    private static VenusUI gui = null;
    /**
     * MARS copyright years
     */
    public static final String COPYRIGHT_YEARS = "2003-2014";
    /**
     * MARS copyright holders
     */
    public static final String COPYRIGHT_HOLDERS = "Pete Sanderson and Kenneth Vollmar";
    /**
     * The current MARS version number. Can't wait for "initialize()" call to
     * set it.
     */
    public static final String VERSION = "4.5";
    /**
     * List of accepted file extensions for MIPS assembly source files.
     */
    public static final ArrayList<String> fileExtensions;
    /**
     * Maximum length of scrolled message window (MARS Messages and Run I/O)
     */
    public static final int maximumMessageCharacters;
    /**
     * Maximum number of assembler errors produced by one assemble operation
     */
    public static final int maximumErrorMessages;
    /**
     * Maximum number of back-step operations to buffer
     */
    public static final int maximumBacksteps;
    /**
     * Placeholder for non-printable ASCII codes, default is: {@code .}
     */
    public static final String ASCII_NON_PRINT;
    /**
     * Array of strings to display for ASCII codes in ASCII display of data
     * segment. ASCII code 0-255 is array index. {@code "null"} codes are
     * translated as non-printable, while {@code "space"} codes will be
     * represented by the proper space character.
     */
    public static final String[] ASCII_TABLE;

    static {
        Properties configProps = loadPropertiesFromFile(CONFIG_FILENAME);

        String extensions = configProps.getProperty("Extensions", "");
        fileExtensions = (extensions.isEmpty()
                ? new ArrayList<>()
                : Stream.of(extensions.split(" "))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new)));

        maximumMessageCharacters = getIntegerProperty(configProps, "MessageLimit", 1000000);
        maximumErrorMessages = getIntegerProperty(configProps, "ErrorLimit", 200);
        maximumBacksteps = getIntegerProperty(configProps, "BackstepLimit", 1000);

        String anp = configProps.getProperty("AsciiNonPrint", ".");
        ASCII_NON_PRINT = anp.equals("space") ? " " : anp;

        String[] literals = configProps.getProperty("AsciiTable").split(" +");
        int maxLength = 0;
        for (int i = 0; i < literals.length; i++) {
            if (literals[i].equals("null")) literals[i] = ASCII_NON_PRINT;
            if (literals[i].equals("space")) literals[i] = " ";
            if (literals[i].length() > maxLength)
                maxLength = literals[i].length();
        }
        String padding = "        ";
        maxLength++;
        for (int i = 0; i < literals.length; i++)
            literals[i] = padding.substring(0, maxLength - literals[i].length()) + literals[i];
        ASCII_TABLE = literals;
    }

    /**
     * MARS exit code -- useful with SYSCALL 17 when running from command line
     * (not GUI)
     */
    public static int exitCode = 0;

    public static boolean runSpeedPanelExists = false;

    /**
     * Getter for MARS' GUI, is null if arguments were provided at application
     * start
     *
     * @return the GUI's main class reference
     */
    public static VenusUI getGUI() {
        return gui;
    }

    /**
     * Getter for MARS' settings
     *
     * @return the Settings object containing all application settings
     */
    public static Settings getSettings() {
        return settings;
    }

    /**
     * Method called at system initialization to create global data structures.
     */
    public static void initialize() {
        if (!initialized) {
            Thread.setDefaultUncaughtExceptionHandler(Main.exHandler);
            logger.setLevel(debug ? Level.INFO : Level.WARNING);
            settings = new Settings();
            instructionSet = new InstructionSet();
            instructionSet.populate();
            symbolTable = new SymbolTable("global");
            initialized = true;
            debug = false;
        }
    }

    // Read and return integer property value for given file and property name.
    // Default value is returned if property file or name not found.
    private static int getIntegerProperty(Properties propertiesFile, String propertyName, int defaultValue) {
        int limit = defaultValue;  // just in case no entry is found
        try {
            limit = Integer.parseInt(propertiesFile.getProperty(propertyName, Integer.toString(defaultValue)));
        }
        catch (NumberFormatException nfe) {
        } // do nothing, I already have a default
        return limit;
    }

    /**
     * Get list of MarsTools that reside outside the MARS distribution.
     * Currently this is done by adding the tool's path name to the list of
     * values for the external_tools property. Use ";" as delimiter!
     *
     * @return ArrayList. Each item is file path to .class file of a class that
     * implements MarsTool. If none, returns empty list.
     */
    public static ArrayList<String> getExternalTools() {
        ArrayList<String> toolsList = new ArrayList<>();
        String delimiter = ";";
        String tools = loadPropertiesFromFile(CONFIG_FILENAME).getProperty("ExternalTools");
        if (tools != null) {
            StringTokenizer st = new StringTokenizer(tools, delimiter);
            while (st.hasMoreTokens())
                toolsList.add(st.nextToken());
        }
        return toolsList;
    }

    /**
     * Produce Properties (a Hashtable) object containing key-value pairs from
     * specified properties file. This may be used as an alternative to
     * readPropertiesFile() which uses a different implementation.
     *
     * @param file Properties filename. Do NOT include the file extension as it
     * is assumed to be ".properties" and is added here.
     * @return Properties (Hashtable) of key-value pairs read from the file.
     */
    public static Properties loadPropertiesFromFile(String file) {
        Properties p = new Properties();
        try {
            p.load(Main.class.getResourceAsStream("/" + file + ".properties"));
        }
        catch (NullPointerException | IOException ioe) {
            // If it doesn't work, properties will be empty
        }
        return p;
    }

    /**
     * Return whether backstepping is permitted at this time. Backstepping is
     * ability to undo execution steps one at a time. Available only in the IDE.
     * This is not a persistent setting and is not under MARS user control.
     *
     * @return true if backstepping is permitted, false otherwise.
     */
    public static boolean isBackSteppingEnabled() {
        return program != null && program.getBackStepper() != null && program.getBackStepper().enabled();
    }

    /**
     * Read any syscall number assignment overrides from configuration file.
     *
     * @return ArrayList of SyscallNumberOverride objects
     */
    public ArrayList<SyscallNumberOverride> getSyscallOverrides() {
        ArrayList<SyscallNumberOverride> overrides = new ArrayList<>();
        Properties p = loadPropertiesFromFile(SYSCALL_FILENAME);
        Enumeration<Object> keys = p.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            overrides.add(new SyscallNumberOverride(key, p.getProperty(key)));
        }
        return overrides;
    }

    /**
     * Starting point of MARS
     *
     * @param args the program arguments. If none are provided, the application
     * will start its GUI
     */
    public static void main(String[] args) {
        initialize();

        if (args.length == 0) {
            // Puts MARS menu on Mac OS menu bar
            System.setProperty("apple.laf.useScreenMenuBar", "true");

            // Calling GUI related functionality outside EDT!
            //----------------------------------------------------------------------
            // Putting this call inside EDT will cause both the splash and the main
            // frame to show at the same time, effectively forfeiting the splash's
            // purpose; calling it externally seems to be safe
            //
            // Andrea Proietto, 15/04/28 21:37
            //
            // NOTE 151016 - This seems to actually slow down startup - removing
//            MarsSplashScreen.showSplash(2000);
            memory.configure(Memory.getConfigByName(StringSettings.MEMORY_CONFIGURATION.get()));
            EventQueue.invokeLater(() -> {
                settings.AWTinit();
                Thread.setDefaultUncaughtExceptionHandler(exHandler);
                gui = new VenusUI();
            });
        }
        else
            new MarsLaunch(args);
    }
}
