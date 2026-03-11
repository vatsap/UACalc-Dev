/* RelationSymbol.java */

package org.uacalc.alg.rel;

public class RelationSymbol implements Comparable<RelationSymbol> {

  private final String name;
  private final int arity;

  public RelationSymbol(String name, int arity) {
    if (name == null) throw new IllegalArgumentException("name is null");
    if (arity < 0) throw new IllegalArgumentException("arity < 0");
    this.name = name;
    this.arity = arity;
  }

  public String name() { return name; }

  public int arity() { return arity; }

  @Override
  public String toString() {
    return name + "/" + arity;
  }

  @Override
  public int compareTo(RelationSymbol other) {
    int c = name.compareTo(other.name);
    if (c != 0) return c;
    return Integer.compare(arity, other.arity);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof RelationSymbol)) return false;
    RelationSymbol sym = (RelationSymbol) obj;
    return name.equals(sym.name()) && arity == sym.arity();
  }

  @Override
  public int hashCode() {
    return name.hashCode() + 31 * arity;
  }
}
