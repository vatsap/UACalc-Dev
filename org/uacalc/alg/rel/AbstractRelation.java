package org.uacalc.alg.rel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.uacalc.alg.SmallAlgebra;
import org.uacalc.fol.FOFormula;

/**
 * Algebra-independent abstract relation definition.
 *
 * This is the saved abstract definition of a relation:
 *   - symbol (name + arity)
 *   - parsed first-order formula
 *
 * It may have several concrete implementations, one for each algebra.
 */
public final class AbstractRelation {

  private final UUID id;
  private RelationSymbol symbol;
  private FOFormula formula;
  private String definitionText;

  /**
   * Concrete implementations of this abstract relation, indexed by algebra.
   *
   * IdentityHashMap is intentional: algebras are typically tracked by object
   * identity in the UI/controller layer.
   */
  private final Map<SmallAlgebra, RelationImp> implementations =
      new IdentityHashMap<SmallAlgebra, RelationImp>();

  public AbstractRelation(RelationSymbol symbol, FOFormula formula, String definitionText) {
    this(UUID.randomUUID(), symbol, formula, definitionText);
  }

  public AbstractRelation(UUID id, RelationSymbol symbol, FOFormula formula, String definitionText) {
    if (id == null) throw new IllegalArgumentException("id is null");
    if (symbol == null) throw new IllegalArgumentException("symbol is null");
    if (formula == null) throw new IllegalArgumentException("formula is null");
    if (definitionText == null) throw new IllegalArgumentException("definitionText is null");

    this.id = id;
    this.symbol = symbol;
    this.formula = formula;
    this.definitionText = definitionText;
  }

  public UUID id() {
    return id;
  }

  public RelationSymbol symbol() {
    return symbol;
  }

  public int arity() {
    return symbol.arity();
  }

  public String name() {
    return symbol.name();
  }

  public FOFormula formula() {
    return formula;
  }

  /**
   * Replace the relation symbol.
   * Useful when the user edits the name / arity and clicks Save.
   */
  public void setSymbol(RelationSymbol symbol) {
    if (symbol == null) {
      throw new IllegalArgumentException("symbol is null");
    }
    this.symbol = symbol;
  }

  /**
   * Replace the parsed first-order formula.
   */
  public void setFormula(FOFormula formula) {
    if (formula == null) {
      throw new IllegalArgumentException("formula is null");
    }
    this.formula = formula;
  }

  public String definitionText() {
    return definitionText;
  }

  public void setDefinitionText(String definitionText) {
    if (definitionText == null) {
      throw new IllegalArgumentException("definitionText is null");
    }
    this.definitionText = definitionText;
  }

  /**
   * Convenience method for editing the whole abstract definition at once.
   */

  public void redefine(RelationSymbol symbol, FOFormula formula, String definitionText) {
    setSymbol(symbol);
    setFormula(formula);
    setDefinitionText(definitionText);
  }

  /**
   * Register a concrete implementation of this abstract relation on an algebra.
   *
   * If there already is an implementation for the same algebra, it is replaced.
   */
  public void addImplementation(RelationImp imp) {
    if (imp == null) {
      throw new IllegalArgumentException("imp is null");
    }
    if (imp.abstractRelation() != this) {
      throw new IllegalArgumentException(
          "Implementation does not belong to this AbstractRelation");
    }
    implementations.put(imp.algebra(), imp);
  }

  /**
   * Remove the implementation on the given algebra, if any.
   */
  public RelationImp removeImplementation(SmallAlgebra algebra) {
    if (algebra == null) {
      throw new IllegalArgumentException("algebra is null");
    }
    return implementations.remove(algebra);
  }

  /**
   * Remove all implementations.
   */
  public void clearImplementations() {
    implementations.clear();
  }

  /**
   * Returns the implementation on the given algebra, or null if there is none.
   */
  public RelationImp getImplementation(SmallAlgebra algebra) {
    if (algebra == null) {
      throw new IllegalArgumentException("algebra is null");
    }
    return implementations.get(algebra);
  }

  public boolean isImplementedOn(SmallAlgebra algebra) {
    if (algebra == null) {
      throw new IllegalArgumentException("algebra is null");
    }
    return implementations.containsKey(algebra);
  }

  public int implementationCount() {
    return implementations.size();
  }

  /**
   * Returns all concrete implementations of this abstract relation.
   */
  public List<RelationImp> getImplementations() {
    return Collections.unmodifiableList(
        new ArrayList<RelationImp>(implementations.values()));
  }

  /**
   * Returns the algebras on which this relation is currently implemented.
   */
  public List<SmallAlgebra> getImplementedAlgebras() {
    return Collections.unmodifiableList(
        new ArrayList<SmallAlgebra>(implementations.keySet()));
  }

  @Override
  public String toString() {
    return "AbstractRelation[" + symbol
        + ", implementations=" + implementations.size() + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof AbstractRelation)) return false;
    AbstractRelation other = (AbstractRelation) obj;
    return id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}