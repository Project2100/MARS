package mars.venus;

import java.awt.event.ActionEvent;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.KeyStroke;

/*
 Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

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
 * Parent class for Action subclasses to be defined for every menu/toolbar
 * option.
 */
class GuiAction extends AbstractAction {
    VenusUI mainUI;
    Consumer<ActionEvent> delegate;
    BiConsumer<GuiAction, ActionEvent> del;

    protected GuiAction(String name, Icon icon, String descrip,
            Integer mnemonic, KeyStroke accel, VenusUI gui) {
        super(name, icon);
        putValue(SHORT_DESCRIPTION, descrip);
        if (mnemonic != null)
            putValue(MNEMONIC_KEY, mnemonic);
        if (accel != null)
            putValue(ACCELERATOR_KEY, accel);
        mainUI=gui;
    }
    protected GuiAction(String name, Icon icon, String descrip,
            Integer mnemonic, KeyStroke accel) {
        this(name, icon, descrip, mnemonic, accel, (VenusUI)null);
    }
    
    protected GuiAction(String name, Icon icon, String descrip,
            Integer mnemonic, KeyStroke accel, Consumer <ActionEvent> c) {
        super(name, icon);
        putValue(SHORT_DESCRIPTION, descrip);
        if (mnemonic != null)
            putValue(MNEMONIC_KEY, mnemonic);
        if (accel != null)
            putValue(ACCELERATOR_KEY, accel);
        delegate=c;
        mainUI=null;
    }
    
    protected GuiAction(String name, Icon icon, String descrip,
            Integer mnemonic, KeyStroke accel, BiConsumer <GuiAction, ActionEvent> c) {
        super(name, icon);
        putValue(SHORT_DESCRIPTION, descrip);
        if (mnemonic != null)
            putValue(MNEMONIC_KEY, mnemonic);
        if (accel != null)
            putValue(ACCELERATOR_KEY, accel);
        del=c;
        mainUI=null;
    }
    
    /**
     * ActionListener's actionPerformed(). @see java.awt.event.ActionListener
     *
     * @param e the event dispatched by the EDT
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (del != null) del.accept(this, e);
        else delegate.accept(e);
    }
}
