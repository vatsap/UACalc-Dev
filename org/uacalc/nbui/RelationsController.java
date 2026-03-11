package org.uacalc.nbui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.uacalc.alg.SmallAlgebra;
import org.uacalc.alg.rel.AbstractRelation;
import org.uacalc.alg.rel.RelationImp;
import org.uacalc.alg.rel.RelationSymbol;
import org.uacalc.fol.FOFormula;
import org.uacalc.fol.grammar.FOParser;
import org.uacalc.fol.grammar.ParseException;
import org.uacalc.ui.util.GUIAlgebra;

/**
 * Controller for the Relations tab.
 *
 * Abstract relations are global and algebra-independent.
 * Implemented relations are concrete RelationImp objects tied to a specific SmallAlgebra.
 */
public class RelationsController {

  private final UACalc uacalc;

  /** Global registry of abstract relations. */
  private final List<AbstractRelation> relations = new ArrayList<AbstractRelation>();

  // --- UI components ---
  private JTable relationsTable;
  private DefaultTableModel relationsModel;

  private JTextField nameField;
  private JSpinner aritySpinner;
  private JTextArea definitionTextArea;

  private JTable implementationsTable;
  private DefaultTableModel implementationsModel;

  private JButton implementBtn;
  private JButton saveBtn;
  private JButton saveAsCopyBtn;
  private JButton drawImpButton;
  private JButton removeImpButton;
  private JLabel definitionPrefixLabel;
  private JTextArea relationImpString;

  /** Currently selected abstract relation in the left table. */
  private AbstractRelation currentRelation;

  public RelationsController(UACalc uacalc, PropertyChangeSupport cs) {
    this.uacalc = uacalc;

    buildUI();
    seedRelations();
    refreshRelationsTable();
    addPropertyChangeListener(cs);
  }

  private MainController getMainController() {
    return uacalc.getMainController();
  }

