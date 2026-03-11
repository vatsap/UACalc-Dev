package org.uacalc.ui.table;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.io.*;
import org.uacalc.alg.SmallAlgebra;
import org.uacalc.ui.util.*;;


public class BatchAlgebraTableModel extends AbstractTableModel {

  private final GUIAlgebraList algebraList;

  private static final String[] columnNames = {
    "Internal", "Name", "Type", "Description", "File"
  };

  public BatchAlgebraTableModel(GUIAlgebraList list) {
    this.algebraList = list;
  }

  @Override
  public int getRowCount() {
    return algebraList.size();
  }

  @Override
  public int getColumnCount() {
    return columnNames.length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    GUIAlgebra gAlg = algebraList.get(rowIndex);
    switch (columnIndex) {
      case 0: return gAlg.toString();
      case 1: return gAlg.getAlgebra().getName();
      case 2: return gAlg.getAlgebra().algebraType();
      case 3: return gAlg.getAlgebra().getDescription();
      case 4:
        final String dirty = gAlg.needsSave() ? "** " : "";
        return gAlg.getFile() != null ? dirty + gAlg.getFile().getName() : dirty;
      default: return null;
    }
  }

  @Override
  public String getColumnName(int col) {
    return columnNames[col];
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return false;
  }

  public GUIAlgebra getAlgebraAt(int row) {
    return algebraList.get(row);
  }

  public GUIAlgebraList getAlgebraList() {
    return algebraList;
  }
}

