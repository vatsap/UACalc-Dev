package org.uacalc.io;

import org.uacalc.nbui.*;
import org.uacalc.ui.table.AlgebraTableModel;
import org.uacalc.ui.table.BatchAlgebraTableModel;
import org.uacalc.ui.util.*;

import java.io.*;
import java.util.*;
import java.util.List;

import org.uacalc.alg.*;
import org.uacalc.alg.op.*;
import org.uacalc.alg.conlat.*;
import org.uacalc.util.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

public class BatchExportDialog extends JDialog {
    private JTable batchTable;
    private JButton exportButton;
    private JRadioButton singleFileRadio;
    private JRadioButton multiFileRadio;
    private JCheckBox tikzAlgebra;
    private JCheckBox tikzCon;
    private JCheckBox tikzSub;
    private JCheckBox reversedDraw;
    private JCheckBox labelInsideAlg;
    private JCheckBox labelInsideCon;
    private JCheckBox labelInsideSub;

    private DefaultListModel<CheckItem> signaturesModel;
    private JList<CheckItem> signaturesList;
    private TableRowSorter<TableModel> batchSorter;

    private DefaultListModel<CheckItem> opsModel;
    private JList<CheckItem> opsList;

    public BatchExportDialog(JFrame parent, GUIAlgebraList algebraList, MainController mc) {
        super(parent, "Batch Export", true);

        BatchAlgebraTableModel model = new BatchAlgebraTableModel(algebraList);
        batchTable = new JTable(model);
        batchTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setupColumnWidths(batchTable);
        batchSorter = new TableRowSorter<>(batchTable.getModel());
        batchTable.setRowSorter(batchSorter);


        JScrollPane scrollPane = new JScrollPane(batchTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Algebras"));

        JPanel modePanel = setupModeSelector();

        // Export button
        exportButton = new JButton("Export Selected to LaTeX");
        exportButton.addActionListener(e -> exportSelected(batchTable, model, mc));

        setLayout(new BorderLayout());
        add(modePanel, BorderLayout.NORTH);
        JComponent filterPanel = buildFilterPanel(algebraList, model);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, filterPanel, scrollPane);
        split.setResizeWeight(0.25);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        add(exportButton, BorderLayout.SOUTH);
        

        setSize(900, 600);
        setLocationRelativeTo(parent);
        applySignatureFilter(model);
    }

