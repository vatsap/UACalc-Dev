package org.uacalc.nbui;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.nio.file.Files;
import java.util.prefs.*;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import org.latdraw.diagram.*;
import org.uacalc.alg.*;
import org.uacalc.alg.op.Operation;
import org.uacalc.alg.op.OperationWithDefaultValue;
import org.uacalc.alg.op.Operations;
import org.uacalc.lat.*;
import org.uacalc.alg.conlat.*;
import org.uacalc.alg.sublat.*;
import org.uacalc.io.*;
import org.uacalc.ui.*;
import org.uacalc.ui.table.*;
import org.uacalc.ui.util.*;
import org.uacalc.ui.tm.ProgressReport;
import org.uacalc.io.lua.*;


public class MainController {

  
  
  
  private boolean dirty = false;
  private UACalc uacalcUI;
  private GUIAlgebra currentAlgebra;  // Small ??
  //private java.util.List<SmallAlgebra> algebraList = new ArrayList<SmallAlgebra>();
  private AlgebraTableModel algebraTableModel = new  AlgebraTableModel();
  private final GUIAlgebraList algebraList = algebraTableModel.getAlgebraList();
  //private final java.util.List<GUIAlgebra> algs = new ArrayList<GUIAlgebra>();
  private File currentFile;
  private String title = "";  // if currentFile is null this might be "New"
  private String progName = "UACalculator  " + Version.versionString + Version.buildDate + "  ";
  private String currentFolder;
  private AlgebraEditorController algEditorController;
  private ComputationsController computationsController;
  private RelationsController relationsController;
  private AlgebrasController algebrasController;
  private ConController conController;
  private SubController subController;
  private DrawingController drawingController;
  private PropertyChangeSupport propertyChangeSupport;
  
  private JFileChooser fileChooser;
  
  private JComboBox algListComboBox = new JComboBox();
  public static final String ALGEBRA_CHANGED = "Algebra Changed";
  
  //private Tabs tabs;
  private final Random random = new Random();
  
  public MainController(UACalc uacalcUI) {
    this.uacalcUI = uacalcUI;
    propertyChangeSupport = new PropertyChangeSupport(this);
    algEditorController = new AlgebraEditorController(uacalcUI);
    computationsController = new ComputationsController(uacalcUI);
    relationsController = new RelationsController(uacalcUI, propertyChangeSupport);
    algebrasController = new AlgebrasController(uacalcUI);
    conController = new ConController(uacalcUI, propertyChangeSupport);
    subController = new SubController(uacalcUI, propertyChangeSupport);
    drawingController = new DrawingController(uacalcUI, propertyChangeSupport);
    setupAlgTable();
    setTitle();
  }
  
  private void setupAlgTable() {
    final JTable algTable = uacalcUI.getAlgListTable();
    algTable.setModel(algebraTableModel);
    TableColumn col = algTable.getColumnModel().getColumn(0); // Internal
    col.setPreferredWidth(60);
    col.setMinWidth(50);

    col = algTable.getColumnModel().getColumn(1); // Name
    col.setPreferredWidth(160);
    col.setMinWidth(120);

    col = algTable.getColumnModel().getColumn(2); // Type
    col.setPreferredWidth(160);
    col.setMinWidth(120);


    col = algTable.getColumnModel().getColumn(3); // Signature
    col.setPreferredWidth(200);
    col.setMinWidth(160);

    col = algTable.getColumnModel().getColumn(4); // Cardinality
    col.setPreferredWidth(70);
    col.setMinWidth(50);

    col = algTable.getColumnModel().getColumn(5); // File
    col.setPreferredWidth(220);
    col.setMinWidth(160);
    algTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        int viewIndex = algTable.getSelectedRow();
        if (viewIndex < 0) return;
        int modelIndex = algTable.convertRowIndexToModel(viewIndex);
        GUIAlgebra gAlg = algebraList.get(modelIndex);
        //System.out.println("curr alg = " + getCurrentAlgebra());
        //System.out.println("gAlg = " + gAlg);
        //System.out.println("curr alg equals gAlg is " + gAlg.equals(getCurrentAlgebra()));
        //System.out.println(" number of ops = " +
        //    gAlg.getAlgebra().operations().size());
        if (!gAlg.equals(getCurrentAlgebra())) {
          setCurrentAlgebra(gAlg);
        }
      }
    });
  }  
  
  public File getCurrentFile() { return currentFile; }
  public void setCurrentFile(File f) { currentFile = f; }
  
  public PropertyChangeSupport getPropertyChangeSupport() {
    return propertyChangeSupport;
  }  
  
  public void quit() {
    boolean dirty = false;
    for (GUIAlgebra gAlg : algebraList) {
      if (gAlg.needsSave()) {
        dirty = true;
        break;
      }
    }
    if (dirty) {
      if (checkSave()) System.exit(0);
      else return;
    }
    else System.exit(0);
  }
  
  
  public boolean checkSave() {
    Object[] options = { "Yes", "No" };
    int n = JOptionPane.showOptionDialog(uacalcUI.getFrame(),
        "Some algebra have not been saved. Exit anyway?", 
        "Unsaved Algebras",
        JOptionPane.YES_NO_OPTION, 
        JOptionPane.QUESTION_MESSAGE, 
        null,
        options, 
        options[0]);
    if (n == JOptionPane.YES_OPTION) {
      return true;
    }
    return false;
 }




