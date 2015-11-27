package mars.settings;

import java.awt.Color;
import java.util.Observable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import mars.Main;
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
 * object.
 * <p>
 * If the Preference objects fail due to security exceptions, changes to
 * settings will not carry over from one MARS session to the next.
 * <p>
 * Actual implementation of the Preference objects is platform-dependent. For
 * Windows, they are stored in Registry. To see, run {@code regedit} and browse
 * to: {@code HKEY_CURRENT_USER\Software\JavaSoft\Prefs\mars}
 *
 * @author Pete Sanderson
 * @see java.util.prefs.Preferences
 */
public class Settings extends Observable {

    // Preferences node - see documentation
    static final Preferences PREFS_NODE = Preferences.userNodeForPackage(Main.class);
    static final String ERROR_MESSAGE
            = "Invalid value retrieved from property \"{0}\": {1}";
    
    // Properties object which holds default settings
    static final Properties properties = Main.loadPropertiesFromFile(Main.SETTINGS_FILENAME);
    
    /**
     * Create Settings object and set to saved values. If saved values not
     * found, will set based on defaults stored in Settings.properties file. If
     * file problems, will set based on defaults stored in this class.
     */
    public Settings() {
    }

    static void changed() {
        Main.getSettings().setChanged();
        Main.getSettings().notifyObservers();
    }

    public final void AWTinit() {

        // For syntax styles, need to initialize from SyntaxUtilities defaults.
        // Taking care not to explicitly create a Color object, since it may trigger
        // Swing initialization (that caused problems for UC Berkeley when we
        // created Font objects here).  It shouldn't, but then again Font shouldn't
        // either but they said it did.  (see HeadlessException)   
        // On the other hand, the first statement of this method causes Color objects
        // to be created!  It is possible but a real pain in the rear to avoid using 
        // Color objects totally.  Requires new methods for the SyntaxUtilities class.
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

        for (int i = 0; i < syntaxStyleColorSettingsKeys.length; i++) {
            syntaxStyleColorSettingsValues[i] = PREFS_NODE.get(syntaxStyleColorSettingsKeys[i], syntaxStyleColorSettingsValues[i]);
            syntaxStyleBoldSettingsValues[i] = PREFS_NODE.getBoolean(syntaxStyleBoldSettingsKeys[i], syntaxStyleBoldSettingsValues[i]);
            syntaxStyleItalicSettingsValues[i] = PREFS_NODE.getBoolean(syntaxStyleItalicSettingsKeys[i], syntaxStyleItalicSettingsValues[i]);
        }
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
            PREFS_NODE.put(syntaxStyleColorSettingsKeys[index], syntaxStyleColorSettingsValues[index]);
            PREFS_NODE.putBoolean(syntaxStyleBoldSettingsKeys[index], syntaxStyleBoldSettingsValues[index]);
            PREFS_NODE.putBoolean(syntaxStyleItalicSettingsKeys[index], syntaxStyleItalicSettingsValues[index]);
            PREFS_NODE.flush();
        }
        catch (SecurityException | BackingStoreException se) {
            Main.logger.log(Level.SEVERE, "Can't save application settings!", se);
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
            catch (NumberFormatException e) {
                color = null;
            }
        return color;
    }
}
