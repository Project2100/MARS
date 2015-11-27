package mars.settings;

import java.awt.Font;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import mars.Main;

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
// DPS 3-Oct-2012
// Changed default font family from "Courier New" to "Monospaced" after receiving reports that Mac were not
// correctly rendering the left parenthesis character in the editor or text segment display.
// See http://www.mirthcorp.com/community/issues/browse/MIRTH-1921?page=com.atlassian.jira.plugin.system.issuetabpanels:all-tabpanel
/**
 * 
 * @author Project2100
 */
public enum FontSettings {

    EDITOR_FONT("EditorFont", "Monospaced", Font.PLAIN, 12),
    EVEN_ROW_FONT("EvenRowFont", "Monospaced", Font.PLAIN, 12),
    ODD_ROW_FONT("OddRowFont", "Monospaced", Font.PLAIN, 12),
    TEXTSEGMENT_HIGHLIGHT_FONT("TextSegmentHighlightFont", "Monospaced", Font.PLAIN, 12),
    TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FONT("TextSegmentDelaySlotHighlightFont", "Monospaced", Font.PLAIN, 12),
    DATASEGMENT_HIGHLIGHT_FONT("DataSegmentHighlightFont", "Monospaced", Font.PLAIN, 12),
    REGISTER_HIGHLIGHT_FONT("RegisterHighlightFont", "Monospaced", Font.PLAIN, 12);

    final String familyID, styleID, sizeID;
    private final Font fDefault;
    private Font font;

    private FontSettings(String identifier, String type, int style, int size) {

        familyID = identifier + "Family";
        styleID = identifier + "Style";
        sizeID = identifier + "Size";

        String val1 = Settings.properties.getProperty(familyID);
        if (val1 == null) val1 = type;

        int val2;
        try {
            val2 = Integer.decode(Settings.properties.getProperty(styleID));
        }
        catch (NumberFormatException | NullPointerException ex) {
            Main.logger.log(Level.WARNING, Settings.ERROR_MESSAGE,
                    new Object[] {styleID, ex.getMessage()});
            val2 = style;
        }

        int val3;
        try {
            val3 = Integer.decode(Settings.properties.getProperty(sizeID));
        }
        catch (NumberFormatException | NullPointerException ex) {
            Main.logger.log(Level.WARNING, Settings.ERROR_MESSAGE,
                    new Object[] {sizeID, ex.getMessage()});
            val3 = size;
        }

        font = new Font(
                Settings.PREFS_NODE.get(familyID, val1),
                Settings.PREFS_NODE.getInt(styleID, val2),
                Settings.PREFS_NODE.getInt(sizeID, val3));
        fDefault = new Font(val1, val2, val3);
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
        Settings.PREFS_NODE.put(familyID, font.getFamily());
        Settings.PREFS_NODE.putInt(styleID, font.getStyle());
        Settings.PREFS_NODE.putInt(sizeID, font.getSize());
        try {
            Settings.PREFS_NODE.flush();
        }
        catch (SecurityException | BackingStoreException e) {
            Main.logger.log(Level.SEVERE, "Unable to save font setting: " + familyID, e);
        }
        Settings.changed();
    }
}
