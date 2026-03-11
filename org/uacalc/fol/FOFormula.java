package org.uacalc.fol;

import java.util.Map;
import java.util.Set;

import org.uacalc.alg.SmallAlgebra;
import org.uacalc.terms.Variable;

public interface FOFormula {

  boolean evaluate(SmallAlgebra alg, Map<Variable,Integer> assignment);

  Set<Variable> getVariables();

  default boolean isSentence() {
    return getVariables().isEmpty();
  }

  StringBuffer writeStringBuffer(StringBuffer sb);

}