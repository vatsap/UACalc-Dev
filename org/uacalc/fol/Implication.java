package org.uacalc.fol;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.uacalc.alg.SmallAlgebra;
import org.uacalc.terms.Variable;

public class Implication implements FOFormula {

  private final FOFormula left;
  private final FOFormula right;

  public Implication(FOFormula left, FOFormula right) {
    if (left == null || right == null) {
      throw new NullPointerException();
    }
    this.left = left;
    this.right = right;
  }

  public FOFormula left() {
    return left;
  }

  public FOFormula right() {
    return right;
  }

  public boolean evaluate(SmallAlgebra alg, Map<Variable,Integer> assignment) {
    return !left.evaluate(alg, assignment) || right.evaluate(alg, assignment);
  }

  public Set<Variable> getVariables() {
    LinkedHashSet<Variable> ans = new LinkedHashSet<Variable>(left.getVariables());
    ans.addAll(right.getVariables());
    return ans;
  }

  public StringBuffer writeStringBuffer(StringBuffer sb) {
    sb.append("(");
    left.writeStringBuffer(sb);
    sb.append(" -> ");
    right.writeStringBuffer(sb);
    sb.append(")");
    return sb;
  }

  public String toString() {
    return writeStringBuffer(new StringBuffer()).toString();
  }
}