/*
  private void buildMenu() {
    // Instantiates JMenuBar, JMenu and JMenuItem
    JMenuBar menuBar = new JMenuBar();

    // the file menu
    JMenu file = (JMenu) menuBar.add(new JMenu("File"));
    file.setMnemonic(KeyEvent.VK_F);

    ClassLoader cl = this.getClass().getClassLoader();


    ImageIcon icon = new ImageIcon(cl.getResource(
                             "org/uacalc/ui/images/New16.gif"));

    JMenu newMenu = (JMenu)file.add(new JMenu("New"));
    JMenuItem newTablesMI = (JMenuItem)newMenu.add(new JMenuItem("New (Tables)", icon));
    newTablesMI.setMnemonic(KeyEvent.VK_N);
    KeyStroke cntrlN = KeyStroke.getKeyStroke(KeyEvent.VK_N,Event.CTRL_MASK);
    newTablesMI.setAccelerator(cntrlN);
    JMenuItem newScriptMI = (JMenuItem)newMenu.add(new JMenuItem("New (Script)", icon));
    
    newTablesMI.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        //newTableAlgebra();
      }
    });


    file.add(new JSeparator());

    ImageIcon openIcon = new ImageIcon(cl.getResource(
                             "org/uacalc/ui/images/Open16.gif"));
    JMenuItem openMI = (JMenuItem)file.add(new JMenuItem("Open", openIcon));
    openMI.setMnemonic(KeyEvent.VK_O);
    KeyStroke cntrlO = KeyStroke.getKeyStroke(KeyEvent.VK_O,Event.CTRL_MASK);
    openMI.setAccelerator(cntrlO);
    
    icon = new ImageIcon(cl.getResource("org/uacalc/ui/images/Save16.gif"));
    JMenuItem saveMI = (JMenuItem) file.add(new JMenuItem("Save", icon));
    saveMI.setMnemonic(KeyEvent.VK_S);
    KeyStroke cntrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK);
    saveMI.setAccelerator(cntrlS);

    icon = new ImageIcon(cl.getResource("org/uacalc/ui/images/SaveAs16.gif"));

    JMenu saveAsMenu = (JMenu)file.add(new JMenu("Save As"));
    JMenuItem saveAsUAMI
      = (JMenuItem)saveAsMenu.add(new JMenuItem("ua file (new format", icon));
    JMenuItem saveAsAlgMI
      = (JMenuItem)saveAsMenu.add(new JMenuItem("alg file (old format)", icon));

    saveAsUAMI.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            saveAs(ExtFileFilter.UA_EXT);
          }
          catch (IOException ex) {
            System.err.println("IO error in saving: " + ex.getMessage());
          }
        }
      });

    saveAsAlgMI.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            saveAs(ExtFileFilter.ALG_EXT);
          }
          catch (IOException ex) {
            System.err.println("IO error in saving: " + ex.getMessage());
          }
        }
      });

    saveMI.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          save();
        }
        catch (IOException ex) {
          System.err.println("IO error in saving: " + ex.getMessage());
        }
      }
    });

    JMenuItem exitMI = (JMenuItem)file.add(new JMenuItem("Exit"));
    exitMI.setMnemonic(KeyEvent.VK_X);
    KeyStroke cntrlQ = KeyStroke.getKeyStroke(KeyEvent.VK_Q,Event.CTRL_MASK);
    exitMI.setAccelerator(cntrlQ);

    JMenu draw = (JMenu) menuBar.add(new JMenu("Draw"));

    JMenuItem drawConMI = (JMenuItem)draw.add(new JMenuItem("Con"));

    drawConMI.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getCurrentAlgebra() != null) drawCon(getCurrentAlgebra());
        }
      });

    JMenuItem drawSubMI = (JMenuItem)draw.add(new JMenuItem("Sub"));

    drawSubMI.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getCurrentAlgebra() != null) drawSub(getCurrentAlgebra());
        }
      });

    JMenuItem drawBelindaMI =
                    (JMenuItem)draw.add(new JMenuItem("Belinda"));
    drawBelindaMI.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getCurrentAlgebra() != null) drawBelinda(getCurrentAlgebra());
        }
      });

    JMenu showHide = (JMenu) menuBar.add(new JMenu("Show/Hide"));

    final JCheckBoxMenuItem showDiagLabelsCB = (JCheckBoxMenuItem)showHide.add(
                         new JCheckBoxMenuItem("Show Diagram Labels", true));

    showDiagLabelsCB.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          //shaper.getCurrentPanel().repaint();
          if (getLatDrawPanel().getDiagram() != null) {
            getLatDrawPanel().getDiagram().setPaintLabels(
                                           showDiagLabelsCB.isSelected());
          }
          repaint();
        }
      });



    setJMenuBar(menuBar);

    openMI.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          //should this if statement go inside the try?
             try {
               open();
             }
             catch (IOException err) {
               err.printStackTrace();
             }
          }
      });
    
    

    exitMI.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (isDirty()) {
            if (checkSave()) {
              System.exit(0);
            }
          }
          else {
            System.exit(0);
          }
        }
      });

  }
*/


  public void algebraStructureChanged() {
    if (algebrasController == null) return;
    javax.swing.SwingUtilities.invokeLater(() -> {
      algebrasController.refreshSignaturesFromOpenAlgebras();
      algebrasController.applySignatureFilter();
    });
  }

  
  public GUIAlgebra addAlgebra(SmallAlgebra alg, boolean makeCurrent) {
    return addAlgebra(alg, null, makeCurrent);
  }

  public GUIAlgebra addAlgebra(SmallAlgebra alg) {
    return addAlgebra(alg, null, true);
  }
  
  /**
   * Make alg into a GUIAlgebra, add it to the list and to the table
   * with the list and scroll to the bottom.
   * Right now the list of algebras is maintained the algebraTableModel.
   * We may want to change that.
   */
  public GUIAlgebra addAlgebra(SmallAlgebra alg, File file, boolean makeCurrent) {
    return addAlgebra(new GUIAlgebra(alg, file), makeCurrent);
  }
  
  /**
   * Make alg into a GUIAlgebra, add it to the list and to the table
   * with the list and scroll to the bottom.
   * Right now the list of algebras is maintained the algebraTableModel.
   * We may want to change that.
   */
  
  public GUIAlgebra addAlgebra(GUIAlgebra gAlg, boolean makeCurrent) {
    getAlgebraList().add(gAlg, makeCurrent);

    final JTable algTable = uacalcUI.getAlgListTable();
    algTable.revalidate();
    scrollToBottom(algTable);
    algTable.repaint();

    javax.swing.SwingUtilities.invokeLater(() -> {
      algebraStructureChanged();

      if (makeCurrent) {
        // wait until the filter/sorter refresh has run too
        javax.swing.SwingUtilities.invokeLater(() -> {
          selectAlgebraInTable(gAlg);
        });
      }
    });

    return gAlg;
  }
  
  // TODO: soon each algebra will have a separate monitorPanel and these
  // will change.
  //public MonitorPanel getMonitorPanel() { return monitorPanel; }
  
  //public ProgressReport getMonitor() { return monitorPanel.getProgressReport(); }
  
  public LatDrawPanel getLatDrawPanel() {
    //return tabs.getLatticeDrawer();
    return null;
  }
  
  public AlgebraEditorController getAlgebraEditorController() {
    return algEditorController;
  }
  
  public ComputationsController getComputationsController() {
    return computationsController;
  }

  public AlgebrasController getAlgebrasController() {
    return algebrasController; 
  }
  
  public RelationsController getRelationsController() {
    return relationsController; 
  }
  
  public ConController getConController() {
    return conController;
  }
  
  public SubController getSubController() {
    return subController;
  }
  
  public DrawingController getDrawingController() {
    return drawingController;
  }
  
  public void resetToolBar() {
    // TODO: fix these
    //mainPanel.remove(toolBar);
    //toolBar = tabs.getCurrentToolBar();
    //mainPanel.add(toolBar, BorderLayout.NORTH);
    uacalcUI.validate();
    uacalcUI.repaint();
  }
  
