package mars.venus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
//import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
//import mars.MIPSprogram;
import mars.Main;
//import mars.assembler.Symbol;
//import mars.assembler.SymbolTable;
import mars.assembler.staticassembler.StaticAssembler.Label;
import mars.mips.newhardware.MIPSMachine;
//import mars.mips.hardware.Memory;
import mars.settings.IntegerSettings;
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
 * Represents the Labels window, which is a type of JInternalFrame. Venus user
 * can view MIPS program labels.
 *
 * @author Sanderson and Team JSpim
 *
 */
public class LabelsWindow extends JInternalFrame {
    
    private JPanel labelPanel;      // holds J
    private JCheckBox dataLabels, textLabels;
    private List<JSymbolTable> symbolTables;
    private static final int MAX_DISPLAYED_CHARS = 24;
    private static final int PREFERRED_NAME_COLUMN_WIDTH = 60;
    private static final int PREFERRED_ADDRESS_COLUMN_WIDTH = 60;
    private static final int LABEL_COLUMN = 0;
    private static final int ADDRESS_COLUMN = 1;
    private static final String[] columnToolTips = {
        /* LABEL_COLUMN */"Programmer-defined label (identifier).",
        /* ADDRESS_COLUMN */ "Text or data segment address at which label is defined."
    };
    private static String[] columnNames;
    private Comparator<Map.Entry<String, Label>> tableSortComparator;
    private static final List<Comparator<Map.Entry<String, Label>>> tableSortingComparators;
    static {
        
        //  Comparator class used to sort in ascending order a List of symbols alphabetically by name
        Comparator<Map.Entry<String, Label>> labelNameAscendingComparator = (a, b) -> a.getKey().compareTo(b.getKey());
        //  Comparator class used to sort in ascending order a List of symbols numerically by address
        Comparator<Map.Entry<String, Label>> labelAddressAscendingComparator = (a, b) -> Integer.compareUnsigned(a.getValue().address, b.getValue().address);

        tableSortingComparators = Arrays.asList(
                /*  0  */labelAddressAscendingComparator,
                /*  1  */ labelAddressAscendingComparator.reversed(),
                /*  2  */ labelAddressAscendingComparator,
                /*  3  */ labelAddressAscendingComparator.reversed(),
                /*  4  */ labelNameAscendingComparator,
                /*  5  */ labelNameAscendingComparator,
                /*  6  */ labelNameAscendingComparator.reversed(),
                /*  7  */ labelNameAscendingComparator.reversed()
        );
    }
    // The array of state transitions; primary index corresponds to state in table above,
    // secondary index corresponds to table columns (0==label name, 1==address).
    private static final int[][] sortStateTransitions = {
        /*  0  */{4, 1},
        /*  1  */ {5, 0},
        /*  2  */ {6, 3},
        /*  3  */ {7, 2},
        /*  4  */ {6, 0},
        /*  5  */ {7, 1},
        /*  6  */ {4, 2},
        /*  7  */ {5, 3}
    };
    // The array of column headings; index corresponds to state in table above.
    private static final char ASCENDING_SYMBOL = '\u25b2'; //triangle with base at bottom ("points" up, to indicate ascending sort)
    private static final char DESCENDING_SYMBOL = '\u25bc';//triangle with base at top ("points" down, to indicate descending sort)
    private static final String[][] sortColumnHeadings = {
        /*  0  */{"Label", "Address  " + ASCENDING_SYMBOL},
        /*  1  */ {"Label", "Address  " + DESCENDING_SYMBOL},
        /*  2  */ {"Label", "Address  " + ASCENDING_SYMBOL},
        /*  3  */ {"Label", "Address  " + DESCENDING_SYMBOL},
        /*  4  */ {"Label  " + ASCENDING_SYMBOL, "Address"},
        /*  5  */ {"Label  " + ASCENDING_SYMBOL, "Address"},
        /*  6  */ {"Label  " + DESCENDING_SYMBOL, "Address"},
        /*  7  */ {"Label  " + DESCENDING_SYMBOL, "Address"}
    };

    // Current sort state (0-7, see table above).  Will be set from saved Settings in constructor.
    private int sortState = 0;

