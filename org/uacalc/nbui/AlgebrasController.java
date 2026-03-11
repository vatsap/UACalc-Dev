package org.uacalc.nbui;

import java.awt.Rectangle;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.uacalc.ui.util.GUIAlgebra;
import org.uacalc.alg.SmallAlgebra;
import org.uacalc.alg.op.OperationSymbol;
import org.uacalc.alg.op.SimilarityType;

/**
 * Builds UI for the "Algebras" tab (no real functionality yet).
 *
 * LEFT (vertical split):
 *   - signatures (checkbox list)
 *   - Algebras (list)
 *
 * RIGHT (vertical split):
 *   - Known for current algebra (list)
 *   - Tasks (titled panel, empty for now)
 */
public class AlgebrasController {

  private final UACalc uacalc;

  // Models (so you can fill them later)
  private DefaultListModel<CheckItem> signaturesModel;
  private DefaultListModel<String> knownModel;

  private final LinkedHashMap<String, SimilarityType> sigByKey = new LinkedHashMap<>();
  private TableRowSorter<TableModel> algSorter;

  // UI components (optional access later)
  private JList<CheckItem> signaturesList;
  private JList<String> knownList;

  private JPanel tasksPanel;
  private JButton renameButton;

  public AlgebrasController(UACalc uacalc) {
    this.uacalc = uacalc;
    buildUI();
  }

  private MainController getMainController() { return uacalc.getMainController(); }

