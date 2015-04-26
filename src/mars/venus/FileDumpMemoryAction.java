package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import mars.Main;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.util.Binary;
import mars.util.MemoryDump;
import static mars.venus.VenusUI.getMainFrame;

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
 * Action for the File -> Save For Dump Memory menu item
 */
class FileDumpMemoryAction extends GuiAction {

    public FileDumpMemoryAction(String name, Icon icon, String descrip,
            int mnemonic, KeyStroke accel) {
        super(name, icon, descrip, mnemonic, accel);

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        String[] segmentArray = MemoryDump.getSegmentNames();

        String[] segmentListArray = new String[segmentArray.length];
        int[] segmentListBaseArray = new int[segmentArray.length];
        int[] segmentListHighArray = new int[segmentArray.length];
        
        // Calculate the actual highest address to be dumped.  For text segment, this depends on the
        // program length (number of machine code instructions).  For data segment, this depends on
        // how many MARS 4K word blocks have been referenced during assembly and/or execution.
        // Then generate label from concatentation of segmentArray[i], baseAddressArray[i]
        // and highAddressArray[i].  This lets user know exactly what range will be dumped.  Initially not
        // editable but maybe add this later.
        // If there is nothing to dump (e.g. address of first null == base address), then
        // the segment will not be listed.
        int segmentCount = 0;
        for (String segmentName : segmentArray) {
            Integer[] bounds = MemoryDump.getSegmentBounds(segmentName);
            int upperBound;
            try {
                upperBound = Main.memory.getAddressOfFirstNull(bounds[0], bounds[1]);
            }
            catch (AddressErrorException aee) {
                upperBound = bounds[0];
            }
            upperBound-=Memory.WORD_LENGTH_BYTES;
            
            if (upperBound >= bounds[0]) {
                segmentListBaseArray[segmentCount] = bounds[0];
                segmentListHighArray[segmentCount] = upperBound;
                segmentListArray[segmentCount] = segmentName + " (" + Binary.intToHexString(bounds[0]) + " - " + Binary.intToHexString(upperBound) + ")";
                segmentCount++;
            }
        }
        if (segmentCount == 0) {
            JOptionPane.showMessageDialog(getMainFrame(), "There is nothing to dump!", "MARS", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (segmentCount < segmentListArray.length) {
            String[] tempArray = new String[segmentCount];
            System.arraycopy(segmentListArray, 0, tempArray, 0, segmentCount);
            segmentListArray = tempArray;
        }

        new DumpMemoryDialog(segmentListArray, segmentListBaseArray, segmentListHighArray)
                .setVisible(true);
    }

}