    /**
     * Constructor for the Labels (symbol table) window.
     *
     */
    public LabelsWindow() {
        super("Labels", true, false, true, true);

        sortState = IntegerSettings.LABEL_SORT_STATE.get();
        columnNames = sortColumnHeadings[sortState];
        tableSortComparator = tableSortingComparators.get(sortState);

        labelPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        ///////////////////////////////////////////////////////////////
        //   Listener class to respond to "Text" or "Data" checkbox click 
        ItemListener updater = (ItemEvent ie)
                -> symbolTables.forEach(JSymbolTable::generateLabelTable);

        dataLabels = new JCheckBox("Data", true);
        textLabels = new JCheckBox("Text", true);
        dataLabels.addItemListener(updater);
        textLabels.addItemListener(updater);
        dataLabels.setToolTipText("If checked, will display labels defined in data segment");
        textLabels.setToolTipText("If checked, will display labels defined in text segment");

        JPanel features = new JPanel();
        features.add(dataLabels);
        features.add(textLabels);

        Container contentPane = this.getContentPane();
        contentPane.add(features, BorderLayout.SOUTH);
        contentPane.add(labelPanel);
    }
    
    /**
     * Initialize table of labels (symbol table)
     */
    public void refresh() {
        labelPanel.removeAll();

        // Populate list of tables, first is global table
        // LEGACY
//        (symbolTables = new ArrayList<>()).add(new JSymbolTable(null));
//        for (MIPSprogram program : ExecuteAction.getMIPSprogramsToAssemble())
//            symbolTables.add(new JSymbolTable(program));
        Set<Map.Entry<String, Label>> entries = Main.exec.getLabels().entrySet();
        JSymbolTable globals = new JSymbolTable(Main.exec.rootFileName, Main.machine, entries.stream().filter(e -> e.getValue().valid && e.getValue().global).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        JSymbolTable locals = new JSymbolTable(Main.exec.rootFileName, Main.machine, entries.stream().filter(e -> e.getValue().valid && !e.getValue().global).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        symbolTables = List.of(globals, locals);

        Box tablesBox = Box.createVerticalBox();

        ArrayList<Box> tableNames = new ArrayList<>();
        JTableHeader tableHeader = null;
        for (JSymbolTable symtab : symbolTables)
            if (symtab.hasSymbols()) {

                String name = symtab.getSymbolTableName();
                if (name.length() > MAX_DISPLAYED_CHARS)
                    name = name.substring(0, MAX_DISPLAYED_CHARS - 3) + "...";
                // To get left-justified, put file name into first slot of horizontal Box, then glue.
                JLabel nameLab = new JLabel(name, JLabel.LEFT);
                
                Box nameLabel = Box.createHorizontalBox();
                nameLabel.add(nameLab);
                nameLabel.add(Box.createHorizontalGlue());
                nameLabel.add(Box.createHorizontalStrut(1));
                tableNames.add(nameLabel);
                tablesBox.add(nameLabel);
                
                JTable table = symtab.generateLabelTable();
                tableHeader = table.getTableHeader();
                // The following is selfish on my part.  Column re-ordering doesn't work correctly when
                // displaying multiple symbol tables; the headers re-order but the columns do not.
                // Given the low perceived benefit of reordering displayed symbol table information
                // versus the perceived effort to make reordering work for multiple symbol tables,
                // I am taking the easy way out here.  PS 19 July 2007.
                tableHeader.setReorderingAllowed(false);
                // TODO Can I manage cross-table selecion?
                table.setSelectionBackground(table.getBackground());
                table.setSelectionForeground(table.getForeground());
                // Sense click on label/address and scroll Text/Data segment display to it.
                table.addMouseListener(new LabelDisplayMouseListener());
                tablesBox.add(table);
            }
        JScrollPane labelScrollPane = new JScrollPane(tablesBox,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // Set file name label's max width to scrollpane's viewport width, max height to small.
        // Does it do any good?  Addressing problem that occurs when label (filename) is wider than
        // the table beneath it -- the table column widths are stretched to attain the same width and
        // the address information requires scrolling to see.  All because of a long file name.
        for (Box nameLabel : tableNames)
            nameLabel.setMaximumSize(new Dimension(
                    labelScrollPane.getViewport().getViewSize().width,
                    (int) (1.5 * nameLabel.getFontMetrics(nameLabel.getFont()).getHeight())));
        labelScrollPane.setColumnHeaderView(tableHeader);
        
        labelPanel.add(labelScrollPane);
    }

    /**
     * Method to update display of label addresses. Since label information
     * doesn't change, this should only be done when address base is changed.
     * (e.g. between base 16 hex and base 10 dec).
     */
    public void updateLabelAddresses() {
        symbolTables.forEach(JSymbolTable::updateLabelAddresses);
    }

    /////////////////////////////////////////////////////////////////
    //  Private listener class to sense clicks on a table entry's 
    //  Label or Address.  This will trigger action by Text or Data
    //  segment to scroll to the corresponding label/address.
    //  Suggested by Ken Vollmar, implemented by Pete Sanderson
    //  July 2007.
    private class LabelDisplayMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            JTable table = (JTable) e.getSource();
            int row = table.rowAtPoint(e.getPoint());
            int column = table.columnAtPoint(e.getPoint());
            Object data = table.getValueAt(row, column);
            if (table.getColumnName(column).equals(columnNames[LABEL_COLUMN]))
                // Selected a Label name, so get its address.
                data = table.getModel().getValueAt(row, ADDRESS_COLUMN);
            int address = 0;
            try {
                address = Binary.stringToInt((String) data);
            }
            catch (NumberFormatException nfe) {
                // Cannot happen because address is generated internally.
            }
            catch (ClassCastException cce) {
                // Cannot happen because table contains only strings.
            }
            // Scroll to this address, either in Text Segment display or Data Segment display
            if (Main.machine.getMemory().inTextSegment(address) || Main.machine.getMemory().inKernelTextSegment(address))
                Main.getGUI().textSegment.selectStepAtAddress(address);
            else
                Main.getGUI().dataSegment.selectCellForAddress(address);
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Represents one symbol table for the display.
    private class JSymbolTable {

        private Object[][] labelData;
        private JTable labelTable;
//        private ArrayList<Symbol> symbols;
//        private SymbolTable symbolTable;
        private String tableName;
        private List<Map.Entry<String, Label>> symbols;
        private MIPSMachine machine;
        
        public JSymbolTable(String name, MIPSMachine machine, Map<String, Label> labels) {
            tableName = name;
            symbols = new ArrayList(labels.entrySet());
            this.machine = machine;
        }

        // Associated MIPSprogram object.  If null, this represents global symbol table.
//        public JSymbolTable(MIPSprogram program) {
//            symbolTable = (program == null)
//                    ? Main.symbolTable
//                    : program.getLocalSymbolTable();
//            tableName = (program == null)
//                    ? "[Global]"
//                    : new File(program.getFilename()).getName();
//        }

        // Returns file name of associated file for local symbol table or "[Global]"    
        public String getSymbolTableName() {
            return tableName;
        }

        public boolean hasSymbols() {
            return !symbols.isEmpty();
        }

        // builds the Table containing labels and addresses for this symbol table.
        private JTable generateLabelTable() {
            
            // FOr display format purposes
            int addressBase = Main.getGUI().dataSegment.getAddressDisplayBase();
            
            Map<String, Label> symbolss;
            
            // Basically filtering either text or data labels (or a combo of both)
            if (textLabels.isSelected() && dataLabels.isSelected());
//                symbols = symbolTable.getAllSymbols();
            else if (textLabels.isSelected() && !dataLabels.isSelected())
//                symbols = symbolTable.getTextSymbols();
                symbolss = symbols
                        .stream()
                        .filter(e -> machine.getMemory().inTextSegment(e.getValue().address) || machine.getMemory().inKernelTextSegment(e.getValue().address))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            else if (!textLabels.isSelected() && dataLabels.isSelected())
//                symbols = symbolTable.getDataSymbols();
                symbolss = symbols
                        .stream()
                        .filter(e -> machine.getMemory().inDataSegment(e.getValue().address) || machine.getMemory().inKernelDataSegment(e.getValue().address))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            else
                symbols = new ArrayList<>();
            
            
            
            
            symbols.sort(tableSortComparator); // DPS 25 Dec 2008
            labelData = new Object[symbols.size()][2];
            
            for (int i = 0; i < symbols.size(); i++) { //sets up the label table
                Map.Entry<String, Label> s = symbols.get(i);
                labelData[i][LABEL_COLUMN] = s.getKey();
                labelData[i][ADDRESS_COLUMN] = NumberDisplayBaseChooser.formatNumber(s.getValue().address, addressBase);
            }
            
            
            LabelTableModel m = new LabelTableModel(labelData, LabelsWindow.columnNames);
            if (labelTable == null)
                labelTable = new MyTippedJTable(m);
            else
                labelTable.setModel(m);
            labelTable.getColumnModel().getColumn(ADDRESS_COLUMN).setCellRenderer(new MonoRightCellRenderer());
            return labelTable;
        }

        public void updateLabelAddresses() {
            if (labelPanel.getComponentCount() == 0)
                return; // ignore if no content to change
            int addressBase = Main.getGUI().dataSegment.getAddressDisplayBase();
            int address;
            String formattedAddress;
            int numSymbols = (labelData == null) ? 0 : labelData.length;
            for (int i = 0; i < numSymbols; i++) {
                address = symbols.get(i).getValue().address;
                formattedAddress = NumberDisplayBaseChooser.formatNumber(address, addressBase);
                labelTable.getModel().setValueAt(formattedAddress, i, ADDRESS_COLUMN);
            }
        }
    }
    //////////////////////  end of LabelsForOneSymbolTable class //////////////////  

    ///////////////////////////////////////////////////////////////      
    // Class representing label table data 
    class LabelTableModel extends AbstractTableModel {

        String[] columns;
        Object[][] data;

        public LabelTableModel(Object[][] d, String[] n) {
            data = d;
            columns = n;
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  
         */
        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        @Override
        public void setValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }

        private void printDebugData() {
            int numRows = getRowCount();
            int numCols = getColumnCount();

            for (int i = 0; i < numRows; i++) {
                System.out.print("    row " + i + ":");
                for (int j = 0; j < numCols; j++)
                    System.out.print("  " + data[i][j]);
                System.out.println();
            }
            System.out.println("--------------------------");
        }
    }

    ///////////////////////////////////////////////////////////////////
    //
    // JTable subclass to provide custom tool tips for each of the
    // label table column headers. From Sun's JTable tutorial.
    // http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
    //
    private class MyTippedJTable extends JTable {

        MyTippedJTable(LabelTableModel m) {
            super(m);
        }

        //Implement table header tool tips. 
        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new SymbolTableHeader(columnModel);
        }

        // Implement cell tool tips.  All of them are the same (although they could be customized).
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
            Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
            if (c instanceof JComponent) {
                JComponent jc = (JComponent) c;
                jc.setToolTipText("Click on label or address to view it in Text/Data Segment");
            }
            return c;
        }

        /////////////////////////////////////////////////////////////////
        //
        // Customized table header that will both display tool tip when
        // mouse hovers over each column, and also sort the table when
        // mouse is clicked on each column.  The tool tip and sort are
        // customized based on the column under the mouse.
        private class SymbolTableHeader extends JTableHeader {

            public SymbolTableHeader(TableColumnModel cm) {
                super(cm);

                addMouseListener(new MouseAdapter() {
                    /////////////////////////////////////////////////////////////////////
                    // When user clicks on table column header, system will sort the
                    // table based on that column then redraw it.
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        sortState = sortStateTransitions[sortState][realIndex];
                        tableSortComparator = tableSortingComparators.get(sortState);
                        columnNames = sortColumnHeadings[sortState];
                        IntegerSettings.LABEL_SORT_STATE.set(sortState);
                        refresh();
                        LabelsWindow.this.validate();
                    }
                });
            }

            @Override
            public String getToolTipText(MouseEvent e) {
                return columnToolTips[columnModel.getColumn(
                        columnModel.getColumnIndexAtX(e.getX())).getModelIndex()];
            }

        }
    }
}