  private void buildUI() {
    JPanel host = uacalc.getAlgebrasPanel();
    host.removeAll();
    host.setLayout(new BorderLayout());

    // ---------- LEFT: signatures (checkbox list) ----------
    signaturesModel = new DefaultListModel<CheckItem>();
    signaturesList = new JList<CheckItem>(signaturesModel);
    signaturesList.setCellRenderer(new CheckItemRenderer());
    signaturesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    // Minimal toggle to make checkbox list feel real (UI-only)
    signaturesList.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            int idx = signaturesList.locationToIndex(e.getPoint());
            if (idx < 0) return;

            Rectangle r = signaturesList.getCellBounds(idx, idx);
            if (r == null || !r.contains(e.getPoint())) return; // klik mimo buňku

            CheckItem item = signaturesModel.getElementAt(idx);
            item.selected = !item.selected;

            signaturesList.repaint(r);
            applySignatureFilter();

            e.consume(); // zabrání "dvojkliku" kvůli selection/focus
        }
    });

    JButton renameButton = new JButton("Rename");
    renameButton.addActionListener(e -> renameSelectedSignature());

    JScrollPane signaturesScroll = new JScrollPane(signaturesList);
    signaturesScroll.setBorder(BorderFactory.createTitledBorder("Signatures"));

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add(signaturesScroll, BorderLayout.CENTER);
    leftPanel.add(renameButton, BorderLayout. SOUTH);
    leftPanel.setPreferredSize(new Dimension(320, 400));

    // ---------- RIGHT: Known for current algebra list ----------
    knownModel = new DefaultListModel<String>();
    knownList = new JList<String>(knownModel);
    knownList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JScrollPane knownScroll = new JScrollPane(knownList);
    knownScroll.setBorder(BorderFactory.createTitledBorder("Known for current algebra"));

    // ---------- RIGHT: Tasks panel (empty placeholder) ----------
    tasksPanel = new JPanel(new CardLayout());
    tasksPanel.setBorder(BorderFactory.createTitledBorder("Tasks"));
    // (empty for now)

    JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, knownScroll, tasksPanel);
    rightSplit.setBorder(null);
    rightSplit.setResizeWeight(0.55);

    // ---------- MAIN split ----------
    JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, leftPanel, rightSplit);
    mainSplit.setBorder(null);
    mainSplit.setResizeWeight(0.35);

    host.add(mainSplit, BorderLayout.CENTER);

    host.revalidate();
    host.repaint();
  }

  private void renameSelectedSignature() {
    CheckItem item = signaturesList.getSelectedValue();
    if (item == null) {
      getMainController().beep();
      getMainController().setUserWarning("No signature selected.", false);
      return;
    }

    SimilarityType oldSig = sigByKey.get(item.key);
    if (oldSig == null) return;

    java.util.List<OperationSymbol> oldOps =
        new ArrayList<OperationSymbol>(oldSig.getSortedOperationSymbols());
    java.util.List<OperationSymbol> newOps = new ArrayList<OperationSymbol>();

    for (OperationSymbol oldOp : oldOps) {
      String prompt = "Operation \"" + oldOp.name() + "\" of arity "
          + oldOp.arity() + " will be renamed to:";
      String newName = (String) javax.swing.JOptionPane.showInputDialog(
          uacalc.getFrame(),
          prompt,
          "Rename signature",
          javax.swing.JOptionPane.QUESTION_MESSAGE,
          null,
          null,
          oldOp.name()
      );

      if (newName == null) return; // cancel whole wizard
      newName = newName.trim();

      if (newName.length() == 0) {
        getMainController().beep();
        getMainController().setUserWarning("Operation name cannot be empty.", false);
        return;
      }

      OperationSymbol newOp = new OperationSymbol(newName, oldOp.arity(), oldOp.isAssociative());
      newOps.add(newOp);
    }

    java.util.Set<String> names = new java.util.HashSet<String>();
    for (OperationSymbol sym : newOps) {
      if (!names.add(sym.name())) {
        getMainController().beep();
        getMainController().setUserWarning(
            "Two operations cannot have the same name: " + sym.name(), false);
        return;
      }
    }

    SimilarityType newSig = new SimilarityType(newOps, true);
    renameSignaturesOnAlgebras(item.key, newSig);
    refreshSignaturesFromOpenAlgebras();
    applySignatureFilter();

    GUIAlgebra current = getMainController().getCurrentAlgebra(); //Reselecting current algebra to get update
    if (current != null && current.getAlgebra() != null) {
      SimilarityType currSig = current.getAlgebra().similarityType();
      if (currSig != null && newSig.normalizedKey().equals(currSig.normalizedKey())) {
        getMainController().setCurrentAlgebra(current);
      }
    }
    getMainController().setUserMessage("Signature renamed.", false);
  }

  private void renameSignaturesOnAlgebras(String oldKey, SimilarityType newSig) {
    MainController mc = getMainController();
    if (mc == null) return;

    for (GUIAlgebra gAlg : mc.getAlgebraList()) {
      if (gAlg == null || gAlg.getAlgebra() == null) continue;

      SimilarityType sig = gAlg.getAlgebra().similarityType();
      if (sig == null) continue;
      if (!oldKey.equals(sig.normalizedKey())) continue;

      renameAlgebraSignature(gAlg, newSig);
    }
  }

  private void renameAlgebraSignature(GUIAlgebra gAlg, SimilarityType newSig) {
    if (gAlg == null || gAlg.getAlgebra() == null || newSig == null) return;

    SmallAlgebra alg = gAlg.getAlgebra();
    SimilarityType oldSig = alg.similarityType();
    if (oldSig == null) return;

    java.util.List<OperationSymbol> oldOps =
        new ArrayList<OperationSymbol>(oldSig.getSortedOperationSymbols());
    java.util.List<OperationSymbol> newOps =
        new ArrayList<OperationSymbol>(newSig.getSortedOperationSymbols());

    if (oldOps.size() != newOps.size()) {
      getMainController().beep();
      getMainController().setUserWarning(
          "Cannot rename signature: incompatible number of operations.", false);
      return;
    }

    java.util.Map<OperationSymbol, OperationSymbol> renameMap =
        new LinkedHashMap<OperationSymbol, OperationSymbol>();

    for (int i = 0; i < oldOps.size(); i++) {
      OperationSymbol oldSym = oldOps.get(i);
      OperationSymbol newSym = newOps.get(i);

      if (oldSym.arity() != newSym.arity()) {
        getMainController().beep();
        getMainController().setUserWarning(
            "Cannot rename signature: arity mismatch for operation " + oldSym.name() + ".", false);
        return;
      }

      renameMap.put(oldSym, newSym);
    }

    for (org.uacalc.alg.op.Operation op : alg.operations()) {
      OperationSymbol oldSym = op.symbol();
      OperationSymbol newSym = renameMap.get(oldSym);
      if (newSym == null) continue;

      if (op instanceof org.uacalc.alg.op.AbstractOperation) {
        ((org.uacalc.alg.op.AbstractOperation) op).setSymbol(newSym);
      }
    }

    alg.updateSimilarityType();
    alg.resetConAndSub();
    gAlg.setNeedsSave(true);

    MainController mc = getMainController();
    if (mc != null) {
      mc.getPropertyChangeSupport().firePropertyChange(
          MainController.ALGEBRA_CHANGED, null, null);
      mc.algebraStructureChanged();
    }
  }

  private Set<String> getAllSignatureKeysInModel() {
    Set<String> keys = new HashSet<>();
    for (int i = 0; i < signaturesModel.size(); i++) {
        keys.add(signaturesModel.getElementAt(i).key);
    }
    return keys;
  }

  public void refreshSignaturesFromOpenAlgebras() {
    MainController mc = getMainController();
    if (mc == null) return;

    Set<String> previouslySelected = getSelectedSignatureKeys();
    Set<String> previouslyPresent = getAllSignatureKeysInModel();
    boolean nothingWasSelected = previouslySelected.isEmpty();
    // 1) zjisti co je teď zaškrtnuto (aby refresh nezrušil user volbu)

    // 2) znovu postav unikátní seznam signature typů z otevřených algeber
    sigByKey.clear();
    for (GUIAlgebra gAlg : mc.getAlgebraList()) {
        if (gAlg == null || gAlg.getAlgebra() == null) continue;

        SimilarityType st = gAlg.getAlgebra().similarityType();
        if (st == null) continue;

        sigByKey.putIfAbsent(st.normalizedKey(), st); // tvoje nová metoda
    }

    // 3) přepiš model signatures listu
    signaturesModel.clear();

    // UX: když ještě user nic nevybíral, dej defaultně všechny signatury zaškrtnuté

    for (Map.Entry<String, SimilarityType> e : sigByKey.entrySet()) {
    String key = e.getKey();
    SimilarityType st = e.getValue();
    String label = st.toStringWithArities();

    // pravidla:
    // 1) pokud user zatím nikdy nic neřešil => vše zapnuté
    // 2) jinak zachovej staré volby
    // 3) ale: NOVÉ key, které dřív neexistovalo, zapni automaticky
    boolean selected;
    if (nothingWasSelected) {
      selected = true; // úplný start -> všechno viditelné
    } else if (previouslySelected.contains(key)) {
      selected = true; // user to měl zapnuté
    } else if (!previouslyPresent.contains(key)) {
      selected = true; // NOVÁ signatura -> auto zapnout
    } else {
      selected = false; // existovala už dřív a user ji měl vypnutou
    }

    signaturesModel.addElement(new CheckItem(label, key, selected));
    }


  signaturesList.repaint();
}

