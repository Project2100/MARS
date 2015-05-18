/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import mars.Main;
import mars.Settings;

/**
 *
 * @author Project2100
 */
final class HighlightingDialog extends JDialog {

    // NOTE: These must follow same sequence and buttons must
    //       follow this sequence too!
    private static final int[] backgroundSettingPositions = {
        Settings.TEXTSEGMENT_HIGHLIGHT_BACKGROUND,
        Settings.TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_BACKGROUND,
        Settings.DATASEGMENT_HIGHLIGHT_BACKGROUND,
        Settings.REGISTER_HIGHLIGHT_BACKGROUND,
        Settings.EVEN_ROW_BACKGROUND,
        Settings.ODD_ROW_BACKGROUND
    };

    private static final int[] foregroundSettingPositions = {
        Settings.TEXTSEGMENT_HIGHLIGHT_FOREGROUND,
        Settings.TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FOREGROUND,
        Settings.DATASEGMENT_HIGHLIGHT_FOREGROUND,
        Settings.REGISTER_HIGHLIGHT_FOREGROUND,
        Settings.EVEN_ROW_FOREGROUND,
        Settings.ODD_ROW_FOREGROUND
    };

    private static final int[] fontSettingPositions = {
        Settings.TEXTSEGMENT_HIGHLIGHT_FONT,
        Settings.TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FONT,
        Settings.DATASEGMENT_HIGHLIGHT_FONT,
        Settings.REGISTER_HIGHLIGHT_FONT,
        Settings.EVEN_ROW_FONT,
        Settings.ODD_ROW_FONT
    };

    JButton[] backgroundButtons;
    JButton[] foregroundButtons;
    JButton[] fontButtons;
    JCheckBox[] defaultCheckBoxes;
    JLabel[] samples;
    Color[] currentNondefaultBackground, currentNondefaultForeground;
    Color[] initialSettingsBackground, initialSettingsForeground;
    Font[] initialFont, currentFont, currentNondefaultFont;
    JButton dataHighlightButton, registerHighlightButton;
    boolean currentDataHighlightSetting, initialDataHighlightSetting;
    boolean currentRegisterHighlightSetting, initialRegisterHighlightSetting;

    private static final int gridVGap = 2;
    private static final int gridHGap = 2;
    // Tool tips for color buttons
    private static final String SAMPLE_TOOL_TIP_TEXT = "Preview based on background and text color settings";
    private static final String BACKGROUND_TOOL_TIP_TEXT = "Click, to select background color";
    private static final String FOREGROUND_TOOL_TIP_TEXT = "Click, to select text color";
    private static final String FONT_TOOL_TIP_TEXT = "Click, to select text font";
    private static final String DEFAULT_TOOL_TIP_TEXT = "Check, to select default color (disables color select buttons)";
    // Tool tips for the control buttons along the bottom
    public static final String CLOSE_TOOL_TIP_TEXT = "Apply current settings and close dialog";
    public static final String APPLY_TOOL_TIP_TEXT = "Apply current settings now and leave dialog open";
    public static final String RESET_TOOL_TIP_TEXT = "Reset to initial settings without applying";
    public static final String CANCEL_TOOL_TIP_TEXT = "Close dialog without applying current settings";
    // Tool tips for the data and register highlighting enable/disable controls
    private static final String DATA_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT = "Click, to enable or disable highlighting in Data Segment window";
    private static final String REGISTER_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT = "Click, to enable or disable highlighting in Register windows";
    private static final String fontButtonText = "font";

    public HighlightingDialog() {

        super(Main.getGUI().mainFrame, "Runtime Table Highlighting Colors and Fonts", true);
        setContentPane(buildDialogPanel());
        pack();
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(Main.getGUI().mainFrame);
    }

