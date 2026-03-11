package org.uacalc.alg.rel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.uacalc.alg.SmallAlgebra;
import org.uacalc.fol.FOFormula;
import org.uacalc.terms.Variable;
import org.uacalc.terms.VariableImp;

/**
 * Concrete implementation of an abstract relation on a specific finite algebra.
 *
 * This class is intentionally minimalistic:
 *   - it remembers which abstract relation it implements
 *   - it remembers on which algebra it is implemented
 *   - it stores the actual extension of the relation as a set of tuples
 *
 * The tuple set is computed at construction time by evaluating the
 * abstract relation's FOFormula on all tuples of the algebra.
 */
public final class RelationImp implements Relation {

  private final AbstractRelation abstractRelation;
  private final SmallAlgebra algebra;
  private final Set<Tuple> tuples;

  /**
   * Immutable tuple wrapper so tuples can be stored in a Set using value equality.
   */
  public static final class Tuple {
    private final int[] entries;

    public Tuple(int... entries) {
      if (entries == null) {
        throw new IllegalArgumentException("entries is null");
      }
      this.entries = Arrays.copyOf(entries, entries.length);
    }

    public int arity() {
      return entries.length;
    }

    public int get(int i) {
      return entries[i];
    }

    public int[] toArray() {
      return Arrays.copyOf(entries, entries.length);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof Tuple)) return false;
      Tuple other = (Tuple) obj;
      return Arrays.equals(entries, other.entries);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(entries);
    }

    @Override
    public String toString() {
      return Arrays.toString(entries);
    }
  }

  /**
   * Construct the concrete implementation by evaluating the abstract relation's
   * formula on all tuples of the algebra.
   */
  public RelationImp(AbstractRelation abstractRelation, SmallAlgebra algebra) {
    if (abstractRelation == null) {
      throw new IllegalArgumentException("abstractRelation is null");
    }
    if (algebra == null) {
      throw new IllegalArgumentException("algebra is null");
    }
    if (abstractRelation.formula() == null) {
      throw new IllegalArgumentException("abstractRelation.formula() is null");
    }

    this.abstractRelation = abstractRelation;
    this.algebra = algebra;
    this.tuples = Collections.unmodifiableSet(buildTupleSet(abstractRelation, algebra));
  }

  public AbstractRelation abstractRelation() {
    return abstractRelation;
  }

  public SmallAlgebra algebra() {
    return algebra;
  }

  /**
   * Returns the stored extension of the relation.
   */
  public Set<Tuple> tuples() {
    return tuples;
  }

  @Override
  public RelationSymbol symbol() {
    return abstractRelation.symbol();
  }

  @Override
  public int arity() {
    return abstractRelation.arity();
  }

  @Override
  public int getSetSize() {
    return algebra.cardinality();
  }

  @Override
  public boolean holds(int... args) {
    validateTuple(args);
    return tuples.contains(new Tuple(args));
  }

  /**
   * Recomputes the tuples from the current abstract relation formula
   * and returns a fresh RelationImp.
   */
  public RelationImp rebuild() {
    return new RelationImp(abstractRelation, algebra);
  }

  private Set<Tuple> buildTupleSet(AbstractRelation abstractRelation, SmallAlgebra algebra) {
    final Set<Tuple> result = new LinkedHashSet<Tuple>();
    final int arity = abstractRelation.arity();
    final int size = algebra.cardinality();
    final FOFormula formula = abstractRelation.formula();

    if (arity == 0) {
      if (formula.evaluate(algebra, Collections.<Variable,Integer>emptyMap())) {
        result.add(new Tuple());
      }
      return result;
    }

    int[] current = new int[arity];
    fillTuplesRecursively(result, current, 0, size, formula, algebra);
    return result;
  }

  private void fillTuplesRecursively(Set<Tuple> result,
                                     int[] current,
                                     int depth,
                                     int size,
                                     FOFormula formula,
                                     SmallAlgebra algebra) {
    if (depth == current.length) {
      if (evaluateTuple(current, formula, algebra)) {
        result.add(new Tuple(current));
      }
      return;
    }

    for (int a = 0; a < size; a++) {
      current[depth] = a;
      fillTuplesRecursively(result, current, depth + 1, size, formula, algebra);
    }
  }

  private boolean evaluateTuple(int[] tuple, FOFormula formula, SmallAlgebra algebra) {
    Map<Variable,Integer> assignment = new HashMap<Variable,Integer>();

    for (int i = 0; i < tuple.length; i++) {
      assignment.put(defaultVariable(i), Integer.valueOf(tuple[i]));
    }

    return formula.evaluate(algebra, assignment);
  }

  private void validateTuple(int[] args) {
    if (args == null) {
      throw new IllegalArgumentException("args is null");
    }

    if (args.length != arity()) {
      throw new IllegalArgumentException(
          "Wrong arity: expected " + arity() + ", got " + args.length);
    }

    final int n = algebra.cardinality();
    for (int i = 0; i < args.length; i++) {
      if (args[i] < 0 || args[i] >= n) {
        throw new IllegalArgumentException(
            "Argument out of universe range: " + args[i] + " not in [0," + (n - 1) + "]");
      }
    }
  }

  /**
   * Canonical variable names used for relation arguments.
   *
   * index 0 -> x
   * index 1 -> y
   * index 2 -> z
   * index 3 -> u1
   * index 4 -> u2
   * ...
   */
  public static Variable defaultVariable(int index) {
    switch (index) {
      case 0: return Variable.x;
      case 1: return Variable.y;
      case 2: return Variable.z;
      default: return new VariableImp("u" + (index - 2));
    }
  }

  @Override
  public String toString() {
    return "RelationImp[" + symbol()
        + ", alg=" + algebra.getName()
        + ", tuples=" + tuples.size() + "]";
  }
}