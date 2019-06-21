package org.evosuite.testsuite.secondaryobjectives;

import org.evosuite.Properties;
import org.evosuite.ga.SecondaryObjective;
import org.evosuite.testsuite.TestSuiteChromosome;

/**
 * Dummy secondary objective for test suites, which can be used in places where EvoSuite requires
 * one, but none is implemented or none is necessary. The methods of this class should not be called
 * and throw {@link UnsupportedOperationException}s when they are called anyway.
 */
public class DummySecondaryObjective extends
    SecondaryObjective<TestSuiteChromosome> {

  /**
   * The secondary objective for which this dummy has been created.
   */
  private final Properties.SecondaryObjective objective;

  /**
   * The reason for which an actual secondary objective has not been implemented.
   */
  private final String reason;

  /**
   * Constructs this dummy objective for the specified objective and reason.
   */
  public DummySecondaryObjective(Properties.SecondaryObjective objective, String reason) {
    this.objective = objective;
    this.reason = reason;
  }

  /**
   * Do not call. See class documentation.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public int compareChromosomes(TestSuiteChromosome chromosome1, TestSuiteChromosome chromosome2) {
    throw new UnsupportedOperationException("Secondary objective: " + objective + " is not "
        + "implemented for whole suit generation. Reason: " + reason);
  }

  /**
   * Do not call. See class documentation.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public int compareGenerations(TestSuiteChromosome parent1, TestSuiteChromosome parent2,
      TestSuiteChromosome child1, TestSuiteChromosome child2) {
    throw new UnsupportedOperationException("Secondary objective: " + objective + " is not "
        + "implemented for whole suit generation. Reason: " + reason);
  }
}