    private JComponent buildFilterPanel(GUIAlgebraList algebraList, BatchAlgebraTableModel model) {
        // --- signatures list ---
        signaturesModel = new DefaultListModel<>();
        signaturesList = new JList<>(signaturesModel);
        signaturesList.setCellRenderer(new CheckItemRenderer());
        signaturesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        signaturesList.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            int idx = signaturesList.locationToIndex(e.getPoint());
            if (idx < 0) return;

            Rectangle r = signaturesList.getCellBounds(idx, idx);
            if (r == null || !r.contains(e.getPoint())) return;

            CheckItem it = signaturesModel.get(idx);
            it.selected = !it.selected;

            signaturesList.repaint(r);

            applySignatureFilter(model);
            refreshOperationsFromSelection(model);

            e.consume(); // <- KLÍČOVÉ
        }
        });

        // naplň signatures z algebraList
        LinkedHashMap<String, org.uacalc.alg.op.SimilarityType> sigs = new LinkedHashMap<>();
        for (GUIAlgebra g : algebraList) {
            if (g == null || g.getAlgebra() == null) continue;
            org.uacalc.alg.op.SimilarityType st = g.getAlgebra().similarityType();
            if (st == null) continue;
            sigs.putIfAbsent(st.normalizedKey(), st);
        }
        for (Map.Entry<String, SimilarityType> e : sigs.entrySet()) {
            String key = e.getKey();
            SimilarityType st = e.getValue();
            String label = st.toStringWithArities(); // nebo st.toString()
            signaturesModel.addElement(new CheckItem(label, key, true)); // defaultně vše viditelné
        }

        JScrollPane sigScroll = new JScrollPane(signaturesList);
        sigScroll.setBorder(BorderFactory.createTitledBorder("Signatures"));

        // --- operations list ---
        opsModel = new DefaultListModel<>();
        opsList = new JList<>(opsModel);
        opsList.setCellRenderer(new CheckItemRenderer());
        opsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        opsList.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            int idx = opsList.locationToIndex(e.getPoint());
            if (idx < 0) return;

            Rectangle r = opsList.getCellBounds(idx, idx);
            if (r == null || !r.contains(e.getPoint())) return;

            CheckItem it = opsModel.get(idx);
            it.selected = !it.selected;

            opsList.repaint(r);

            e.consume();
        }
        });

        JScrollPane opsScroll = new JScrollPane(opsList);
        opsScroll.setBorder(BorderFactory.createTitledBorder("Operations (from selected algebras)"));

        // panel
        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, sigScroll, opsScroll);
        //left.setDividerLocation(0.40);
        left.setResizeWeight(0.40);
        left.setBorder(null);
        left.setPreferredSize(new Dimension(260, 400));

        // když se změní výběr řádků v tabulce, přepočítej ops
        batchTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refreshOperationsFromSelection(model);
        });

        // init ops podle aktuálního výběru (typicky prázdné)
        refreshOperationsFromSelection(model);

        return left;
    }



    private void exportSelected(JTable table, BatchAlgebraTableModel model, MainController mc) {
        int[] selected = batchTable.getSelectedRows();
        if (selected.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select at least one algebra.");
            return;
        }

        if (singleFileRadio.isSelected()) {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Save LaTeX File");
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File outFile = fc.getSelectedFile();
                Set<String> selectedOps = selectedOperationNames();
                mc.batchExportAlgebras(model, selected, selectedOps, outFile, true, tikzAlgebra.isSelected(), labelInsideAlg.isSelected(), reversedDraw.isSelected(), tikzCon.isSelected(), labelInsideCon.isSelected(), tikzSub.isSelected(), labelInsideSub.isSelected());
            }
        } else {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Choose Output Folder");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File folder = fc.getSelectedFile();
                Set<String> selectedOps = selectedOperationNames();
                mc.batchExportAlgebras(model, selected, selectedOps, folder, false, tikzAlgebra.isSelected(), labelInsideAlg.isSelected(), reversedDraw.isSelected(), tikzCon.isSelected(), labelInsideCon.isSelected(), tikzSub.isSelected(), labelInsideSub.isSelected());
            }
        }
    }

 private JPanel setupModeSelector() {

    // --- Master checkboxes ---
    tikzAlgebra = new JCheckBox("TikZ of algebra (if possible)", true);
    tikzCon = new JCheckBox("TikZ of Con(A)", false);
    tikzSub = new JCheckBox("TikZ of Sub(A)", false);

    // --- Sub-settings
    labelInsideAlg = new JCheckBox("Label inside vertex", false);
    reversedDraw = new JCheckBox("Upside-down diagram", false);
    labelInsideCon = new JCheckBox("Label inside vertex", true);
    labelInsideSub = new JCheckBox("Label inside vertex", true);


    int indent = 15; // ident of subsettings
    Border leftPad = BorderFactory.createEmptyBorder(0, indent, 0, 0);
    reversedDraw.setBorder(leftPad);
    labelInsideAlg.setBorder(leftPad);
    labelInsideCon.setBorder(leftPad);
    labelInsideSub.setBorder(leftPad);


    // disable until parent selected
    labelInsideCon.setEnabled(false);
    labelInsideSub.setEnabled(false);

    

    // --- Wiring: enable/disable sub-options depending on parent ---
    tikzAlgebra.addActionListener(e -> {
        boolean on = tikzAlgebra.isSelected();
        reversedDraw.setEnabled(on);
        labelInsideAlg.setEnabled(on);
    });

    tikzCon.addActionListener(e ->
        labelInsideCon.setEnabled(tikzCon.isSelected())
    );

    tikzSub.addActionListener(e ->
        labelInsideSub.setEnabled(tikzSub.isSelected())
    );

    // ----- layout for the three columns -----
    JPanel col1 = new JPanel();
    col1.setLayout(new BoxLayout(col1, BoxLayout.Y_AXIS));
    col1.add(tikzAlgebra);
    col1.add(reversedDraw);
    col1.add(labelInsideAlg);

    JPanel col2 = new JPanel();
    col2.setLayout(new BoxLayout(col2, BoxLayout.Y_AXIS));
    col2.add(tikzCon);
    col2.add(labelInsideCon);

    JPanel col3 = new JPanel();
    col3.setLayout(new BoxLayout(col3, BoxLayout.Y_AXIS));
    col3.add(tikzSub);
    col3.add(labelInsideSub);

    JPanel TikzSettings = new JPanel(new GridLayout(1, 3, 20, 0)); // 3 columns, with some horizontal gap
    TikzSettings.add(col1);
    TikzSettings.add(col2);
    TikzSettings.add(col3);

    // ----- radio buttons for file mode -----
    singleFileRadio = new JRadioButton("Export to a single .tex file", true);
    multiFileRadio  = new JRadioButton("Export to multiple files (one per algebra)");
    ButtonGroup group = new ButtonGroup();
    group.add(singleFileRadio);
    group.add(multiFileRadio);

    JPanel globalSettings = new JPanel(new GridLayout(2, 1));
    globalSettings.add(singleFileRadio);
    globalSettings.add(multiFileRadio);

    //titles
    JLabel TikzSetTitle = new JLabel("TikZ export settings:");
    JLabel globalSetTitle = new JLabel("Global settings:");

    JPanel TikzSetWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
    TikzSetWrapper.add(TikzSetTitle);

    JPanel globalSetWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
    globalSetWrapper.add(globalSetTitle);

    // ----- top panel -----
    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
    topPanel.add(TikzSetWrapper);
    topPanel.add(TikzSettings);
    topPanel.add(globalSetWrapper);
    topPanel.add(globalSettings);
    topPanel.add(Box.createVerticalStrut(8));

    return topPanel;
}



    private String exportLatexContent(GUIAlgebra gAlg) {
        // TODO: Replace with real export logic
        return "% LaTeX export for " + gAlg.getAlgebra().getName();
    }

    private void setupColumnWidths(JTable table) {
        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(60);
        colModel.getColumn(1).setPreferredWidth(120);
        colModel.getColumn(2).setPreferredWidth(120);
        colModel.getColumn(3).setPreferredWidth(480);
        colModel.getColumn(4).setPreferredWidth(120);
    }

    private static class CheckItem {
        final String label;   // text zobrazený v listu
        final String key;     // interní identifikátor (signatureKey / opName)
        boolean selected;

        CheckItem(String label, String key, boolean selected) {
            this.label = label;
            this.key = key;
            this.selected = selected;
        }

        @Override
        public String toString() {
            return label;
        }  
    }

    private static class CheckItemRenderer extends JCheckBox
    implements ListCellRenderer<CheckItem> {

    @Override
    public Component getListCellRendererComponent(
        JList<? extends CheckItem> list,
        CheckItem value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {

        setText(value.label);
        setSelected(value.selected);
        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setFocusPainted(false);

        return this;
    }
    }
    private void applySignatureFilter(BatchAlgebraTableModel model) {
        final Set<String> allowed = new HashSet<>();
        for (int i = 0; i < signaturesModel.size(); i++) {
            CheckItem it = signaturesModel.get(i);
            if (it.selected) allowed.add(it.key);
        }

        batchSorter.setRowFilter(new RowFilter<TableModel, Integer>() {
            @Override public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
            if (allowed.isEmpty()) return false;
            int modelRow = entry.getIdentifier();
            GUIAlgebra g = model.getAlgebraAt(modelRow);
            if (g == null || g.getAlgebra() == null) return false;
            SimilarityType st = g.getAlgebra().similarityType();
            if (st == null) return false;
            return allowed.contains(st.normalizedKey());
            }
        });
    }

    private void refreshOperationsFromSelection(BatchAlgebraTableModel model) {
        // seber vybrané řádky (ve view indexech) a převeď na model indexy
        int[] viewRows = batchTable.getSelectedRows();

        LinkedHashMap<String, org.uacalc.alg.op.OperationSymbol> uniq = new LinkedHashMap<>();
        for (int vr : viewRows) {
            int mr = batchTable.convertRowIndexToModel(vr);
            GUIAlgebra g = model.getAlgebraAt(mr);
            if (g == null || g.getAlgebra() == null) continue;

            for (Operation op : g.getAlgebra().operations()) {
            OperationSymbol sym = op.symbol();
            uniq.putIfAbsent(sym.name(), sym); // dedupe by NAME
            }
        }

        // zachovej předchozí zaškrtnutí pro známé položky
        Map<String, Boolean> prevState = new HashMap<>();
        for (int i = 0; i < opsModel.size(); i++) {
        CheckItem it = opsModel.get(i);
        prevState.put(it.key, it.selected);
        }

        opsModel.clear();

        for (Map.Entry<String, OperationSymbol> e : uniq.entrySet()) {
        String name = e.getKey();
        OperationSymbol sym = e.getValue();
        String label = sym.toString(true);

        boolean selected = true; // default pro nové operace
        Boolean old = prevState.get(name);
        if (old != null) selected = old.booleanValue();

        opsModel.addElement(new CheckItem(label, name, selected));
        }

        opsList.repaint();
    }

    private Set<String> selectedOperationNames() {
        Set<String> s = new HashSet<>();
        for (int i = 0; i < opsModel.size(); i++) {
            CheckItem it = opsModel.get(i);
            if (it.selected) s.add(it.key); // key = op name
        }
        return s;
    }


}

