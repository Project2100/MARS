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
import mars.assembler.SymbolTable;
import mars.mips.hardware.Memory;
import mars.mips.instructions.InstructionSet;
import mars.mips.instructions.syscalls.SyscallNumberOverride;
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
     * Application logger, central point for exception reporting
     */
    public static final Logger logger = Logger.getLogger(Main.class.getName());

    /**
     * This lambda will be used as the default handler for uncaught exceptions
     */
    public static final Thread.UncaughtExceptionHandler exHandler = (thread, exception)
            -> Main.logger.log(Level.SEVERE, "Uncaught exception in thread: " + thread.getName(), exception);

    // List these first because they are referenced by methods called at initialization.
    private static final String configPropertiesFile = "Config";
    private static final String syscallPropertiesFile = "Syscall";

    // Properties file used to hold default settings
    public static final Properties properties = new Properties();
    static {
        try {
            properties.load(Main.class.getResourceAsStream("/Settings.properties"));
        }
        catch (IOException e) {
            Main.logger.log(Level.WARNING, "Unable to read Settings.properties file. Using built-in defaults.", e);
        }
    }

    /**
     * The status of implemented MIPS instructions.
     */
    public static InstructionSet instructionSet;
    /**
     * the program currently being worked with. Used by GUI only, not command
     * line. *
     */
    public static MIPSprogram program;
    /**
     * Symbol table for file currently being assembled.
     */
    public static SymbolTable symbolTable;
    /**
     * Simulated MIPS memory component.
     */
    public static Memory memory;
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
    public static final String copyrightYears = "2003-2014";
    /**
     * MARS copyright holders
     */
    public static final String copyrightHolders = "Pete Sanderson and Kenneth Vollmar";
    /**
     * The current MARS version number. Can't wait for "initialize()" call to
     * set it.
     */
    public static final String version = "4.5";
    /**
     * List of accepted file extensions for MIPS assembly source files.
     */
    public static final ArrayList<String> fileExtensions = getFileExtensions();
    /**
     * Maximum length of scrolled message window (MARS Messages and Run I/O)
     */
    public static final int maximumMessageCharacters = getMessageLimit();
    /**
     * Maximum number of assembler errors produced by one assemble operation
     */
    public static final int maximumErrorMessages = getErrorLimit();
    /**
     * Maximum number of back-step operations to buffer
     */
    public static final int maximumBacksteps = getBackstepLimit();
    /**
     * Placeholder for non-printable ASCII codes
     */
    public static final String ASCII_NON_PRINT = getAsciiNonPrint();
    /**
     * Array of strings to display for ASCII codes in ASCII display of data
     * segment. ASCII code 0-255 is array index.
     */
    public static final String[] ASCII_TABLE = getAsciiStrings();
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
            memory = Memory.getInstance();  //clients can use Memory.getInstance instead of Globals.memory
            instructionSet = new InstructionSet();
            instructionSet.populate();
            symbolTable = new SymbolTable("global");
            initialized = true;
            debug = false;
            memory.clear(); // will establish memory configuration from setting
        }
    }

    // Read byte limit of Run I/O or MARS Messages text to buffer.
    private static int getMessageLimit() {
        return getIntegerProperty(configPropertiesFile, "MessageLimit", 1000000);
    }

    // Read limit on number of error messages produced by one assemble operation.
    private static int getErrorLimit() {
        return getIntegerProperty(configPropertiesFile, "ErrorLimit", 200);
    }

    // Read backstep limit (number of operations to buffer) from properties file.
    private static int getBackstepLimit() {
        return getIntegerProperty(configPropertiesFile, "BackstepLimit", 1000);
    }

    // Read ASCII default display character for non-printing characters, from properties file.
    public static String getAsciiNonPrint() {
        String anp = getPropertyEntry(configPropertiesFile, "AsciiNonPrint");
        return (anp == null) ? "." : ((anp.equals("space")) ? " " : anp);
    }

    // Read ASCII strings for codes 0-255, from properties file. If string
    // value is "null", substitute value of ASCII_NON_PRINT.  If string is
    // "space", substitute string containing one space character.
    public static String[] getAsciiStrings() {
        String let = getPropertyEntry(configPropertiesFile, "AsciiTable");
        String placeHolder = getAsciiNonPrint();
        String[] lets = let.split(" +");
        int maxLength = 0;
        for (int i = 0; i < lets.length; i++) {
            if (lets[i].equals("null")) lets[i] = placeHolder;
            if (lets[i].equals("space")) lets[i] = " ";
            if (lets[i].length() > maxLength) maxLength = lets[i].length();
        }
        String padding = "        ";
        maxLength++;
        for (int i = 0; i < lets.length; i++)
            lets[i] = padding.substring(0, maxLength - lets[i].length()) + lets[i];
        return lets;
    }

    // Read and return integer property value for given file and property name.
    // Default value is returned if property file or name not found.
    private static int getIntegerProperty(String propertiesFile, String propertyName, int defaultValue) {
        int limit = defaultValue;  // just in case no entry is found
        Properties properties = loadPropertiesFromFile(propertiesFile);
        try {
            limit = Integer.parseInt(properties.getProperty(propertyName, Integer.toString(defaultValue)));
        }
        catch (NumberFormatException nfe) {
        } // do nothing, I already have a default
        return limit;
    }

    // Read assembly language file extensions from properties file.  Resulting
    // string is tokenized into array list (assume StringTokenizer default delimiters).
    private static ArrayList<String> getFileExtensions() {
        ArrayList<String> extensionsList = new ArrayList<>();
        String extensions = getPropertyEntry(configPropertiesFile, "Extensions");
        if (extensions != null) {
            StringTokenizer st = new StringTokenizer(extensions);
            while (st.hasMoreTokens())
                extensionsList.add(st.nextToken());
        }
        return extensionsList;
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
        String tools = getPropertyEntry(configPropertiesFile, "ExternalTools");
        if (tools != null) {
            StringTokenizer st = new StringTokenizer(tools, delimiter);
            while (st.hasMoreTokens())
                toolsList.add(st.nextToken());
        }
        return toolsList;
    }

    /**
     * Read and return property file value (if any) for requested property.
     *
     * @param propertiesFile name of properties file (do NOT include filename
     * extension, which is assumed to be ".properties")
     * @param propertyName String containing desired property name
     * @return String containing associated value; null if property not found
     */
    public static String getPropertyEntry(String propertiesFile, String propertyName) {
        return loadPropertiesFromFile(propertiesFile).getProperty(propertyName);
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
        Properties properties = new Properties();
        try {
            properties.load(Main.class.getResourceAsStream("/" + file + ".properties"));
        }
        catch (NullPointerException | IOException ioe) {
            // If it doesn't work, properties will be empty
        }
        return properties;
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
        Properties properties = loadPropertiesFromFile(syscallPropertiesFile);
        Enumeration<Object> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            overrides.add(new SyscallNumberOverride(key, properties.getProperty(key)));
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
            // NOTE 151016 - This seems to slow down startup - removing
//            MarsSplashScreen.showSplash(2000);

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
