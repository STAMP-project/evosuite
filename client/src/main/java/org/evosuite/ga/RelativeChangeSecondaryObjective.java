package org.evosuite.ga;

/**
 * A {@link SecondaryObjective} that can compare chromosomes relative to each other quantitatively.
 *
 * @param <T> the type of chromosome that can be compared by this secondary objective
 */
public abstract class RelativeChangeSecondaryObjective<T extends Chromosome> extends
    SecondaryObjective<T> {

  /**
   * Compares two chromosomes relative to each other quantitatively. Returns a value that specifies
   * how good the first supplied chromosome is compared to the second in terms of this objective.
   *
   * @return a double value specifying how desirable it is to keep the first supplied chromosome
   * compared to the second. The return value should be interpreted as follows: the first chromosome
   * is {@code return value} times as desirable to keep as the second chromosome. E.g., a return
   * value of 2.0 would mean that the first chromosome paramter is twice as desirable to keep as the
   * second. A return value of 0.5 would mean the reverse.
   */
  public abstract double relativeChange(T chromosome1, T chromosome2);

}
