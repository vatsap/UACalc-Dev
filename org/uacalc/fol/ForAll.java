package org.uacalc.fol;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.uacalc.alg.SmallAlgebra;
import org.uacalc.terms.Variable;

public class ForAll implements FOFormula {

  private final Variable variable;
  private final FOFormula formula;

  public ForAll(Variable variable, FOFormula formula) {
    if (variable == null || formula == null) {
      throw new NullPointerException();
    }
    this.variable = variable;
    this.formula = formula;
  }

  public Variable variable() {
    return variable;
  }

  public FOFormula formula() {
    return formula;
  }

  public boolean evaluate(SmallAlgebra alg, Map<Variable,Integer> assignment) {
    Integer oldValue = assignment.get(variable);
    boolean hadOldValue = assignment.containsKey(variable);
    try {
      for (int i = 0; i < alg.cardinality(); i++) {
        assignment.put(variable, Integer.valueOf(i));
        if (!formula.evaluate(alg, assignment)) {
          return false;
        }
      }
      return true;
    }
    finally {
      if (hadOldValue) {
        assignment.put(variable, oldValue);
      }
      else {
        assignment.remove(variable);
      }
    }
  }

  public Set<Variable> getVariables() {
    LinkedHashSet<Variable> ans = new LinkedHashSet<Variable>(formula.getVariables());
    ans.remove(variable);
    return ans;
  }

  public StringBuffer writeStringBuffer(StringBuffer sb) {
    sb.append("forall ");
    sb.append(variable.getName());
    sb.append(" (");
    formula.writeStringBuffer(sb);
    sb.append(")");
    return sb;
  }

  public String toString() {
    return writeStringBuffer(new StringBuffer()).toString();
  }
}