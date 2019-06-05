package org.evosuite.coverage.cbehaviour;

import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;

public class WeightedCommonBehaviourCoverageSuiteFitness extends TestSuiteFitnessFunction {

  @Override
  public double getFitness(AbstractTestSuiteChromosome<? extends ExecutableChromosome> individual) {
    throw new UnsupportedOperationException("Designed for use in MOSA, which does not use suite "
        + "fitness functions");
  }
}
