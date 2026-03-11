package org.uacalc.fol;

import java.util.Map;
import java.util.Set;

import org.uacalc.alg.SmallAlgebra;
import org.uacalc.terms.Variable;

public class Negation implements FOFormula {

  private final FOFormula formula;

  public Negation(FOFormula formula) {
    if (formula == null) {
      throw new NullPointerException("formula");
    }
    this.formula = formula;
  }

  public FOFormula getFormula() {
    return formula;
  }

  public boolean evaluate(SmallAlgebra alg, Map<Variable,Integer> assignment) {
    return !formula.evaluate(alg, assignment);
  }

  public Set<Variable> getVariables() {
    return formula.getVariables();
  }

  public StringBuffer writeStringBuffer(StringBuffer sb) {
    sb.append("!(");
    formula.writeStringBuffer(sb);
    sb.append(")");
    return sb;
  }

  @Override
  public String toString() {
    return writeStringBuffer(new StringBuffer()).toString();
  }
}