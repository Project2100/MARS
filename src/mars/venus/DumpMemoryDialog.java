package mars.venus;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.LayoutStyle;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import mars.Main;
import mars.mips.dump.DumpFormat;
import mars.mips.dump.DumpFormatLoader;
import mars.mips.hardware.AddressErrorException;

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
 *
 * @author Project2100
 */
final class DumpMemoryDialog extends JDialog {

    private static final String title = "Dump Memory To File";

    public DumpMemoryDialog(String[] segmentListArray, int[] segmentListBaseArray, int[] segmentListHighArray) {
        super(Main.getGUI().mainFrame, title, true);

        JComboBox<String> segmentListSelector = new JComboBox<>(segmentListArray);
        segmentListSelector.setSelectedIndex(0);

        JComboBox<DumpFormat> formatListSelector = new JComboBox<>((new DumpFormatLoader()).getDumpFormatsArray());
        formatListSelector.setRenderer(new BasicComboBoxRenderer() {
            // Display tool tip for dump format list items.  Got the technique from
            // http://forum.java.sun.com/thread.jspa?threadID=488762&messageID=2292482

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                if (isSelected) {
                    if (formatListSelector.isPopupVisible()) setOpaque(true);
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                }
                else {
                    setOpaque(false);
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }

                setText((value == null) ? "" : value.toString());

                if (index >= 0 && (formatListSelector.getItemAt(index)).getDescription() != null)
                    setToolTipText(formatListSelector.getItemAt(index).getDescription());
                return this;
            }
        });
        formatListSelector.setSelectedIndex(0);

        JButton dumpButton = new JButton("Dump To File...");
        dumpButton.addActionListener((event) -> {
            if (performDump(segmentListBaseArray[segmentListSelector.getSelectedIndex()], segmentListHighArray[segmentListSelector.getSelectedIndex()], (DumpFormat) formatListSelector.getSelectedItem()))
                DumpMemoryDialog.this.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener((event) -> DumpMemoryDialog.this.dispose());

        JLabel l1 = new JLabel("Memory Segment:");
        JLabel l2 = new JLabel("Dump Format:");

        GroupLayout l = new GroupLayout(getContentPane());
        getContentPane().setLayout(l);
        l.setAutoCreateContainerGaps(true);
        l.setHorizontalGroup(l.createSequentialGroup()
                .addGroup(l.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addGroup(l.createSequentialGroup()
                                .addComponent(l1)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(true, segmentListSelector)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(l2)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(formatListSelector))
                        .addGroup(l.createSequentialGroup()
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(dumpButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(cancelButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))));
        l.setVerticalGroup(l.createSequentialGroup()
                .addGroup(l.createBaselineGroup(false, false)
                        .addComponent(l1)
                        .addComponent(segmentListSelector)
                        .addComponent(l2)
                        .addComponent(formatListSelector))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(l.createParallelGroup()
                        .addComponent(dumpButton)
                        .addComponent(cancelButton)));

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(Main.getGUI().mainFrame);
    }

    // User has clicked "Dump" button, so launch a file chooser then get
    // segment (memory range) and format selections and save to the file.
    private boolean performDump(int firstAddress, int lastAddress, DumpFormat format) {
        File theFile;
        boolean operationOK = false;
        JFileChooser saveDialog = new JFileChooser(Main.getGUI().getEditor().getCurrentSaveDirectory());
        saveDialog.setDialogTitle(title);
        while (!operationOK) {
            int decision = saveDialog.showSaveDialog(Main.getGUI().mainFrame);
            if (decision != JFileChooser.APPROVE_OPTION) return false;
            theFile = saveDialog.getSelectedFile();
            operationOK = true;
            if (theFile.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(Main.getGUI().mainFrame, "File " + theFile.getName() + " already exists.  Do you wish to overwrite it?", "Overwrite existing file?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                switch (overwrite) {
                    case JOptionPane.YES_OPTION:
                        operationOK = true;
                        break;
                    case JOptionPane.NO_OPTION:
                        operationOK = false;
                        break;
                    case JOptionPane.CANCEL_OPTION:
                        return false;
                    default:
                        // should never occur
                        return false;
                }
            }
            if (operationOK)
                try {
                    format.dumpMemoryRange(theFile, firstAddress, lastAddress);
                }
                catch (AddressErrorException | IOException aee) {
                }
        }
        return true;
    }
}