    // The dialog box that appears when menu item is selected.
    private JPanel buildDialogPanel() {
        JPanel contents = new JPanel(new BorderLayout(20, 20));
        contents.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel patches = new JPanel(new GridLayout(backgroundSettingPositions.length, 4, gridVGap, gridHGap));
        currentNondefaultBackground = new Color[backgroundSettingPositions.length];
        currentNondefaultForeground = new Color[backgroundSettingPositions.length];
        initialSettingsBackground = new Color[backgroundSettingPositions.length];
        initialSettingsForeground = new Color[backgroundSettingPositions.length];
        initialFont = new Font[backgroundSettingPositions.length];
        currentFont = new Font[backgroundSettingPositions.length];
        currentNondefaultFont = new Font[backgroundSettingPositions.length];

        backgroundButtons = new JButton[backgroundSettingPositions.length];
        foregroundButtons = new JButton[backgroundSettingPositions.length];
        fontButtons = new JButton[backgroundSettingPositions.length];
        defaultCheckBoxes = new JCheckBox[backgroundSettingPositions.length];
        samples = new JLabel[backgroundSettingPositions.length];
        for (int i = 0; i < backgroundSettingPositions.length; i++) {
            backgroundButtons[i] = new ColorSelectButton();
            foregroundButtons[i] = new ColorSelectButton();
            fontButtons[i] = new JButton(fontButtonText);
            defaultCheckBoxes[i] = new JCheckBox();
            samples[i] = new JLabel(" preview ");
            backgroundButtons[i].addActionListener(new BackgroundChanger(i));
            foregroundButtons[i].addActionListener(new ForegroundChanger(i));
            fontButtons[i].addActionListener(new FontChanger(i));
            defaultCheckBoxes[i].addItemListener(new DefaultChanger(i));
            samples[i].setToolTipText(SAMPLE_TOOL_TIP_TEXT);
            backgroundButtons[i].setToolTipText(BACKGROUND_TOOL_TIP_TEXT);
            foregroundButtons[i].setToolTipText(FOREGROUND_TOOL_TIP_TEXT);
            fontButtons[i].setToolTipText(FONT_TOOL_TIP_TEXT);
            defaultCheckBoxes[i].setToolTipText(DEFAULT_TOOL_TIP_TEXT);
        }

        initializeButtonColors();

        for (int i = 0; i < backgroundSettingPositions.length; i++) {
            patches.add(backgroundButtons[i]);
            patches.add(foregroundButtons[i]);
            patches.add(fontButtons[i]);
            patches.add(defaultCheckBoxes[i]);
        }

        JPanel descriptions = new JPanel(new GridLayout(backgroundSettingPositions.length, 1, gridVGap, gridHGap));
        // Note the labels have to match buttons by position...
        descriptions.add(new JLabel("Text Segment highlighting", SwingConstants.RIGHT));
        descriptions.add(new JLabel("Text Segment Delay Slot highlighting", SwingConstants.RIGHT));
        descriptions.add(new JLabel("Data Segment highlighting *", SwingConstants.RIGHT));
        descriptions.add(new JLabel("Register highlighting *", SwingConstants.RIGHT));
        descriptions.add(new JLabel("Even row normal", SwingConstants.RIGHT));
        descriptions.add(new JLabel("Odd row normal", SwingConstants.RIGHT));

        JPanel sample = new JPanel(new GridLayout(backgroundSettingPositions.length, 1, gridVGap, gridHGap));
        for (int i = 0; i < backgroundSettingPositions.length; i++)
            sample.add(samples[i]);

        JPanel instructions = new JPanel(new FlowLayout(FlowLayout.CENTER));
        // create deaf, dumb and blind checkbox, for illustration
        JCheckBox illustrate = new JCheckBox() {
            @Override
            protected void processMouseEvent(MouseEvent e) {
            }

            @Override
            protected void processKeyEvent(KeyEvent e) {
            }
        };
        illustrate.setSelected(true);
        instructions.add(illustrate);
        instructions.add(new JLabel("= use default colors (disables color selection buttons)"));
        int spacer = 10;
        Box mainArea = Box.createHorizontalBox();
        mainArea.add(Box.createHorizontalGlue());
        mainArea.add(descriptions);
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(Box.createHorizontalGlue());
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(sample);
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(Box.createHorizontalGlue());
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(patches);

        contents.add(mainArea, BorderLayout.EAST);
        contents.add(instructions, BorderLayout.NORTH);

        // Control highlighting enable/disable for Data Segment window and Register windows
        JPanel dataRegisterHighlightControl = new JPanel(new GridLayout(2, 1));
        dataHighlightButton = new JButton();
        dataHighlightButton.setText(getHighlightControlText(currentDataHighlightSetting));
        dataHighlightButton.setToolTipText(DATA_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT);
        dataHighlightButton.addActionListener((ActionEvent e) -> {
            currentDataHighlightSetting = !currentDataHighlightSetting;
            dataHighlightButton.setText(getHighlightControlText(currentDataHighlightSetting));
        });
        registerHighlightButton = new JButton();
        registerHighlightButton.setText(getHighlightControlText(currentRegisterHighlightSetting));
        registerHighlightButton.setToolTipText(REGISTER_HIGHLIGHT_ENABLE_TOOL_TIP_TEXT);
        registerHighlightButton.addActionListener((ActionEvent e) -> {
            currentRegisterHighlightSetting = !currentRegisterHighlightSetting;
            registerHighlightButton.setText(getHighlightControlText(currentRegisterHighlightSetting));
        });
        JPanel dataHighlightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel registerHighlightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataHighlightPanel.add(new JLabel("* Data Segment highlighting is"));
        dataHighlightPanel.add(dataHighlightButton);
        registerHighlightPanel.add(new JLabel("* Register highlighting is"));
        registerHighlightPanel.add(registerHighlightButton);
        dataRegisterHighlightControl.setBorder(new LineBorder(Color.BLACK));
        dataRegisterHighlightControl.add(dataHighlightPanel);
        dataRegisterHighlightControl.add(registerHighlightPanel);

        // Bottom row - the control buttons for Apply&Close, Apply, Cancel
        Box controlPanel = Box.createHorizontalBox();
        JButton okButton = new JButton("Apply and Close");
        okButton.setToolTipText(CLOSE_TOOL_TIP_TEXT);
        okButton.addActionListener((ActionEvent e) -> {
            setHighlightingSettings();
            dispose();
        });
        JButton applyButton = new JButton("Apply");
        applyButton.setToolTipText(APPLY_TOOL_TIP_TEXT);
        applyButton.addActionListener((ActionEvent e) -> {
            setHighlightingSettings();
        });
        JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText(RESET_TOOL_TIP_TEXT);
        resetButton.addActionListener((ActionEvent e) -> {
            resetButtonColors();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText(CANCEL_TOOL_TIP_TEXT);
        cancelButton.addActionListener((ActionEvent e) -> dispose());
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(okButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(applyButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(cancelButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(resetButton);
        controlPanel.add(Box.createHorizontalGlue());

        JPanel allControls = new JPanel(new GridLayout(2, 1));
        allControls.add(dataRegisterHighlightControl);
        allControls.add(controlPanel);
        contents.add(allControls, BorderLayout.SOUTH);
        return contents;
    }

    private String getHighlightControlText(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    // Called once, upon dialog setup.
    private void initializeButtonColors() {
        Settings settings = Main.getSettings();
        LineBorder lineBorder = new LineBorder(Color.BLACK);
        Color backgroundSetting, foregroundSetting;
        Font fontSetting;
        for (int i = 0; i < backgroundSettingPositions.length; i++) {
            backgroundSetting = settings.getColorSettingByPosition(backgroundSettingPositions[i]);
            foregroundSetting = settings.getColorSettingByPosition(foregroundSettingPositions[i]);
            fontSetting = settings.getFontByPosition(fontSettingPositions[i]);
            backgroundButtons[i].setBackground(backgroundSetting);
            foregroundButtons[i].setBackground(foregroundSetting);
            fontButtons[i].setFont(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT); //fontSetting);
            fontButtons[i].setMargin(new Insets(4, 4, 4, 4));
            initialFont[i] = currentFont[i] = fontSetting;
            currentNondefaultBackground[i] = backgroundSetting;
            currentNondefaultForeground[i] = foregroundSetting;
            currentNondefaultFont[i] = fontSetting;
            initialSettingsBackground[i] = backgroundSetting;
            initialSettingsForeground[i] = foregroundSetting;
            samples[i].setOpaque(true); // otherwise, background color will not be rendered
            samples[i].setBorder(lineBorder);
            samples[i].setBackground(backgroundSetting);
            samples[i].setForeground(foregroundSetting);
            samples[i].setFont(fontSetting);
            boolean usingDefaults = backgroundSetting.equals(settings.getDefaultColorSettingByPosition(backgroundSettingPositions[i]))
                    && foregroundSetting.equals(settings.getDefaultColorSettingByPosition(foregroundSettingPositions[i]))
                    && fontSetting.equals(settings.getDefaultFontByPosition(fontSettingPositions[i]));
            defaultCheckBoxes[i].setSelected(usingDefaults);
            backgroundButtons[i].setEnabled(!usingDefaults);
            foregroundButtons[i].setEnabled(!usingDefaults);
            fontButtons[i].setEnabled(!usingDefaults);
        }
        currentDataHighlightSetting = initialDataHighlightSetting = settings.getDataSegmentHighlighting();
        currentRegisterHighlightSetting = initialRegisterHighlightSetting = settings.getRegistersHighlighting();
    }

    // Set the color settings according to current button colors.  Occurs when "Apply" selected.
    private void setHighlightingSettings() {
        Settings settings = Main.getSettings();
        for (int i = 0; i < backgroundSettingPositions.length; i++) {
            settings.setColorSettingByPosition(backgroundSettingPositions[i], backgroundButtons[i].getBackground());
            settings.setColorSettingByPosition(foregroundSettingPositions[i], foregroundButtons[i].getBackground());
            settings.setFontByPosition(fontSettingPositions[i], samples[i].getFont());//fontButtons[i].getFont());			
        }
        settings.setDataSegmentHighlighting(currentDataHighlightSetting);
        settings.setRegistersHighlighting(currentRegisterHighlightSetting);
        ExecutePane executePane = Main.getGUI().executeTab;
        (Main.getGUI().registersTab).refresh();
        (Main.getGUI().coprocessor0Tab).refresh();
        (Main.getGUI().coprocessor1Tab).refresh();
        // If a successful assembly has occured, the various panes will be populated with tables
        // and we want to apply the new settings.  If it has NOT occurred, there are no tables
        // in the Data and Text segment windows so we don't want to disturb them.  
        // In the latter case, the component count for the Text segment window is 0 (but is 1 
        // for Data segment window).
        if (executePane.getTextSegmentWindow().getContentPane().getComponentCount() > 0) {
            executePane.getDataSegmentWindow().updateValues();
            executePane.getTextSegmentWindow().highlightStepAtPC();
        }
    }

    // Called when Reset selected.  
    private void resetButtonColors() {
        Settings settings = Main.getSettings();
        dataHighlightButton.setText(getHighlightControlText(initialDataHighlightSetting));
        registerHighlightButton.setText(getHighlightControlText(initialRegisterHighlightSetting));
        Color backgroundSetting, foregroundSetting;
        Font fontSetting;
        for (int i = 0; i < backgroundSettingPositions.length; i++) {
            backgroundSetting = initialSettingsBackground[i];
            foregroundSetting = initialSettingsForeground[i];
            fontSetting = initialFont[i];
            backgroundButtons[i].setBackground(backgroundSetting);
            foregroundButtons[i].setBackground(foregroundSetting);
            //fontButtons[i].setFont(fontSetting);	
            samples[i].setBackground(backgroundSetting);
            samples[i].setForeground(foregroundSetting);
            samples[i].setFont(fontSetting);
            boolean usingDefaults = backgroundSetting.equals(settings.getDefaultColorSettingByPosition(backgroundSettingPositions[i]))
                    && foregroundSetting.equals(settings.getDefaultColorSettingByPosition(foregroundSettingPositions[i]))
                    && fontSetting.equals(settings.getDefaultFontByPosition(fontSettingPositions[i]));
            defaultCheckBoxes[i].setSelected(usingDefaults);
            backgroundButtons[i].setEnabled(!usingDefaults);
            foregroundButtons[i].setEnabled(!usingDefaults);
            fontButtons[i].setEnabled(!usingDefaults);
        }
    }

    /////////////////////////////////////////////////////////////////
    //
    //  Class that handles click on the background selection button
    //    		
    private class BackgroundChanger implements ActionListener {

        private int position;

        public BackgroundChanger(int pos) {
            position = pos;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton) e.getSource();
            Color newColor = JColorChooser.showDialog(null, "Set Background Color", button.getBackground());
            if (newColor != null) {
                button.setBackground(newColor);
                currentNondefaultBackground[position] = newColor;
                samples[position].setBackground(newColor);
            }
        }
    }

    /////////////////////////////////////////////////////////////////
    //
    //  Class that handles click on the foreground selection button
    //   		
    private class ForegroundChanger implements ActionListener {

        private int position;

        public ForegroundChanger(int pos) {
            position = pos;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton) e.getSource();
            Color newColor = JColorChooser.showDialog(null, "Set Text Color", button.getBackground());
            if (newColor != null) {
                button.setBackground(newColor);
                currentNondefaultForeground[position] = newColor;
                samples[position].setForeground(newColor);
            }
        }
    }

    /////////////////////////////////////////////////////////////////
    //
    //  Class that handles click on the font select button
    //   		
    private class FontChanger implements ActionListener {

        private int position;

        public FontChanger(int pos) {
            position = pos;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton) e.getSource();
            FontSettingDialog fontDialog = new FontSettingDialog(null, "Select Text Font", samples[position].getFont());
            Font newFont = fontDialog.showDialog();
            if (newFont != null)
                //button.setFont(newFont);
                samples[position].setFont(newFont);
        }
    }

    /////////////////////////////////////////////////////////////////
    //
    // Class that handles action (check, uncheck) on the Default checkbox.
    //   	
    private class DefaultChanger implements ItemListener {

        private int position;

        public DefaultChanger(int pos) {
            position = pos;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            // If selected: disable buttons, set their bg values from default setting, set sample bg & fg
            // If deselected: enable buttons, set their bg values from current setting, set sample bg & bg
            Color newBackground, newForeground;
            Font newFont;
            if (e.getStateChange() == ItemEvent.SELECTED) {
                backgroundButtons[position].setEnabled(false);
                foregroundButtons[position].setEnabled(false);
                fontButtons[position].setEnabled(false);
                newBackground = Main.getSettings().getDefaultColorSettingByPosition(backgroundSettingPositions[position]);
                newForeground = Main.getSettings().getDefaultColorSettingByPosition(foregroundSettingPositions[position]);
                newFont = Main.getSettings().getDefaultFontByPosition(fontSettingPositions[position]);
                currentNondefaultBackground[position] = backgroundButtons[position].getBackground();
                currentNondefaultForeground[position] = foregroundButtons[position].getBackground();
                currentNondefaultFont[position] = samples[position].getFont();
            }
            else {
                backgroundButtons[position].setEnabled(true);
                foregroundButtons[position].setEnabled(true);
                fontButtons[position].setEnabled(true);
                newBackground = currentNondefaultBackground[position];
                newForeground = currentNondefaultForeground[position];
                newFont = currentNondefaultFont[position];
            }
            backgroundButtons[position].setBackground(newBackground);
            foregroundButtons[position].setBackground(newForeground);
            //fontButtons[position].setFont(newFont);
            samples[position].setBackground(newBackground);
            samples[position].setForeground(newForeground);
            samples[position].setFont(newFont);
        }
    }

    ///////////////////////////////////////////////////////////////////
    //
    // Modal dialog to set a font.
    //
    private class FontSettingDialog extends AbstractFontSettingDialog {

        private boolean resultOK;

        public FontSettingDialog(Frame owner, String title, Font currentFont) {
            super(owner, title, true, currentFont);
        }

        private Font showDialog() {
            resultOK = true;
            // Because dialog is modal, this blocks until user terminates the dialog.
            this.setVisible(true);
            return resultOK ? getFont() : null;
        }

        @Override
        protected void closeDialog() {
            this.setVisible(false);
        }

        private void performOK() {
            resultOK = true;
        }

        private void performCancel() {
            resultOK = false;
        }

        // Control buttons for the dialog.  
        @Override
        protected Component buildControlPanel() {
            Box controlPanel = Box.createHorizontalBox();
            JButton okButton = new JButton("OK");
            okButton.addActionListener((ActionEvent e) -> {
                performOK();
                closeDialog();
            });
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener((ActionEvent e) -> {
                performCancel();
                closeDialog();
            });
            JButton resetButton = new JButton("Reset");
            resetButton.addActionListener((ActionEvent e) -> {
                reset();
            });
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(okButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(cancelButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(resetButton);
            controlPanel.add(Box.createHorizontalGlue());
            return controlPanel;
        }

        // required by Abstract super class but not used here.
        @Override
        protected void apply(Font font) {
        }

    }
}