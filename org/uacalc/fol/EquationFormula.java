package org.uacalc.fol;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.uacalc.alg.SmallAlgebra;
import org.uacalc.eq.Equation;
import org.uacalc.terms.Term;
import org.uacalc.terms.Variable;

public class EquationFormula implements FOFormula {

  private final Equation equation;

  public EquationFormula(Equation equation) {
    if (equation == null) {
      throw new NullPointerException("equation");
    }
    this.equation = equation;
  }

  public EquationFormula(Term left, Term right) {
    this(new Equation(left, right));
  }

  public Equation getEquation() {
    return equation;
  }

  public Term leftSide() {
    return equation.leftSide();
  }

  public Term rightSide() {
    return equation.rightSide();
  }

  public boolean evaluate(SmallAlgebra alg, Map<Variable,Integer> assignment) {
    return leftSide().intEval(alg, assignment) == rightSide().intEval(alg, assignment);
  }

  public Set<Variable> getVariables() {
    return new LinkedHashSet<Variable>(equation.getVariableList());
  }

  public StringBuffer writeStringBuffer(StringBuffer sb) {
    leftSide().writeStringBuffer(sb);
    sb.append(" = ");
    rightSide().writeStringBuffer(sb);
    return sb;
  }

  public String toString() {
    return writeStringBuffer(new StringBuffer()).toString();
  }
}