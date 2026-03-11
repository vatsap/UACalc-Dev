/* Relation.java */

package org.uacalc.alg.rel;

public interface Relation {

  RelationSymbol symbol();

  int arity();

  /** Size of the underlying universe (for "concrete" relations on finite algebras). */
  int getSetSize();

  /** True iff the tuple (a0, a1, ..., a_{arity-1}) is in the relation. */
  boolean holds(int... args);

}
