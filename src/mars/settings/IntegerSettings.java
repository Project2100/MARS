package mars.settings;

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
/**
 *
 * @author Project2100
 */
public enum IntegerSettings {

    CARET_BLINK_RATE("CaretBlinkRate", 500),
    EDITOR_TAB_SIZE("EditorTabSize", 8),
    LABEL_SORT_STATE("LabelSortState", 0),
    EDITOR_POPUP_PREFIX_LENGTH("EditorPopupPrefixLength", 2);

    final String identifier;
    final int defaultValue;
    private int value;

    private IntegerSettings(String id, int def) {
        identifier = id;

        int val;
        try {
            val = Integer.decode(Settings.properties.getProperty(identifier));
        }
        catch (NumberFormatException | NullPointerException ex) {
            Main.logger.log(Level.WARNING, Settings.ERROR_MESSAGE,
                    new Object[] {identifier, ex.getMessage()});
            val = def;
        }

        value = Settings.PREFS_NODE.getInt(identifier, val);
        defaultValue = val;
    }

    public int get() {
        return value;
    }

    public void set(int value) {
        this.value = value;
        Settings.PREFS_NODE.putInt(identifier, value);
        try {
            Settings.PREFS_NODE.flush();
        }
        catch (SecurityException | BackingStoreException e) {
            Main.logger.log(Level.SEVERE, "Unable to save integer setting: " + identifier, e);
        }
        Settings.changed();
    }
}
