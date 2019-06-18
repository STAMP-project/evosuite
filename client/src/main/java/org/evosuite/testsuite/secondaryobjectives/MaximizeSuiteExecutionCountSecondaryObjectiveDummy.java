package org.evosuite.testsuite.secondaryobjectives;

import org.evosuite.ga.SecondaryObjective;
import org.evosuite.testsuite.TestSuiteChromosome;

/**
 * Dummy secondary objective for test suites for the {@code MAX_EXEC_COUNT} secondary objective.
 * This class is not intended to be used, because the {@code MAX_EXEC_COUNT} secondary objective is
 * intended for use in MOSA only, which does not use secondary objectives for whole test suites. It
 * is still defined because EvoSuite requires a class for whole test suites to be present.
 */
public class MaximizeSuiteExecutionCountSecondaryObjectiveDummy extends
    SecondaryObjective<TestSuiteChromosome> {

  /**
   * Do not call. See class documentation.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public int compareChromosomes(TestSuiteChromosome chromosome1, TestSuiteChromosome chromosome2) {
    throw new UnsupportedOperationException("This secondary objective is intended for use with "
        + "MOSA, which does not use secondary objective for whole suites");
  }

  /**
   * Do not call. See class documentation.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public int compareGenerations(TestSuiteChromosome parent1, TestSuiteChromosome parent2,
      TestSuiteChromosome child1, TestSuiteChromosome child2) {
    throw new UnsupportedOperationException("This secondary objective is intended for use with "
        + "MOSA, which does not use secondary objective for whole suites");
  }
}