  private void addPropertyChangeListener(PropertyChangeSupport cs) {
    cs.addPropertyChangeListener(
        MainController.ALGEBRA_CHANGED,
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent evt) {
            updateButtonsEnabled();
            refreshImpForCurrentAlgebra();
          }
        }
    );
  }

  /** Switch to the Relations tab (no hardcoded indices). */
  public void showRelationsTab() {
    int idx = uacalc.getTabbedPane().indexOfTab("Relations");
    if (idx >= 0 && uacalc.getTabbedPane().getSelectedIndex() != idx) {
      uacalc.getTabbedPane().setSelectedIndex(idx);
      uacalc.repaint();
    }
    updateButtonsEnabled();
  }

  // ---------------- UI BUILD ----------------

  private void buildUI() {
    JPanel host = uacalc.getRelationsMainPanel();
    host.setLayout(new BorderLayout());

    // Left: abstract relations registry
    relationsModel = new DefaultTableModel(new Object[] {"Name", "Arity", "Formula"}, 0) {
      public boolean isCellEditable(int r, int c) { return false; }
    };
    relationsTable = new JTable(relationsModel);
    relationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    relationsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) onRelationSelectionChanged();
      }
    });

    JScrollPane leftScroll = new JScrollPane(relationsTable);
    leftScroll.setBorder(BorderFactory.createTitledBorder("Relations (abstract)"));

    JPanel leftButtons = new JPanel();
    JButton newBtn = new JButton("New");
    JButton dupBtn = new JButton("Duplicate");
    JButton delBtn = new JButton("Delete");
    implementBtn = new JButton("Implement to current algebra");

    newBtn.addActionListener(e -> createNewRelation());
    dupBtn.addActionListener(e -> duplicateCurrentRelation());
    delBtn.addActionListener(e -> deleteCurrentRelation());
    implementBtn.addActionListener(e -> implementToCurrentAlgebra());

    leftButtons.add(newBtn);
    leftButtons.add(dupBtn);
    leftButtons.add(delBtn);
    leftButtons.add(implementBtn);

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add(leftScroll, BorderLayout.CENTER);
    leftPanel.add(leftButtons, BorderLayout.SOUTH);

    // Right top: relationEditor
    JPanel relationEditor = new JPanel();
    relationEditor.setLayout(new BoxLayout(relationEditor, BoxLayout.Y_AXIS));
    relationEditor.setBorder(BorderFactory.createTitledBorder("Current (abstract) relation"));

    JPanel nameAndArity = new JPanel();
    nameField = new JTextField(20);
    aritySpinner = new JSpinner(new SpinnerNumberModel(2, 1, 3, 1));

    nameAndArity.add(new JLabel("Name:"));
    nameAndArity.add(nameField);
    nameAndArity.add(Box.createHorizontalStrut(10));
    nameAndArity.add(new JLabel("Arity:"));
    nameAndArity.add(aritySpinner);

    nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
      public void insertUpdate(javax.swing.event.DocumentEvent e) { updateDefinitionPrefixLabel(); }
      public void removeUpdate(javax.swing.event.DocumentEvent e) { updateDefinitionPrefixLabel(); }
      public void changedUpdate(javax.swing.event.DocumentEvent e) { updateDefinitionPrefixLabel(); }
    });

    aritySpinner.addChangeListener(e -> updateDefinitionPrefixLabel());

    definitionPrefixLabel = new JLabel();
    updateDefinitionPrefixLabel();

    definitionTextArea = new JTextArea(2, 50);
    definitionTextArea.setLineWrap(true);
    definitionTextArea.setWrapStyleWord(true);
    JScrollPane defScroll = new JScrollPane(definitionTextArea);
    defScroll.setBorder(BorderFactory.createTitledBorder("Definition (FO formula)"));

    JPanel relationEditorButtons = new JPanel();
    saveBtn = new JButton("Save");
    saveAsCopyBtn = new JButton("Save as copy");

    saveBtn.addActionListener(e -> saveRelation(false));
    saveAsCopyBtn.addActionListener(e -> saveRelation(true));

    relationEditorButtons.add(saveBtn);
    relationEditorButtons.add(saveAsCopyBtn);

    relationEditor.add(nameAndArity);
    relationEditor.add(Box.createVerticalStrut(4));
    relationEditor.add(definitionPrefixLabel);
    relationEditor.add(Box.createVerticalStrut(4));
    relationEditor.add(defScroll);
    relationEditor.add(relationEditorButtons);


    // Right bottom: implementations in current algebra
    implementationsModel = new DefaultTableModel(new Object[] {"Name", "Arity", "Size"}, 0) {
      public boolean isCellEditable(int r, int c) { return false; }
    };
    implementationsTable = new JTable(implementationsModel);
    implementationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    implementationsTable.getSelectionModel().addListSelectionListener(
        e -> {
          if (!e.getValueIsAdjusting()) {
            updateButtonsEnabled();
            updateRelationImpString();
          }
        }
    );

    JScrollPane impScroll = new JScrollPane(implementationsTable);
    impScroll.setBorder(BorderFactory.createTitledBorder("Implemented relations in current algebra"));

    relationImpString = new JTextArea(6, 40);
    relationImpString.setEditable(false);
    relationImpString.setLineWrap(true);
    relationImpString.setWrapStyleWord(true);

    JScrollPane relationImpScroll = new JScrollPane(relationImpString);
    relationImpScroll.setBorder(BorderFactory.createTitledBorder("Current relation implementation"));

    JPanel impButtons = new JPanel();
    drawImpButton = new JButton("Draw relation");
    removeImpButton = new JButton("Remove");
    

    drawImpButton.addActionListener(e -> drawSelectedInstance());
    removeImpButton.addActionListener(e -> removeSelectedImp());

    impButtons.add(drawImpButton);
    impButtons.add(removeImpButton);

    JPanel impBottom = new JPanel();
    impBottom.setLayout(new BoxLayout(impBottom, BoxLayout.Y_AXIS));
    impBottom.add(Box.createVerticalStrut(4));
    impBottom.add(impButtons);
    impBottom.add(Box.createVerticalStrut(4));
    impBottom.add(relationImpScroll);


    JPanel implPanel = new JPanel(new BorderLayout());
    implPanel.add(impScroll, BorderLayout.CENTER);
    implPanel.add(impBottom, BorderLayout.SOUTH);

    JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, relationEditor, implPanel);
    rightSplit.setBorder(null);
    rightSplit.setResizeWeight(0.3);

    JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, leftPanel, rightSplit);
    mainSplit.setBorder(null);
    mainSplit.setResizeWeight(0.4);

    host.add(mainSplit, BorderLayout.CENTER);

    leftPanel.setPreferredSize(new Dimension(360, 400));
  }

  private RelationImp getSelectedImplementation() {
    if (implementationsTable == null) return null;

    int r = implementationsTable.getSelectedRow();
    if (r < 0) return null;

    if (getMainController() == null) return null;

    GUIAlgebra gAlg = getMainController().getCurrentAlgebra();
    if (gAlg == null) return null;

    SmallAlgebra alg = gAlg.getAlgebra();
    if (alg == null) return null;

    String name = (String) implementationsModel.getValueAt(r, 0);
    int arity = ((Integer) implementationsModel.getValueAt(r, 1)).intValue();

    for (AbstractRelation absRel : relations) {
      RelationImp imp = absRel.getImplementation(alg);
      if (imp != null
          && imp.symbol().name().equals(name)
          && imp.arity() == arity) {
        return imp;
      }
    }
    return null;
  }

  private void updateRelationImpString() {
    if (relationImpString == null) return;

    RelationImp imp = getSelectedImplementation();
    if (imp == null) {
      relationImpString.setText("");
      relationImpString.setCaretPosition(0);
      return;
    }

    StringBuilder sb = new StringBuilder();
    if (imp.tuples().isEmpty()) {
      sb.append("\n∅");
    }
    else {
      boolean first= true;
      for (RelationImp.Tuple t : imp.tuples()) {
        if (!first) sb.append(", ");
        sb.append(t.toString());
        first= false;
      }
    }

    relationImpString.setText(sb.toString());
    relationImpString.setCaretPosition(0);
  }

  private void updateDefinitionPrefixLabel() {
    if (definitionPrefixLabel == null) return;

    String nm = "";
    if (nameField != null) {
      nm = nameField.getText().trim();
    }
    if (nm.length() == 0) {
      nm = "relation";
    }

    int ar = 2;
    if (aritySpinner != null) {
      ar = ((Integer)aritySpinner.getValue()).intValue();
    }

    String tupleText;
    switch (ar) {
      case 1:
        tupleText = "x";
        break;
      case 2:
        tupleText = "(x, y)";
        break;
      case 3:
        tupleText = "(x, y, z)";
        break;
      default:
        tupleText = "(...)";
        break;
    }

    definitionPrefixLabel.setText(tupleText + " in " + nm + " if and only if");
  }

  // ---------------- DATA / ACTIONS ----------------

  private void seedRelations() {
    // keep empty, or add a real parsed example later
  }

  private void refreshRelationsTable() {
    relationsModel.setRowCount(0);
    for (AbstractRelation r : relations) {
      relationsModel.addRow(new Object[] {
          r.name(),
          Integer.valueOf(r.arity()),
          r.formula() == null ? "" : r.definitionText()
      });
    }

    if (!relations.isEmpty() && relationsTable.getSelectedRow() < 0) {
      relationsTable.setRowSelectionInterval(0, 0);
    }
  }

  private void onRelationSelectionChanged() {
    int r = relationsTable.getSelectedRow();
    if (r < 0 || r >= relations.size()) {
      currentRelation = null;
      clearrelationEditor();
      updateButtonsEnabled();
      return;
    }

    currentRelation = relations.get(r);
    loadRelationIntorelationEditor(currentRelation);
    updateButtonsEnabled();
  }

  private void loadRelationIntorelationEditor(AbstractRelation relation) {
    nameField.setText(relation.name());
    aritySpinner.setValue(Integer.valueOf(relation.arity()));
    definitionTextArea.setText(relation.formula() == null ? "" : relation.definitionText());
    updateDefinitionPrefixLabel();
  }

  private void clearrelationEditor() {
    nameField.setText("");
    aritySpinner.setValue(Integer.valueOf(2));
    definitionTextArea.setText("");
    updateDefinitionPrefixLabel();
  }

  private void createNewRelation() {
    clearrelationEditor();
    currentRelation = null;
    relationsTable.clearSelection();
    updateButtonsEnabled();
  }

  private void duplicateCurrentRelation() {
    if (currentRelation == null) return;

    AbstractRelation copy = new AbstractRelation(
        new RelationSymbol(currentRelation.name() + "_copy", currentRelation.arity()),
        currentRelation.formula(), currentRelation.definitionText()
    );

    relations.add(copy);
    refreshRelationsTable();
    relationsTable.setRowSelectionInterval(relations.size() - 1, relations.size() - 1);
  }

  private void deleteCurrentRelation() {
    if (currentRelation == null) return;

    int idx = relationsTable.getSelectedRow();
    if (idx < 0) return;

    relations.remove(idx);
    currentRelation = null;

    refreshRelationsTable();
    refreshImpForCurrentAlgebra();
    updateButtonsEnabled();
  }

  private void saveRelation(boolean saveAsCopy) {
    String nm = nameField.getText().trim();
    int ar = ((Integer)aritySpinner.getValue()).intValue();
    String def = definitionTextArea.getText().trim();

    if (nm.length() == 0) {
      getMainController().beep();
      getMainController().setUserWarning("Relation name cannot be empty.", false);
      return;
    }

    if (def.length() == 0) {
      getMainController().beep();
      getMainController().setUserWarning("Relation definition cannot be empty.", false);
      return;
    }

    FOFormula formula;
    try {
      formula = FOParser.parseFormula(def);
    }
    catch (org.uacalc.fol.grammar.TokenMgrError e) {
      getMainController().beep();
      getMainController().setUserWarning("Formula lexical error: " + e.getMessage(), false);
      return;
    }
    catch (ParseException e) {
      getMainController().beep();
      getMainController().setUserWarning("Formula parse error: " + e.getMessage(), false);
      return;
    }

    RelationSymbol symbol = new RelationSymbol(nm, ar);

    if (saveAsCopy || currentRelation == null) {
      AbstractRelation relation = new AbstractRelation(symbol, formula, def);
      relations.add(relation);
      refreshRelationsTable();
      relationsTable.setRowSelectionInterval(relations.size() - 1, relations.size() - 1);
      getMainController().setUserMessage("Relation saved.", false);
      return;
    }

    int selectedRow = relationsTable.getSelectedRow();

    currentRelation.redefine(symbol, formula, def);
    currentRelation.clearImplementations();

    refreshRelationsTable();

    if (selectedRow >= 0 && selectedRow < relationsTable.getRowCount()) {
      relationsTable.setRowSelectionInterval(selectedRow, selectedRow);
    }

    refreshImpForCurrentAlgebra();
    getMainController().setUserMessage("Relation updated.", false);
  }

  private void implementToCurrentAlgebra() {
    if (currentRelation == null) return;
    if (getMainController() == null) return;

    GUIAlgebra gAlg = getMainController().getCurrentAlgebra();
    if (gAlg == null) {
      getMainController().beep();
      getMainController().setUserWarning("No current algebra selected.", false);
      return;
    }

    SmallAlgebra alg = gAlg.getAlgebra();
    if (alg == null) return;

    try {
      RelationImp imp = new RelationImp(currentRelation, alg);
      currentRelation.addImplementation(imp);
    }
    catch (RuntimeException ex) {
      getMainController().beep();
      getMainController().setUserWarning(
          "Could not implement relation on current algebra: " + ex.getMessage(), false);
      return;
    }

    refreshImpForCurrentAlgebra();

    int last = implementationsTable.getRowCount() - 1;
    if (last >= 0) {
      implementationsTable.setRowSelectionInterval(last, last);
    }

    updateButtonsEnabled();
    getMainController().setUserMessage("Relation implemented on current algebra.", false);
  }

  public void refreshImpForCurrentAlgebra() {
    implementationsModel.setRowCount(0);

    if (getMainController() == null) return;

    GUIAlgebra gAlg = getMainController().getCurrentAlgebra();
    if (gAlg == null) return;

    SmallAlgebra alg = gAlg.getAlgebra();
    if (alg == null) return;

    for (AbstractRelation absRel : relations) {
      RelationImp relImp = absRel.getImplementation(alg);
      if (relImp != null) {
        implementationsModel.addRow(new Object[] {
            relImp.symbol().name(),
            Integer.valueOf(relImp.arity()),
            Integer.valueOf(relImp.tuples().size())
        });
      }
    }
    // automatically select first row if table not empty
    if (implementationsTable.getRowCount() > 0) {
      implementationsTable.setRowSelectionInterval(0, 0);
    }
    updateButtonsEnabled();
    updateRelationImpString();
  }

  private void drawSelectedInstance() {
    int r = implementationsTable.getSelectedRow();
    if (r < 0) return;

    getMainController().beep();
    getMainController().setUserWarning(
        "Draw relation not implemented yet.", false);
  }

  private void removeSelectedImp() {
    if (getMainController() == null) return;

    GUIAlgebra gAlg = getMainController().getCurrentAlgebra();
    if (gAlg == null) return;

    SmallAlgebra alg = gAlg.getAlgebra();
    if (alg == null) return;

    int selectedRow = implementationsTable.getSelectedRow();
    if (selectedRow < 0) return;

    RelationImp imp = getSelectedImplementation();
    if (imp == null) return;

    imp.abstractRelation().removeImplementation(alg);

    refreshImpForCurrentAlgebra();

    int rowCount = implementationsTable.getRowCount();
    if (rowCount > 0) {
      int newRow = Math.min(selectedRow, rowCount - 1);
      implementationsTable.setRowSelectionInterval(newRow, newRow);
    }
    else {
      relationImpString.setText("");
      relationImpString.setCaretPosition(0);
    }

    updateRelationImpString();
    updateButtonsEnabled();
  }

  private void updateButtonsEnabled() {
    boolean hasAlg = false;
    if (getMainController() != null) {
      GUIAlgebra gAlg = getMainController().getCurrentAlgebra();
      hasAlg = (gAlg != null && gAlg.getAlgebra() != null);
    }

    if (implementBtn != null) implementBtn.setEnabled(currentRelation != null && hasAlg);

    boolean hasInstanceSelection =
        implementationsTable != null && implementationsTable.getSelectedRow() >= 0;

    if (drawImpButton != null) drawImpButton.setEnabled(hasAlg && hasInstanceSelection);
    if (removeImpButton != null) removeImpButton.setEnabled(hasAlg && hasInstanceSelection);
  }
};