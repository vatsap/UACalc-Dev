package org.uacalc.fol;

import java.util.HashMap;

import org.uacalc.alg.SmallAlgebra;
import org.uacalc.terms.Variable;

public class Sentence {

  private final FOFormula formula;

  public Sentence(FOFormula formula) {
    if (formula == null) {
      throw new NullPointerException("formula");
    }
    if (!formula.isSentence()) {
      throw new IllegalArgumentException("FOFormula is not closed: " + formula);
    }
    this.formula = formula;
  }

  public FOFormula getFormula() {
    return formula;
  }

  public boolean evaluate(SmallAlgebra alg) {
    return formula.evaluate(alg, new HashMap<Variable,Integer>());
  }

  @Override
  public String toString() {
    return formula.toString();
  }
}