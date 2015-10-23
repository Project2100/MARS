package mars.venus;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.UIManager;
import mars.*;

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
 * Class for the Run speed slider control. One is created and can be obtained
 * using getInstance().
 *
 * @author Pete Sanderson
 * @version August 2005
 */
public class RunSpeedPanel extends JPanel {

    /**
     * Constant that represents unlimited run speed. Compare with return value
     * of getRunSpeed() to determine if set to unlimited. At the unlimited
     * setting, the GUI will not attempt to update register and memory contents
     * as each instruction is executed. This is the only possible value for
     * command-line use of Mars.
     */
    public final static double UNLIMITED_SPEED = 40;

    private final static int SPEED_INDEX_MIN = 0;
    private final static int SPEED_INDEX_MAX = 40;
    private final static int SPEED_INDEX_INIT = 40;
    private static final int SPEED_INDEX_INTERACTION_LIMIT = 35;
    
    private final double[] speedTable = {
        .05, .1, .2, .3, .4, .5, 1, 2, 3, 4, 5, // 0-10
        6, 7, 8, 9, 10, 11, 12, 13, 14, 15, // 11-20
        16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // 21-30
        26, 27, 28, 29, 30, UNLIMITED_SPEED, UNLIMITED_SPEED, // 31-37
        UNLIMITED_SPEED, UNLIMITED_SPEED, UNLIMITED_SPEED // 38-40
    };
    
    private volatile int runSpeedIndex = SPEED_INDEX_MAX;
    private JLabel sliderLabel = null;
    private JSlider runSpeedSlider = null;
    
    private static RunSpeedPanel runSpeedPanel = null;

    /**
     * Retrieve the run speed panel object
     *
     * @return the run speed panel
     */
    public static RunSpeedPanel getInstance() {
        if (runSpeedPanel == null) {
            runSpeedPanel = new RunSpeedPanel();
            Main.runSpeedPanelExists = true; // DPS 24 July 2008 (needed for standalone tools)
        }
        return runSpeedPanel;
    }

    private RunSpeedPanel() {
        
        super(new BorderLayout());
        runSpeedSlider = new JSlider(JSlider.HORIZONTAL, SPEED_INDEX_MIN, SPEED_INDEX_MAX, SPEED_INDEX_INIT);
        runSpeedSlider.setSize(100, runSpeedSlider.getHeight());
        runSpeedSlider.setMaximumSize(runSpeedSlider.getSize());
        runSpeedSlider.setMajorTickSpacing(5);
        runSpeedSlider.setMinorTickSpacing(1);
        runSpeedSlider.setSnapToTicks(true);
        runSpeedSlider.setPaintTicks(true);
        
        runSpeedSlider.addChangeListener((event) -> {
            if (!runSpeedSlider.getValueIsAdjusting())
                runSpeedIndex = runSpeedSlider.getValue();
            sliderLabel.setText(setLabel(runSpeedSlider.getValue()));
        });

        sliderLabel = new JLabel(setLabel(runSpeedIndex));
        sliderLabel.setHorizontalAlignment(JLabel.CENTER);

        add(runSpeedSlider, BorderLayout.CENTER);
        add(sliderLabel, UIManager.getLookAndFeel().getClass().getName()
                .equals(UIManager.getCrossPlatformLookAndFeelClassName())
                ? BorderLayout.NORTH
                : BorderLayout.EAST);

        setToolTipText("Simulation speed. At "
                + ((int) speedTable[SPEED_INDEX_INTERACTION_LIMIT])
                + " inst/sec or less, all tables are updated after each instruction.");
    }

    /**
     * returns current run speed setting, in instructions/second. Unlimited
     * speed setting is equal to RunSpeedPanel.UNLIMITED_SPEED
     *
     * @return run speed setting in instructions/second.
     */
    public double getRunSpeed() {
        return speedTable[runSpeedIndex];
    }

    // Set label wording depending on current speed setting
    private String setLabel(int index) {
        //String reurn= "  Run speed: "+(index <= SPEED_INDEX_INTERACTION_LIMIT?String.format("", speedTable[index])+" inst/sec":"max (no interaction)");
        String result = "  Run speed: ";
        if (index <= SPEED_INDEX_INTERACTION_LIMIT) {
            if (speedTable[index] < 1)
                result += speedTable[index];
            else
                result += ((int) speedTable[index]);
            result += " inst/sec";
        }
        else
            result += ("max (no interaction)");
        return result;
    }
}