private void ensureAlgSorterInstalled() {
  JTable algTable = uacalc.getAlgListTable();
  if (algSorter == null || algTable.getRowSorter() != algSorter) {
    algSorter = new TableRowSorter<>(algTable.getModel());
    algTable.setRowSorter(algSorter);
  }
}

private Set<String> getSelectedSignatureKeys() {
  Set<String> keys = new HashSet<>();
  for (int i = 0; i < signaturesModel.size(); i++) {
    CheckItem it = signaturesModel.getElementAt(i);
    if (it.selected) keys.add(it.key);
  }
  return keys;
}

public void applySignatureFilter() {
  ensureAlgSorterInstalled();
  MainController mc = getMainController();
  if (mc == null) return;

  final Set<String> allowed = getSelectedSignatureKeys();

    RowFilter<TableModel, Integer> filter = new RowFilter<TableModel, Integer>() {
    @Override
    public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {

        if (allowed.isEmpty()) return false;

        int modelRow = entry.getIdentifier();
        if (modelRow < 0 || modelRow >= mc.getAlgebraList().size()) return false;

        GUIAlgebra gAlg = mc.getAlgebraList().get(modelRow);
        SimilarityType st = gAlg.getAlgebra().similarityType();
        if (st == null) return false;

        return allowed.contains(st.normalizedKey());
    }
    };

  algSorter.setRowFilter(filter);
}


private static final class CheckItem {
    public final String label;
    public final String key;  
    public boolean selected;

    public CheckItem(String label, String key, boolean selected) {
        this.label = label;
        this.key = key;
        this.selected = selected;
    }

    public String toString() { return label; }
    }


private static final class CheckItemRenderer extends JCheckBox implements ListCellRenderer<CheckItem> {
    public Component getListCellRendererComponent(
        JList<? extends CheckItem> list,
        CheckItem value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {

        setText(value == null ? "" : value.label);
        setSelected(value != null && value.selected);

        // keep selection highlight consistent with JList
        if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
        } else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
        }
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setOpaque(true);
        return this;
    }
}
}
