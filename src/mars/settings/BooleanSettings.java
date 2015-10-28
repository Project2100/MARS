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
 * Enumeration of boolean application settings represented as
 * {@code String}/{@code boolean} couples.
 * 
 * @author Project2100
 */
public enum BooleanSettings {

    EXTENDED_ASSEMBLER("ExtendedAssembler", true),
    BARE_MACHINE("BareMachine", false),
    ASSEMBLE_ON_OPEN("AssembleOnOpen", false),
    ASSEMBLE_ALL("AssembleAll", false),
    LABEL_WINDOW_VISIBILITY("LabelWindowVisibility", false),
    DISPLAY_ADDRESSES_IN_HEX("DisplayAddressesInHex", true),
    DISPLAY_VALUES_IN_HEX("DisplayValuesInHex", true),
    EXCEPTION_HANDLER("LoadExceptionHandler", false),
    DELAYED_BRANCHING("DelayedBranching", false),
    EDITOR_LINE_NUMBERS("EditorLineNumbersDisplayed", true),
    WARNINGS_ARE_ERRORS("WarningsAreErrors", false),
    PROGRAM_ARGUMENTS("ProgramArguments", false),
    DATA_SEGMENT_HIGHLIGHTING("DataSegmentHighlighting", true),
    REGISTERS_HIGHLIGHTING("RegistersHighlighting", true),
    START_AT_MAIN("StartAtMain", false),
    EDITOR_CURRENT_LINE_HIGHLIGHTING("EditorCurrentLineHighlighting", true),
    POPUP_INSTRUCTION_GUIDANCE("PopupInstructionGuidance", true),
    POPUP_SYSCALL_INPUT("PopupSyscallInput", false),
    GENERIC_TEXT_EDITOR("GenericTextEditor", false),
    AUTO_INDENT("AutoIndent", true),
    SELF_MODIFYING_CODE("SelfModifyingCode", false);

    final String identifier;
    boolean state;

    private BooleanSettings(String id, boolean def) {
        identifier = id;

        String prop = Main.properties.getProperty(identifier);
        this.state = Settings.preferences.getBoolean(identifier,
                prop != null ? Boolean.parseBoolean(prop) : def);
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
        if (state != newState) {
            try {
                Settings.preferences.putBoolean(identifier, newState);
                Settings.preferences.flush();
            }
            catch (SecurityException | BackingStoreException e) {
                Main.logger.log(Level.SEVERE, "Unable to save boolean setting: " + identifier, e);
            }
            Settings.changed();
        }
    }

    /**
     * Temporarily establish boolean setting. This setting will NOT be written
     * to persistent store! Currently this is used only when running MARS from
     * the command line
     *
     * @param value True to enable the setting, false otherwise.
     */
    public void setNoPersist(boolean value) {
        state = value;
    }
}
