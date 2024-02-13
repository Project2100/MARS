package mars.settings;

import java.awt.Color;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import mars.Main;

/*
 Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

 Developed by Pete Sanderson (psanderson@otterbein.edu)
 and Kenneth Vollmar (kenvollmar@missouristate.edu)

 Copyright (c) 2020 Andrea Proietto

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
 * Color settings for the Venus GUI
 *
 * @author Project2100
 */
public enum ColorSettings {

    TEXTSEGMENT_HIGHLIGHT("TextSegmentHighlight", 0x00ffff99, 0),
    TEXTSEGMENT_DELAYSLOT_HIGHLIGHT("TextSegmentDelaySlotHighlight", 0x0033ff00, 0),
    DATASEGMENT_HIGHLIGHT("DataSegmentHighlight", 0x0099ccff, 0),
    REGISTER_HIGHLIGHT("RegisterHighlight", 0x0099cc55, 0),
    EVEN_ROW("EvenRow", 0x00e0e0e0, 0),
    ODD_ROW("OddRow", 0x00ffffff, 0);

    final String bgID, fgID;
    final Color bgDefault, fgDefault;
    Color background, foreground;

    private ColorSettings(String identifier, int bg, int fg) {
        bgID = identifier + "Background";
        fgID = identifier + "Foreground";

        int val;

        try {
            val = Integer.decode(Settings.properties.getProperty(bgID));
        }
        catch (NumberFormatException | NullPointerException ex) {
            Main.logger.log(Level.WARNING, Settings.ERROR_MESSAGE,
                    new Object[] {bgID, ex.getMessage()});
            val = bg;
        }
        background = new Color(Settings.PREFS_NODE.getInt(bgID, val));
        bgDefault = new Color(val, true);

        try {
            val = Integer.decode(Settings.properties.getProperty(fgID));
        }
        catch (NumberFormatException | NullPointerException ex) {
            Main.logger.log(Level.WARNING, Settings.ERROR_MESSAGE,
                    new Object[] {fgID, ex.getMessage()});
            val = fg;
        }
        foreground = new Color(Settings.PREFS_NODE.getInt(fgID, val));
        fgDefault = new Color(val, true);

    }

    /**
     * Get the background color for this color setting.
     *
     * @return the background color
     */
    public Color getBackground() {
        return background;
    }

    /**
     * Get the default background color for this color setting.
     *
     * @return the default background color
     */
    public Color getDefaultBackground() {
        return bgDefault;
    }

    /**
     * * Get foreground color for this color setting.
     *
     * @return the foreground color
     */
    public Color getForeground() {
        return foreground;
    }

    /**
     * Get default foreground color for this color setting.
     *
     * @return the default foreground color
     */
    public Color getDefaultForeground() {
        return fgDefault;
    }

    /**
     * Set background color for this color setting.
     *
     * @param color the color to save
     */
    public void setBackground(Color color) {
        background = color;
        try {
            Settings.PREFS_NODE.putInt(bgID, background.getRGB());
            Settings.PREFS_NODE.flush();
        }
        catch (SecurityException | BackingStoreException e) {
            Main.logger.log(Level.SEVERE, "Unable to save color setting: " + bgID, e);
        }
    }

    /**
     * Set foreground color for this color setting.
     *
     * @param color the color to save
     */
    public void setForeground(Color color) {
        foreground = color;
        try {
            Settings.PREFS_NODE.putInt(fgID, foreground.getRGB());
            Settings.PREFS_NODE.flush();
        }
        catch (SecurityException | BackingStoreException e) {
            Main.logger.log(Level.WARNING, "Unable to save color setting: " + fgID, e);
        }
    }
}
