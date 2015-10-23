package mars.venus;

import mars.simulator.*;
import java.awt.event.*;
import javax.swing.*;
import mars.Main;

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
 * Action class for the Run -> Pause menu item (and toolbar icon)
 */
public class RunPauseAction extends AbstractAction {

    public RunPauseAction() {
        super("Pause", new ImageIcon(GuiAction.class.getResource(Main.imagesPath + "Pause16.png")));

        putValue(LARGE_ICON_KEY, new ImageIcon(GuiAction.class.getResource(Main.imagesPath + "Pause22.png")));
        putValue(SHORT_DESCRIPTION, "Pause the currently running program");
        putValue(MNEMONIC_KEY, KeyEvent.VK_P);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
    }

    /**
     *
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        Simulator.getInstance().stopExecution(this);
        // RunGoAction's "paused" method will do the cleanup.
    }

}
