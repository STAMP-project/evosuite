package org.evosuite.coverage;

import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;

/**
 * Dummy fitness function for test suites, which can be used in places where EvoSuite requires one,
 * but none is implemented or none is necessary. The methods of this class should not be called and
 * throw {@link UnsupportedOperationException}s when they are called anyway.
 */
public class DummySuiteFitness extends TestSuiteFitnessFunction {

  /**
   * The reason why a suite fitness function is not implemented.
   */
  private final String reason;

  /**
   * Instantiates this dummy with the provided reason why an implementation is not provided.
   */
  public DummySuiteFitness(String reason) {
    this.reason = reason;
  }

  /**
   * Do not call. See class documentation.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public double getFitness(AbstractTestSuiteChromosome<? extends ExecutableChromosome> individual) {
    throw new UnsupportedOperationException("This criterion does not support whole suite fitness "
        + "functions, for reason: " + reason);
  }
}