/*
  public void drawSub(SmallAlgebra alg) {
    final int maxSize = 100;
    if (alg.sub().cardinality() > maxSize) {
      JOptionPane.showMessageDialog(uacalcUI.getFrame(),
          "<html>This subalgebra lattice has " + alg.sub().cardinality() + " elements.<br>"
          + "At most " + maxSize + " elements allowed.</html>",
          "Sub Too Big",
          JOptionPane.WARNING_MESSAGE);
      return;
    }
    BasicLattice lat = new BasicLattice("", alg.sub());
    getLatDrawPanel().setDiagram(lat.getDiagram());
    uacalcUI.repaint();
  }


  public void drawCon(SmallAlgebra alg) {
    final int maxSize = 100;
    if (alg.con().cardinality() > maxSize) {
      JOptionPane.showMessageDialog(uacalcUI.getFrame(),
          "<html>This congruence lattice has " + alg.con().cardinality() + " elements.<br>"
          + "At most " + maxSize + " elements allowed.</html>",
          "Con Too Big",
          JOptionPane.WARNING_MESSAGE);
      System.out.println("Con has " + alg.con().cardinality() + " elements");
      System.out.println("we allow at most " + maxSize + " elements.");
      return;
    }
    BasicLattice lat = new BasicLattice("", alg.con(), true);
    getLatDrawPanel().setDiagram(lat.getDiagram());
    uacalcUI.repaint();
  }


  public void drawBelinda(SmallAlgebra alg) {
    final int maxSize = 100;
    if (alg.cardinality() > maxSize) {
      JOptionPane.showMessageDialog(uacalcUI.getFrame(),
          "<html>This algebra has " + alg.cardinality() + " elements.<br>"
          + "At most " + maxSize + " elements allowed.</html>",
          "Algebra Too Big",
          JOptionPane.WARNING_MESSAGE);
      return;
    }
    Operation op = null;
    for (Iterator it = alg.operations().iterator(); it.hasNext(); ) {
      Operation opx = (Operation)it.next();
      if (Operations.isCommutative(opx) && Operations.isIdempotent(opx)
                                      && Operations.isAssociative(opx)) {
        op = opx;
        break;
      }
    }
    if (op == null) {
      JOptionPane.showMessageDialog(uacalcUI.getFrame(),
          "<html>This algebra does not have any semilattice operations.<br>",
          "No Lattice Operation",
          JOptionPane.WARNING_MESSAGE);
      System.out.println("Could not find a semilattice operations.");
      return;
    }
    java.util.List univ = new ArrayList(alg.universe());
    BasicLattice lat = Lattices.latticeFromMeet("", univ, op);
    //LatDrawer.drawLattice(lat);
    getLatDrawPanel().setDiagram(lat.getDiagram());
    uacalcUI.repaint();
  }
*/  
  public boolean save() {
    System.out.println("current alg = " + getCurrentAlgebra());
    if (getCurrentAlgebra() == null) return true;
    SmallAlgebra alg = getCurrentAlgebra().getAlgebra();
    if (alg == null) return false;
    for (Operation op : alg.operations()) {
      if (op instanceof OperationWithDefaultValue) {
        if (!((OperationWithDefaultValue)op).isTotal()) {
          uacalcUI.beep();
          JOptionPane.showMessageDialog(uacalcUI.getFrame(),
              "<html><center>Not all operations are total.<br>" 
                  + "Fill in the tables<br>"
                  + "or set a default value.</center></html>",
                  "Incomplete operation(s)",
                  JOptionPane.WARNING_MESSAGE);
          return false;
        }
      }
    }
    alg.setName(uacalcUI.getAlgNameTextField().getText());
    alg.setDescription(getAlgebraEditorController().updateDescription());

    File f = getCurrentAlgebra().getFile();
    if (f == null) return saveAs(org.uacalc.io.ExtFileFilter.UA_EXT);
    String ext = ExtFileFilter.getExtension(f);
    boolean newFormat = true;
    if (ext.equals(ExtFileFilter.ALG_EXT)) newFormat = false;
    try {
    //AlgebraIO.writeAlgebraFile(getCurrentAlgebra().getAlgebra(), f, !newFormat);
      AlgebraIO.writeAlgebraFile(alg, f, !newFormat);
      //setDirty(false);
      getCurrentAlgebra().setNeedsSave(false);
      setTitle();
      uacalcUI.repaint();
      return true;
    }
    catch (IOException e) {
      uacalcUI.beep();
      e.printStackTrace();
      return false;
    }
  }

  public boolean saveAs(String ext) {
    if (getCurrentAlgebra() == null) return true;
    SmallAlgebra alg = getCurrentAlgebra().getAlgebra();
    if (alg == null) return false;  
    for (Operation op : alg.operations()) {
      if (op instanceof OperationWithDefaultValue) {
        if (!((OperationWithDefaultValue)op).isTotal()) {
          uacalcUI.beep();
          JOptionPane.showMessageDialog(uacalcUI.getFrame(),
              "<html><center>Not all operations are total.<br>" 
              + "Fill in the tables<br>"
              + "or set a default value.</center></html>",
              "Incomplete operation(s)",
              JOptionPane.WARNING_MESSAGE);
          return false;
        }
      }
    }
    alg.setName(uacalcUI.getAlgNameTextField().getText());
    alg.setDescription(getAlgebraEditorController().updateDescription());
    
    //if (getCurrentAlgebra().getAlgebra().algebraType() == SmallAlgebra.AlgebraType.BASIC) {
    //  alg = getAlgebraEditorController().makeAlgebra();
    //}
    //else {
    //  alg = getCurrentAlgebra().getAlgebra();
    //  alg.setName(uacalcUI.getAlgNameTextField().getText());
    //  alg.setDescription(getAlgebraEditorController().updateDescription());
    //}

    //if (!getAlgebraEditorController().sync()) return false;
    boolean newFormat = true;
    if (ext.equals(org.uacalc.io.ExtFileFilter.ALG_EXT)) newFormat = false;
    if (fileChooser == null) initFileChooser();
    /*
    String pwd = getPrefs().get("algebraDir", null);
    if (pwd == null) pwd = System.getProperty("user.dir");
    JFileChooser fileChooser;
    if (pwd != null)
      fileChooser = new JFileChooser(pwd);
    else
      fileChooser = new JFileChooser();

    fileChooser.addChoosableFileFilter(
         newFormat ? 
         new ExtFileFilter("Alg Files New Format (*.ua, *.xml)", 
                            ExtFileFilter.UA_EXTS) :
         new ExtFileFilter("Alg Files Old Format (*.alg)", 
                            ExtFileFilter.ALG_EXT));
    */
    int option = fileChooser.showSaveDialog(uacalcUI.getFrame());
    if (option == JFileChooser.APPROVE_OPTION) {
      // save original user selection
      File selectedFile = fileChooser.getSelectedFile();
      File f = selectedFile;
      try {
        String extension = ExtFileFilter.getExtension(f);
        if (extension == null || !extension.equals(ext)) {
          f = new File(f.getCanonicalPath() + "." + ext);
        }
        if (f.exists()) {
          Object[] options = { "Yes", "No" };
          int n = JOptionPane.showOptionDialog(uacalcUI.getFrame(),
              "The file already exists. Overwrite?", "Algebra Exists",
              JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
              options, options[0]);
          if (n == JOptionPane.NO_OPTION) {
            return saveAs(ext);
          }
        }
        AlgebraIO.writeAlgebraFile(alg, f, !newFormat);
        getPrefs().put("algebraDir", f.getParent());
        setCurrentFile(f);
        //setDirty(false);
        getCurrentAlgebra().setFile(f);
        getCurrentAlgebra().setNeedsSave(false);
        setTitle();
        uacalcUI.repaint();
        return true;
      }
      catch (IOException e) {
        beep();
        return false;
      }
    }
    return false;
  }

  public boolean saveLatexAs(String ext) {
    ext = "tex";
    if (getCurrentAlgebra() == null) return true;
    SmallAlgebra alg = getCurrentAlgebra().getAlgebra();
    if (alg == null) return false;

    for (Operation op : alg.operations()) {
        if (op instanceof OperationWithDefaultValue) {
            if (!((OperationWithDefaultValue) op).isTotal()) {
                uacalcUI.beep();
                JOptionPane.showMessageDialog(uacalcUI.getFrame(),
                    "<html><center>Not all operations are total.<br>" +
                    "Fill in the tables<br>" +
                    "or set a default value.</center></html>",
                    "Incomplete operation(s)",
                    JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    alg.setName(uacalcUI.getAlgNameTextField().getText());
    alg.setDescription(getAlgebraEditorController().updateDescription());

    String latex;
    try {
        Set<String> opNames = new LinkedHashSet<>();
        for (Operation op : alg.operations()) {
            opNames.add(op.symbol().name());
        }
        latex = latexExport(alg, opNames);
    } catch (IOException | InterruptedException e) {
        beep();
        e.printStackTrace();
        return false;
    }

    if (fileChooser == null) initFileChooser();
    int option = fileChooser.showSaveDialog(uacalcUI.getFrame());
    if (option == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        File f = selectedFile;
        try {
            String extension = ExtFileFilter.getExtension(f);
            if (extension == null || !extension.equals(ext)) {
                f = new File(f.getCanonicalPath() + "." + ext);
            }

            if (f.exists()) {
                Object[] options = {"Yes", "No"};
                int n = JOptionPane.showOptionDialog(uacalcUI.getFrame(),
                        "The file already exists. Overwrite?", "File Exists",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        options, options[0]);
                if (n == JOptionPane.NO_OPTION) {
                    return saveLatexAs(ext); // retry
                }
            }

            Files.writeString(f.toPath(), latex);
            getPrefs().put("latexExportDir", f.getParent());
            uacalcUI.repaint();
            return true;
        } catch (IOException e) {
            beep();
            e.printStackTrace();
            return false;
        }
    }

    return false; // user canceled 
  }

  public boolean saveFullLatexAs(String ext) {
    ext = "tex";
    getDrawingController().getLatDrawer().repaint();
    boolean reversed = getDrawingController().getLatDrawer().getUpsideDown();
    GUIAlgebra gui_alg = getCurrentAlgebra();
    if (gui_alg == null) return true;
    SmallAlgebra alg = getCurrentAlgebra().getAlgebra();
    if (alg == null) return false;

    for (Operation op : alg.operations()) {
        if (op instanceof OperationWithDefaultValue) {
            if (!((OperationWithDefaultValue) op).isTotal()) {
                uacalcUI.beep();
                JOptionPane.showMessageDialog(uacalcUI.getFrame(),
                    "<html><center>Not all operations are total.<br>" +
                    "Fill in the tables<br>" +
                    "or set a default value.</center></html>",
                    "Incomplete operation(s)",
                    JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    if (getDrawingController().isDrawable(gui_alg, true)) {
      getDrawingController().drawAlg();
    } else {
      uacalcUI.getMainController().beep();
      uacalcUI.getMainController().setUserWarning(
          "This algebra is not drawable!", false);
      return false;
    }

    alg.setName(uacalcUI.getAlgNameTextField().getText());
    alg.setDescription(getAlgebraEditorController().updateDescription());

    String latex;
    try {
        Set<String> opNames = new LinkedHashSet<>();
        for (Operation op : alg.operations()) {
            opNames.add(op.symbol().name());
        }
        latex = latexExport(alg, opNames);
    } catch (IOException | InterruptedException e) {
        beep();
        e.printStackTrace();
        return false;
    }
    Diagram diagram = getDrawingController().getLatDrawer().getDiagram();
    String tikz = "";
    if (diagram != null ){
      try {
          tikz = tikzExport(diagram, alg.getName(), reversed, false);
      } catch (IOException | InterruptedException e) {
          beep();
          e.printStackTrace();
          return false;
      }
    }
    String output = latex + "\n\n" + tikz;


    if (fileChooser == null) initFileChooser();
    int option = fileChooser.showSaveDialog(uacalcUI.getFrame());
    if (option == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        File f = selectedFile;
        try {
            String extension = ExtFileFilter.getExtension(f);
            if (extension == null || !extension.equals(ext)) {
                f = new File(f.getCanonicalPath() + "." + ext);
            }

            if (f.exists()) {
                Object[] options = {"Yes", "No"};
                int n = JOptionPane.showOptionDialog(uacalcUI.getFrame(),
                        "The file already exists. Overwrite?", "File Exists",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        options, options[0]);
                if (n == JOptionPane.NO_OPTION) {
                    return saveFullLatexAs(ext); // retry
                }
            }

            Files.writeString(f.toPath(), output);
            getPrefs().put("latexExportDir", f.getParent());
            uacalcUI.repaint();
            return true;
        } catch (IOException e) {
            beep();
            e.printStackTrace();
            return false;
        }
    }

    return false; // user canceled 
  }

  public boolean saveTikzAs(Diagram diagram) {
    boolean reversed = getDrawingController().getLatDrawer().getUpsideDown();
    String ext = "tex";
    if (getCurrentAlgebra() == null) return true;
    SmallAlgebra alg = getCurrentAlgebra().getAlgebra();
    if (alg == null) return false;


    alg.setName(uacalcUI.getAlgNameTextField().getText());
    alg.setDescription(getAlgebraEditorController().updateDescription());

    String tikz;
    try {
        tikz = tikzExport(diagram, alg.getName(), reversed, false);
    } catch (IOException | InterruptedException e) {
        beep();
        e.printStackTrace();
        return false;
    }

    if (fileChooser == null) initFileChooser();
    int option = fileChooser.showSaveDialog(uacalcUI.getFrame());
    if (option == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        File f = selectedFile;
        try {
            String extension = ExtFileFilter.getExtension(f);
            if (extension == null || !extension.equals(ext)) {
                f = new File(f.getCanonicalPath() + "." + ext);
            }

            if (f.exists()) {
                Object[] options = {"Yes", "No"};
                int n = JOptionPane.showOptionDialog(uacalcUI.getFrame(),
                        "The file already exists. Overwrite?", "File Exists",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        options, options[0]);
                if (n == JOptionPane.NO_OPTION) {
                    return saveLatexAs(ext); // retry
                }
            }

            Files.writeString(f.toPath(), tikz);
            getPrefs().put("latexExportDir", f.getParent());
            uacalcUI.repaint();
            return true;
        } catch (IOException e) {
            beep();
            e.printStackTrace();
            return false;
        }
    }

    return false; // user canceled 
  }

  //exporter calls
  public String tikzExport(Diagram diagram, String captionText, boolean reversed, boolean labelInsideVertex) throws IOException, InterruptedException {
    if (diagram == null) return "";
    DiagramData data = new DiagramData();
    data.collectVertexAndEdgeData(diagram);
    data.roundAndScaleData(10.0, 6.0);
    if (reversed == true) {data.upSideDown();}
    TikzExporter tikzExp = new TikzExporter(data.verticesLabels, data.vertices, data.edges, captionText, labelInsideVertex);
    return tikzExp.runLua(); 
  }

  //helper pro předání informací o selektovaných operací pro lua
  private static String buildOpsHeader(Set<String> opNames) {
    if (opNames == null) return "";
    // rozhodnutí: když je prázdný set -> exportuj NIC (pošleme prázdný blok)
    StringBuilder sb = new StringBuilder();
    sb.append("<uacalcExportOps>\n");
    for (String s : opNames) {
      sb.append("  <op>").append(s).append("</op>\n");
    }
    sb.append("</uacalcExportOps>");
    return sb.toString();
  }


  public String latexExport(SmallAlgebra alg, Set<String> selectedOps) throws IOException, InterruptedException {
    File f = File.createTempFile("tempAlg", ".ua");
    try {
        AlgebraIO.writeAlgebraFile(alg, f, false);
        String fileContent = Files.readString(f.toPath());
        String payload = buildOpsHeader(selectedOps) + "\n" + fileContent;
        TexExporter texExp = new TexExporter(payload);
        return texExp.runLua();
    } finally {
        f.delete(); // clean up temp file
    }
  }

  public void setUpBatchDialog() {
    JFrame mainFrame = uacalcUI.getFrame();  // your main window
    BatchExportDialog dialog = new BatchExportDialog(mainFrame, algebraList, this);
    dialog.setVisible(true);
  }

  public void batchExportAlgebras(BatchAlgebraTableModel model, int[] rows, Set<String> selectedOps, File output, boolean singleFile, boolean tikzAlgebra, boolean labelInsideAlg, boolean reversedDraw, boolean tikzCon, boolean labelInsideCon, boolean tikzSub, boolean labelInsideSub) {
    if (singleFile) {
      exportToSingleFile(model, rows, selectedOps, output, tikzAlgebra, labelInsideAlg, reversedDraw, tikzCon, labelInsideCon, tikzSub, labelInsideSub);
    } else {
      exportToMultipleFiles(model, rows, selectedOps, output, tikzAlgebra, labelInsideAlg, reversedDraw, tikzCon, labelInsideCon, tikzSub, labelInsideSub);
    }
  }


  public void exportToMultipleFiles(BatchAlgebraTableModel model, int[] rows, Set<String> selectedOps,  File folder, boolean tikzAlgebra, boolean labelInsideAlg, boolean reversedDraw, boolean tikzCon, boolean labelInsideCon, boolean tikzSub, boolean labelInsideSub) {
      for (int row : rows) {
          GUIAlgebra gAlg = model.getAlgebraAt(row);
          exportToLatexFile(gAlg, folder, selectedOps, tikzAlgebra, labelInsideAlg, reversedDraw, tikzCon, labelInsideCon, tikzSub, labelInsideSub);
      }
  }

  public void exportToLatexFile(GUIAlgebra gAlg, File folder, Set<String> selectedOps, boolean tikzAlgebra, boolean labelInsideAlg, boolean reversedDraw, boolean tikzCon, boolean labelInsideCon, boolean tikzSub, boolean labelInsideSub) {
    SmallAlgebra alg = gAlg.getAlgebra();
      String filename = alg.getName().replaceAll("\\s+", "_") + ".tex";
      File outFile = new File(folder, filename);

      try {
        String latex = latexExport(alg, selectedOps);
        StringBuilder tikzContent = new StringBuilder();
        if(getDrawingController().isDrawable(gAlg, true) && tikzAlgebra == true) {
          setCurrentAlgebra(gAlg);
          getDrawingController().drawAlg();
          String tikz = "";
          Diagram diagram = getDrawingController().getLatDrawer().getDiagram();
          if (diagram != null ){
            try {
              tikz = "\n\n" + tikzExport(diagram, alg.getName(), reversedDraw, labelInsideAlg);
            } catch (IOException | InterruptedException e) {
              beep();
              e.printStackTrace();
            }
            tikzContent.append(tikz).append("\n");
          }
        }
        if(tikzCon == true) {
          setCurrentAlgebra(gAlg);
          getConController().drawCon();
          String tikz = "";
          Diagram diagram = getConController().getConLatDrawer().getDiagram();
          if (diagram != null ){
            try {
              tikz = "\n\n" + tikzExport(diagram, "Con("+alg.getName()+")", false, labelInsideCon);
            } catch (IOException | InterruptedException e) {
              beep();
              e.printStackTrace();
            }
          }

          tikzContent.append(tikz).append("\n");
        }
        if(tikzSub == true) {
          setCurrentAlgebra(gAlg);
          getSubController().drawSub();
          String tikz = "";
          Diagram diagram = getSubController().getSubLatDrawer().getDiagram();
          if (diagram != null ){
            try {
              tikz = "\n\n" + tikzExport(diagram, "Sub("+alg.getName()+")", false, labelInsideSub);
            } catch (IOException | InterruptedException e) {
              beep();
              e.printStackTrace();
            }
          }
          tikzContent.append(tikz).append("\n");
        }
        // Ensure parent directory exists
        outFile.getParentFile().mkdirs();
        Files.writeString(outFile.toPath(), latex+tikzContent.toString());
      } catch (IOException | InterruptedException ex) {
          JOptionPane.showMessageDialog(
              uacalcUI.getFrame(),
              "Error exporting " + alg.getName() + ": " + ex.getMessage(),
              "Export Error",
              JOptionPane.ERROR_MESSAGE
          );
      }
    }

  public void exportToSingleFile(BatchAlgebraTableModel model, int[] rows, Set<String> selectedOps, File outputFile, boolean tikzAlgebra, boolean labelInsideAlg, boolean reversedDraw, boolean tikzCon, boolean labelInsideCon, boolean tikzSub, boolean labelInsideSub) {
    try {
        StringBuilder allContent = new StringBuilder();
        for (int row : rows) {
            GUIAlgebra gAlg = model.getAlgebraAt(row);
            SmallAlgebra alg = gAlg.getAlgebra();
            if((tikzAlgebra ? 1:0) + (tikzCon ? 1 : 0) + (tikzSub ? 1 : 0) >1 && row != rows[0]){
              allContent.append("\\clearpage");
            }
            allContent.append("% Export: ").append(alg.getName()).append("\n");
            allContent.append(latexExport(alg, selectedOps));
            String tikz = "";
            if(getDrawingController().isDrawable(gAlg, true) && tikzAlgebra == true) {
              setCurrentAlgebra(gAlg);
              getDrawingController().drawAlg();
              Diagram diagram = getDrawingController().getLatDrawer().getDiagram();
              if (diagram != null ){
                try {
                  tikz = "\n\n" + tikzExport(diagram, alg.getName(), reversedDraw, labelInsideAlg);
                } catch (IOException | InterruptedException e) {
                  beep();
                  e.printStackTrace();
                }
              }
            }
            allContent.append(tikz).append("\n");
            if(tikzCon == true) {
              setCurrentAlgebra(gAlg);
              getConController().drawCon();
              Diagram diagram = getConController().getConLatDrawer().getDiagram();
              if (diagram != null ){
                try {
                  tikz = "\n\n" + tikzExport(diagram, "Con("+alg.getName()+")", false, labelInsideCon);
                } catch (IOException | InterruptedException e) {
                  beep();
                  e.printStackTrace();
                }
              }
              allContent.append(tikz).append("\n");
            }
            
            if(tikzSub == true) {
              setCurrentAlgebra(gAlg);
              getSubController().drawSub();
              Diagram diagram = getSubController().getSubLatDrawer().getDiagram();
              if (diagram != null){
                try {
                  tikz = "\n\n" + tikzExport(diagram, "Sub("+alg.getName()+")", false, labelInsideSub);
                } catch (IOException | InterruptedException e) {
                  beep();
                  e.printStackTrace();
                }
              }
              allContent.append(tikz).append("\n");
            }
          }
        // Ensure parent directory exists
        outputFile.getParentFile().mkdirs();
        Files.writeString(outputFile.toPath(), allContent.toString());
    } catch (IOException | InterruptedException e) {
        JOptionPane.showMessageDialog(
            uacalcUI.getFrame(),
            "Export error: " + e.getMessage(),
            "Export Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
  }



  public boolean writeLogTextArea() {
    JTextArea textArea = uacalcUI.getLogTextArea();
    if (textArea == null) return false;
    String text = textArea.getText();
    if (text == null) return false;
    String pwd = getPrefs().get("algebraDir", null);
    if (pwd == null) pwd = System.getProperty("user.dir");
    JFileChooser fileChooser;
    if (pwd != null)
      fileChooser = new JFileChooser(pwd);
    else
      fileChooser = new JFileChooser();

    fileChooser.addChoosableFileFilter(
        new ExtFileFilter("csv files (*.txt)",  ExtFileFilter.TXT_EXT));
     
    int option = fileChooser.showSaveDialog(uacalcUI.getFrame());
    if (option == JFileChooser.APPROVE_OPTION) {
      // save original user selection
      File selectedFile = fileChooser.getSelectedFile();
      File f = selectedFile;
      try {
        String extension = ExtFileFilter.getExtension(f);
        String ext = ExtFileFilter.TXT_EXT;
        if (extension == null || !extension.equals(ext)) {
          f = new File(f.getCanonicalPath() + "." + ext);
        }
        if (f.exists()) {
          Object[] options = { "Yes", "No" };
          int n = JOptionPane.showOptionDialog(uacalcUI.getFrame(),
              "The file already exists. Overwrite?", "File Exists",
              JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
              options, options[0]);
          if (n == JOptionPane.NO_OPTION) {
            return false;
          }
        }
        //tableToCSV(desc, model, new PrintStream(f));
        PrintStream out = new PrintStream(f);
        out.print(text);
        
        setTimedMessage("" + f + " saved");
        return true;
      }
      catch (IOException e) {
        beep();
        return false;
      }
    }
    return false;
  }

  public boolean writeCSVTable() {
    TermTableModel model = uacalcUI.getComputationsController().getCurrentTermTableModel();
    System.out.println("model = " + model);
    if (model == null) return false;
    String desc = model.getDescription();  
    String pwd = getPrefs().get("algebraDir", null);
    if (pwd == null) pwd = System.getProperty("user.dir");
    JFileChooser fileChooser;
    if (pwd != null)
      fileChooser = new JFileChooser(pwd);
    else
      fileChooser = new JFileChooser();

    fileChooser.addChoosableFileFilter(
        new ExtFileFilter("csv files (*.csv)",  ExtFileFilter.CSV_EXT));
         
    int option = fileChooser.showSaveDialog(uacalcUI.getFrame());
    if (option == JFileChooser.APPROVE_OPTION) {
      // save original user selection
      File selectedFile = fileChooser.getSelectedFile();
      File f = selectedFile;
      try {
        String extension = ExtFileFilter.getExtension(f);
        String ext = ExtFileFilter.CSV_EXT;
        if (extension == null || !extension.equals(ext)) {
          f = new File(f.getCanonicalPath() + "." + ext);
        }
        if (f.exists()) {
          Object[] options = { "Yes", "No" };
          int n = JOptionPane.showOptionDialog(uacalcUI.getFrame(),
              "The file already exists. Overwrite?", "File Exists",
              JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
              options, options[0]);
          if (n == JOptionPane.NO_OPTION) {
            return false;
          }
        }
        tableToCSV(desc, model, new PrintStream(f));
        setTimedMessage("" + f + " saved");
        return true;
      }
      catch (IOException e) {
        beep();
        return false;
      }
    }
    return false;
  }

  private void initFileChooser() {
    String pwd = getPrefs().get("algebraDir", null);
    if (pwd == null) pwd = System.getProperty("user.dir");
    if (pwd != null) fileChooser = new JFileChooser(pwd);
    else fileChooser = new JFileChooser();
    fileChooser.addChoosableFileFilter(
        new ExtFileFilter("Algebra Files (*.ua, *.alg)", ExtFileFilter.ALL_ALG_EXTS));
    fileChooser.addChoosableFileFilter(
        new ExtFileFilter("Mace4 Models (*.m4)", ExtFileFilter.MACE4_EXTS));
    AlgebraPreviewer algPreviewer = new AlgebraPreviewer(uacalcUI);
    fileChooser.setPreferredSize(new Dimension(850,500));
    fileChooser.setAccessory(algPreviewer);
    fileChooser.addPropertyChangeListener(algPreviewer); // to receive selection changes
    fileChooser.setMultiSelectionEnabled(true);
  }
  
  public void open() {
    String pwd = getPrefs().get("algebraDir", null);
    if (pwd == null) pwd = System.getProperty("user.dir");
    File theFile = null;
    // pwd = currentFolder;
    // JFileChooser fileChooser; // now a field
    if (fileChooser == null) initFileChooser();
    int option = fileChooser.showOpenDialog(uacalcUI.getFrame());

    if (option==JFileChooser.APPROVE_OPTION) {
      File[] theFiles = fileChooser.getSelectedFiles();
      theFile = fileChooser.getSelectedFile();
      System.out.println("files: " + Arrays.toString(fileChooser.getSelectedFiles()));
      currentFolder = theFile.getParent();
      //getPrefs().put("algebraDir", theFile.getParent()); // done below
      if (theFiles.length < 2) open(theFile);
      else open(theFiles);
    }
  }
  
  public void open(File file ) {
    java.util.List<SmallAlgebra> algs = null;
    try {
      algs = AlgebraIO.readAlgebraListFile(file);
      //a = AlgebraIO.readAlgebraFile(file);
      // TODO: add to list of algs
    }
    catch (BadAlgebraFileException e) {
      System.err.println("Bad algebra file " + file);
      e.printStackTrace();
      beep();
    }
    catch (IOException e) {
      System.err.println("IO error on file " + file);
      e.printStackTrace();
      beep();
    }
    catch (NullPointerException e) {
      System.err.println("open failed");
      beep();
    }
    if (algs == null || algs.size() == 0) {
      System.err.println("open failed");
      beep();
      return;
    }
    SmallAlgebra[] algsvec = new SmallAlgebra[algs.size()];
    for (int i = 0; i < algs.size(); i++) {
      algsvec[i] = algs.get(i);
    }
    File[] files = null;
    if (algs.size() == 1) files = new File[] {file};
    addAlgebras(algsvec, files);
    
    //SmallAlgebra a = openAux(file);
    /*
    if (a != null) {
      if (a.algebraType() == SmallAlgebra.AlgebraType.BASIC) {
        a.convertToDefaultValueOps();
      }
      setCurrentAlgebra(addAlgebra(a, file, true));
      uacalcUI.repaint();
    }
    */
    uacalcUI.repaint();
  }

  public SmallAlgebra openAux(File file) {
    getPrefs().put("algebraDir", file.getParent());
    SmallAlgebra a = null;
    try {
      java.util.List<SmallAlgebra> algs = AlgebraIO.readAlgebraListFile(file);
      //a = AlgebraIO.readAlgebraFile(file);
      a = algs.get(0);
      // TODO: add to list of algs
    }
    catch (BadAlgebraFileException e) {
      System.err.println("Bad algebra file " + file);
      e.printStackTrace();
      beep();
    }
    catch (IOException e) {
      System.err.println("IO error on file " + file);
      e.printStackTrace();
      beep();
    }
    catch (NullPointerException e) {
      System.err.println("open failed");
      beep();
    }
    return a;
  }
  
  public void open(File[] files) {
    SmallAlgebra[] algs = new SmallAlgebra[files.length];
    for (int i = 0; i < files.length; i++) {
      algs[i] = openAux(files[i]);
    }
    addAlgebras(algs, files);
  }
  
  private void addAlgebras(SmallAlgebra[] algs, File[] files) {
    boolean makeCurr = true;
    for (int i = 0; i < algs.length; i++) {
      if (algs[i] != null) {
        SmallAlgebra a = algs[i];
        if (a.algebraType() == SmallAlgebra.AlgebraType.BASIC) {
          a.convertToDefaultValueOps();
        }
        File file = null;
        if (files != null) file = files[i];
        System.out.println("file: " + file);
        if (makeCurr) {
          setCurrentAlgebra(addAlgebra(a, file, true));
          makeCurr = false;
        }
        else addAlgebra(a, file, false);
      }
    }
    algebraStructureChanged();
  }
  
  public void loadBuiltIn() {
    Object[] builtInAlgs = new Object[] {
      "polin", "lyndon", "n5", "m3", "m4", "baker2",
      "lat2", "lat2-01", "ba2",
      "sym3", "cyclic2", "cyclic3", "d16", "z3"
    };
    String algName = (String) JOptionPane.showInputDialog(uacalcUI.getFrame(),
        "Choose an algebra:\n", "Choose an algebra",
        JOptionPane.PLAIN_MESSAGE, null, // can be an icon,
        builtInAlgs, builtInAlgs[0]);
    if ((algName == null) || (algName.length() == 0)) return;
    System.out.println(algName + " choosen");
    //ClassLoader cl = this.getClass().getClassLoader();
    ClassLoader cl = Thread.currentThread().getContextClassLoader();

    
    String theFileName = "algebras/" + algName + ".ua";
    System.out.println("file: " + theFileName);
    //InputStream is = ClassLoader.getSystemResourceAsStream(theFileName);
    InputStream is = cl.getResourceAsStream(theFileName);
    if (is == null) {
      System.out.println("null InputStream");
      return;
    }
    SmallAlgebra a = null;
    try {
      a = AlgebraIO.readAlgebraFromStream(is);
    }
    catch (BadAlgebraFileException e) {
      e.printStackTrace();
      beep();
    }
    catch (IOException e) {
      e.printStackTrace();
      beep();
    }
    catch (NullPointerException e) {
      System.err.println("open failed");
      beep();
    }
    if (a != null) {
      if (a.algebraType() == SmallAlgebra.AlgebraType.BASIC) {
        a.convertToDefaultValueOps();
      }
      setCurrentAlgebra(addAlgebra(a, null, true));
      uacalcUI.repaint();
    }
    algebraStructureChanged();
  }
  
  public void switchAlgebra(GUIAlgebra gAlg) {
    setCurrentFile(gAlg.getFile());
    setTitle();
  }

  public GUIAlgebra getCurrentAlgebra() { return currentAlgebra; }
  
  public void removeCurrentAlgebra() {
    // need to check if it needs saving
    getAlgebraList().removeAlgebra(getCurrentAlgebra());
    //uacalcUI.getAlgListTable().setRowSelectionInterval(index, index);
    uacalcUI.getAlgListTable().revalidate();
    scrollToBottom(uacalcUI.getAlgListTable());
    uacalcUI.getAlgListTable().repaint();

    algebraStructureChanged();
  }

  //duplicate algebra workaround: saving temp and loading,renaming.
  //In java it is complicated to do deepcopy
  public void duplicateCurrentAlgebra() {
    GUIAlgebra current = getCurrentAlgebra();
    if (current == null) return; // add this check too to be safe
    SmallAlgebra alg = current.getAlgebra();
    if (alg instanceof BasicAlgebra) {
      try {
        File f = File.createTempFile("tempAlg", ".ua");
        try {
          AlgebraIO.writeAlgebraFile(alg, f, false);
          open(f);
          SmallAlgebra copied_alg = getCurrentAlgebra().getAlgebra();
          copied_alg.setName(alg.getName() + " (copy)");
        } finally {
          f.delete();
          getCurrentAlgebra().setFile(null);
          getCurrentAlgebra().needsSave();

        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    } else {
      setTimedMessage("You can duplicate only a Basic Algebra!");
    }
    algebraStructureChanged();
  }
  
  public void setCurrentAlgebra(SmallAlgebra alg) {
    setCurrentAlgebra(new GUIAlgebra(alg));
  }

  private void selectAlgebraInTable(GUIAlgebra target) {
    if (target == null) return;

    JTable algTable = uacalcUI.getAlgListTable();
    if (algTable == null) return;

    int modelIndex = -1;
    for (int i = 0; i < algebraList.size(); i++) {
      if (algebraList.get(i) == target) {
        modelIndex = i;
        break;
      }
    }

    if (modelIndex < 0) {
      algTable.clearSelection();
      return;
    }

    int viewIndex = -1;
    try {
      if (algTable.getRowSorter() != null) {
        viewIndex = algTable.convertRowIndexToView(modelIndex);
      } else {
        viewIndex = modelIndex;
      }
    } catch (RuntimeException ex) {
      viewIndex = -1;
    }

    if (viewIndex >= 0 && viewIndex < algTable.getRowCount()) {
      algTable.setRowSelectionInterval(viewIndex, viewIndex);
      algTable.scrollRectToVisible(algTable.getCellRect(viewIndex, 0, true));
    } else {
      // row is probably filtered out
      algTable.clearSelection();
    }
  }

  public void setCurrentAlgebra(GUIAlgebra alg) {
    if (alg == null) return;
    currentAlgebra = alg;
    setTitle();

    javax.swing.SwingUtilities.invokeLater(() -> selectAlgebraInTable(alg));

    final int idx = uacalcUI.getTabbedPane().getSelectedIndex();
    getConController().drawCon(alg.getAlgebra(), idx == 4 ? true : false);
    uacalcUI.setEmptyOpTableModel();
    getAlgebraEditorController().setAlgebra(alg);
    getRelationsController().refreshImpForCurrentAlgebra();
    getSubController().drawSub(alg.getAlgebra(), idx == 5 ? true : false);
    getDrawingController().drawAlg(alg, idx == 6 ? true : false);
  }
  public Random getRandom() {
    return random;
  }
  
  public void setRandomSeed(long seed) {
    random.setSeed(seed);
  }

  public boolean isDirty() { return dirty; }
  
  public void setDirty() {
    setDirty(true);
  }

  public void setDirty(boolean v){
    dirty = v;
    setTitle();
  }
  
  public void beep() {
    Toolkit.getDefaultToolkit().beep();
  }

  public void setTitle() {
    GUIAlgebra gAlg = getCurrentAlgebra();
    if (gAlg == null) {
      uacalcUI.setTitle(progName + title);
    }
    else {
      final boolean dirty = getCurrentAlgebra().needsSave();
      final File file = gAlg.getFile();
      final String name = file != null ? file.getName() : "";
      uacalcUI.setTitle(progName + name + (dirty ? " **" : ""));
    }
  }
  
  public void setNew() {
    currentFile = null;
    uacalcUI.setTitle("New **");
    setTitle();
  }


  public GUIAlgebraList getAlgebraList() {
    return algebraList;
  }
  
  public void setAlgListComboBox() {
    algListComboBox.removeAllItems();
    for (GUIAlgebra gAlg : getAlgebraList()) {
      algListComboBox.addItem(gAlg);
    }
  }
  
  /**
   * Give the user a message, set in the dimension panal info area.
   */
  public void setUserMessage(String msg) {
    setUserMessage(msg, false);
  }

  /**
   * Give the user a message, set in the dimension panal info area 
   * and possibly a popup.
   */
  public void setUserMessage(String msg, boolean popup) {
    uacalcUI.getMsgTextField().setText(msg);
    if (popup) {
      JOptionPane.showMessageDialog(uacalcUI.getFrame(), msg, "Information",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }
  
  /**
   * Clear the user message.
   */
  public void clearUserMessage() {
    uacalcUI.getMsgTextField().setText("");
  }

  /**
   * Give the user a warning, set in the dimension panal info area.
   */
  public void setUserWarning(String msg) {
    setUserWarning(msg, true);
  }

  /**
   * Give the user a warning, set in the dimension panal info area
   * and pop up a info dialog.
   */
  public void setUserWarning(String msg, boolean popup) {
    setUserMessage(msg);
    uacalcUI.getMsgTextField().setForeground(Color.RED);
    beep();
    if (popup) {
      beep();
      JOptionPane.showMessageDialog(uacalcUI.getFrame(), msg, "Warning",
        JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Give the user a message and erase it after a few seconds.
   */
  public void setTimedMessage(String msg) {
    setUserMessage(msg);
    final int delay = 8000;
    final ActionListener eraser = new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        setUserMessage("");
      }
    };
    final javax.swing.Timer delayer = new javax.swing.Timer(delay, eraser);
    delayer.setRepeats(false);
    delayer.start();
  }
  
  public static void tableToCSV(String desc, TermTableModel model, PrintStream out) {
    final String comma = ",";
    final String dquote = "\"";
    final String eol = "\n";
    final boolean central = model.getType().equals(TermTableModel.ResultTableType.CENTRALITY);
    out.println("," + dquote +  desc + dquote + comma);
    out.println(",,");
    for (int j = 0 ; j < model.getColumnCount(); j++) {
      out.print(model.getColumnName(j));
      out.print(comma);
    }
    out.print(eol);
    for (int i = 0; i < model.getRowCount(); i++) {
      for (int j = 0 ; j < model.getColumnCount(); j++) {
        if (j == 1 || (j == 6 && central)) out.print(dquote);
        out.print(model.getValueAt(i, j));
        if (j == 1 || (j == 6 && central)) out.print(dquote);
        out.print(comma);
      }
      out.print(eol);
    }
  }


  
  // prefs stuff
  public Preferences getPrefs() {
    return Preferences.userNodeForPackage(uacalcUI.getClass());
  }
  
  public static void scrollToBottom(JTable table) {
    int ht = table.getHeight();
    table.scrollRectToVisible(new Rectangle(0, ht, 0, ht));
    //table.revalidate();
    //table.repaint();
    //table.doLayout();
  }

  /*
  public static void main(String[] args) {
    String inFile = null;
    if (args.length > 0) {
      inFile = args[0];
      if (args.length > 1 && inFile.equals("-open")) inFile = args[1];
      File theFile = new File(inFile);
      try {
      //board = BoardIO.readBoardFile(theFile);
      //boardFileArg = theFile;
      }
      catch (Exception ex) {}
    }

    Runnable  runner = new FrameShower(args);
    EventQueue.invokeLater(runner);
  }
  */



  /*
  private static class FrameShower implements Runnable {
    
    private final String[] args;
    
    private FrameShower(String[] arguments) {
      args = arguments;
    }

    public void run() {
      UACalculator frame = new UACalculator();
      String inFile = null;
      if (args.length > 0) {
        inFile = args[0];
        if (args.length > 1 && inFile.equals("-open")) inFile = args[1];
        frame.open(new File(inFile));
      }
      
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int width = (screenSize.width * 9) / 10;
      int height = (screenSize.height * 9) / 10;
      frame.setLocation((screenSize.width - width) / 2,
                        (screenSize.height - height) / 2);
      frame.setSize(width, height);
      //frame.isDefaultLookAndFeelDecorated();
      frame.setTitle();
      
      
      frame.setVisible(true);
    }
  }
  */

}
