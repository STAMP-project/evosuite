package org.evosuite.coverage.execcount;

import java.util.List;
import org.evosuite.ExecutionCountManager;
import org.evosuite.Properties.Criterion;
import org.evosuite.coverage.FitnessFunctions;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;

/**
 * An {@link ExecutionCountCoverageTestFitness} that prefers test suites that do not exercise
 * commonly executed code.
 */
public class MinExecutionCountCoverageSuiteFitness extends ExecutionCountCoverageSuiteFitness {
  private List<? extends TestFitnessFunction> goals;

  /**
   * Constructs this fitness function with the given execution count manager and factory for line
   * coverage goals.
   */
  public MinExecutionCountCoverageSuiteFitness() {
    goals = FitnessFunctions.getFitnessFactory(Criterion.MIN_EXEC_COUNT).getCoverageGoals();
  }

  @Override
  public double getFitness(AbstractTestSuiteChromosome<? extends ExecutableChromosome> individual) {
    runTestSuite(individual);
    double fitnessValueSum = 0d;
    for (ExecutableChromosome testCase : individual.getTestChromosomes()) {
      fitnessValueSum += goals.get(0).getFitness((TestChromosome) testCase);
    }
    double fitness = fitnessValueSum / individual.getTestChromosomes().size();
    individual.setCoverage(this, 1 - fitness);
    return fitness;
  }
}
