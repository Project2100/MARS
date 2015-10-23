package mars.settings;

import java.util.Arrays;
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
public enum StringSettings {

    EXCEPTION_HANDLER_FILE("ExceptionHandler", ""),
    MEMORY_CONFIGURATION("MemoryConfiguration", ""),
    TEXT_COLUMN_ORDER("TextColumnOrder", "0 1 2 3 4"),
    OPEN_DIRECTORY("OpenDirectory", System.getProperty("user.dir")),
    SAVE_DIRECTORY("SaveDirectory", System.getProperty("user.dir"));

    final String identifier;
    final String vDefault;
    private String value;

    private StringSettings(String id, String def) {
        identifier = id;

        String val = Main.props.getProperty(identifier);

        vDefault = val = (val == null ? def : val);

        value = Settings.preferences.get(identifier,
                val != null ? val : vDefault);
    }

    public String get() {
        return value;
    }

    public void set(String value) {
        this.value = value;
        Settings.preferences.put(identifier, value);
        try {
            Settings.preferences.flush();
        }
        catch (SecurityException | BackingStoreException e) {
            Main.logger.log(Level.SEVERE, "Unable to save string setting: " + identifier, e);
        }
        Settings.changed();
    }

    /**
     * Order of text segment display columns (there are 5, numbered 0 to 4).
     *
     * @return Array of integers indicating the column order. Original order is
     * {@code "0 1 2 3 4"}.
     */
    public static int[] getTextColumnOrder() {
        return Arrays.stream(TEXT_COLUMN_ORDER.value
                .split(" "))
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
