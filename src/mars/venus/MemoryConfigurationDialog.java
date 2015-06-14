package mars.venus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import mars.Main;
import mars.Settings;
import mars.mips.hardware.MemoryConfiguration;
import mars.mips.hardware.MemoryConfigurations;
import mars.simulator.Simulator;
import mars.util.Binary;

/*
 Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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
final class MemoryConfigurationDialog extends JDialog implements ActionListener {

    JTextField[] addressDisplay;
    JLabel[] nameDisplay;
    ConfigurationButton selectedConfigurationButton, initialConfigurationButton;
    GuiAction thisAction;

    MemoryConfigurationDialog(GuiAction action) {
        super(Main.getGUI().mainFrame, "MIPS Memory Configuration", true);
        thisAction = action;

        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel configInfo = new JPanel(new FlowLayout());
        MemoryConfigurations.buildConfigurationCollection();
        configInfo.add(buildConfigChooser());
        configInfo.add(buildConfigDisplay());
        dialogPanel.add(configInfo);
        dialogPanel.add(buildControlPanel(), BorderLayout.SOUTH);

        setContentPane(dialogPanel);

        setDefaultCloseOperation(
                JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                performClose();
            }
        });
        pack();
        setLocationRelativeTo(Main.getGUI().mainFrame);
    }

    private Component buildConfigChooser() {
        JPanel chooserPanel = new JPanel(new GridLayout(4, 1));
        ButtonGroup choices = new ButtonGroup();
        Iterator<MemoryConfiguration> configurationsIterator = MemoryConfigurations.getConfigurationsIterator();
        while (configurationsIterator.hasNext()) {
            MemoryConfiguration config = configurationsIterator.next();
            ConfigurationButton button = new ConfigurationButton(config);
            button.addActionListener(this);
            if (button.isSelected()) {
                this.selectedConfigurationButton = button;
                this.initialConfigurationButton = button;
            }
            choices.add(button);
            chooserPanel.add(button);
        }
        chooserPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        return chooserPanel;
    }

    private Component buildConfigDisplay() {
        JPanel displayPanel = new JPanel();
        MemoryConfiguration config = MemoryConfigurations.getCurrentConfiguration();
        String[] configurationItemNames = config.getConfigurationItemNames();
        int numItems = configurationItemNames.length;
        JPanel namesPanel = new JPanel(new GridLayout(numItems, 1));
        JPanel valuesPanel = new JPanel(new GridLayout(numItems, 1));
        Font monospaced = new Font("Monospaced", Font.PLAIN, 12);
        nameDisplay = new JLabel[numItems];
        //   for (int i=numItems-1; i >= 0; i--) {
        //      namesPanel.add(new JLabel(configurationItemNames[i]));
        //   }
        addressDisplay = new JTextField[numItems];
        for (int i = 0; i < numItems; i++) {
            nameDisplay[i] = new JLabel();
            addressDisplay[i] = new JTextField();
            addressDisplay[i].setEditable(false);
            addressDisplay[i].setFont(monospaced);
        }
        // Display vertically from high to low memory addresses so
        // add the components in reverse order.
        for (int i = addressDisplay.length - 1; i >= 0; i--) {
            namesPanel.add(nameDisplay[i]);
            valuesPanel.add(addressDisplay[i]);
        }
        setConfigDisplay(config);
        Box columns = Box.createHorizontalBox();
        columns.add(valuesPanel);
        columns.add(Box.createHorizontalStrut(6));
        columns.add(namesPanel);
        displayPanel.add(columns);
        return displayPanel;
    }

    // Carry out action for the radio buttons.
    @Override
    public void actionPerformed(ActionEvent e) {
        MemoryConfiguration config = ((ConfigurationButton) e.getSource()).getConfiguration();
        setConfigDisplay(config);
        this.selectedConfigurationButton = (ConfigurationButton) e.getSource();
    }

    // Row of control buttons to be placed along the button of the dialog
    private Component buildControlPanel() {
        Box controlPanel = Box.createHorizontalBox();
        JButton okButton = new JButton("Apply and Close");
        okButton.setToolTipText(HighlightingDialog.CLOSE_TOOL_TIP_TEXT);
        okButton.addActionListener((ActionEvent e) -> {
            performApply();
            performClose();
        });
        JButton applyButton = new JButton("Apply");
        applyButton.setToolTipText(HighlightingDialog.APPLY_TOOL_TIP_TEXT);
        applyButton.addActionListener((ActionEvent e) -> {
            performApply();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText(HighlightingDialog.CANCEL_TOOL_TIP_TEXT);
        cancelButton.addActionListener((ActionEvent e) -> {
            performClose();
        });
        JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText(HighlightingDialog.RESET_TOOL_TIP_TEXT);
        resetButton.addActionListener((ActionEvent e) -> {
            performReset();
        });
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(okButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(applyButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(cancelButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(resetButton);
        controlPanel.add(Box.createHorizontalGlue());
        return controlPanel;
    }

    private void performApply() {
        if (MemoryConfigurations.setCurrentConfiguration(this.selectedConfigurationButton.getConfiguration())) {
            Settings.StringSettings.MEMORY_CONFIGURATION.set(selectedConfigurationButton.getConfiguration().getConfigurationIdentifier());
//            Main.getSettings().setMemoryConfiguration(this.selectedConfigurationButton.getConfiguration().getConfigurationIdentifier());
            Main.getGUI().registersTab.clearHighlighting();
            Main.getGUI().registersTab.updateRegisters();
            Main.getGUI().executePane.getDataSegmentWindow().updateBaseAddressComboBox();
            // 21 July 2009 Re-assemble if the situation demands it to maintain consistency.
            // 20150519 - Do reassemble if executePane is visible
            if (Main.getGUI().executePane.isShowing()) {
                // Stop execution if executing -- should NEVER happen because this 
                // Action's widget is disabled during MIPS execution.
                // 20150519 - Actually leaving this here as a feature
                if (VenusUI.getStarted())
                    Simulator.getInstance().stopExecution(thisAction);
                ExecuteAction.assemble();
            }
        }
    }

    private void performClose() {
        this.setVisible(false);
        this.dispose();
    }

    private void performReset() {
        this.selectedConfigurationButton = this.initialConfigurationButton;
        this.selectedConfigurationButton.setSelected(true);
        setConfigDisplay(this.selectedConfigurationButton.getConfiguration());
    }

    // Set name values in JLabels and address values in the JTextFields
    private void setConfigDisplay(MemoryConfiguration config) {
        String[] configurationItemNames = config.getConfigurationItemNames();
        int[] configurationItemValues = config.getConfigurationItemValues();
        // Will use TreeMap to extract list of address-name pairs sorted by 
        // hex-stringified address. This will correctly handle kernel addresses,
        // whose int values are negative and thus normal sorting yields incorrect
        // results.  There can be duplicate addresses, so I concatenate the name
        // onto the address to make each key unique.  Then slice off the name upon
        // extraction. 
        TreeMap<String, String> treeSortedByAddress = new TreeMap<>();
        for (int i = 0; i < configurationItemValues.length; i++)
            treeSortedByAddress.put(Binary.intToHexString(configurationItemValues[i]) + configurationItemNames[i], configurationItemNames[i]);
        Iterator<Map.Entry<String, String>> setSortedByAddress = treeSortedByAddress.entrySet().iterator();
        
        Map.Entry<String, String> pair;
        int addressStringLength = Binary.intToHexString(configurationItemValues[0]).length();
        for (int i = 0; i < configurationItemValues.length; i++) {
            pair = setSortedByAddress.next();
            nameDisplay[i].setText(pair.getValue());
            addressDisplay[i].setText(pair.getKey().substring(0, addressStringLength));
        }
    }

    // Handy class to connect button to its configuration...     	
    private class ConfigurationButton extends JRadioButton {

        private final MemoryConfiguration configuration;

        public ConfigurationButton(MemoryConfiguration config) {
            super(config.getConfigurationName(), config == MemoryConfigurations.getCurrentConfiguration());
            this.configuration = config;
        }

        public MemoryConfiguration getConfiguration() {
            return configuration;
        }

    }

}
