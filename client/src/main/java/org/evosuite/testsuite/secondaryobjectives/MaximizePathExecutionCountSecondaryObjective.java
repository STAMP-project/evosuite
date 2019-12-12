package org.evosuite.testsuite.secondaryobjectives;

import java.util.Objects;
import java.util.stream.Collectors;
import org.evosuite.ExecutionCountManager;
import org.evosuite.ga.RelativeChangeSecondaryObjective;
import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secondary objective that prefers test suites that execute a path in the CUT that has a larger
 * execution count. Execution counts are provided by an {@link ExecutionCountManager}.
 */
public class MaximizePathExecutionCountSecondaryObjective extends
    RelativeChangeSecondaryObjective<TestSuiteChromosome> {

  private static final Logger logger = LoggerFactory
      .getLogger(
          MaximizePathExecutionCountSecondaryObjective.class);

  /**
   * The manager that is used to determine execution counts for lines of code
   */
  private transient ExecutionCountManager executionCountManager;

  /**
   * Constructs this secondary objective using the given execution count manager
   */
  public MaximizePathExecutionCountSecondaryObjective(ExecutionCountManager executionCountManager) {
    this.executionCountManager = executionCountManager;
  }

  /**
   * Compares two test suite chromosomes, preferring the one that covers the most path weight.
   */
  @Override
  public int compareChromosomes(TestSuiteChromosome chromosome1, TestSuiteChromosome chromosome2) {
    double weight1 = getPathWeight(chromosome1);
    double weight2 = getPathWeight(chromosome2);

    logger.trace("Comparing path weight: " + weight1 + " and " + weight2);
    return Double.compare(weight2, weight1);
  }

  /**
   * Compares two generations of test suite chromosomes, and prefers the generation that contains the
   * test suite that has the largest weight.
   */
  @Override
  public int compareGenerations(TestSuiteChromosome parent1, TestSuiteChromosome parent2,
      TestSuiteChromosome child1, TestSuiteChromosome child2) {
    double weightParent1 = getPathWeight(parent1);
    double weightParent2 = getPathWeight(parent2);
    double weightChild1 = getPathWeight(child1);
    double weightChild2 = getPathWeight(child2);

    logger.trace(
        "Comparing path weight: parents: " + weightParent1 + " and " + weightParent2
            + "; children: " + weightChild1 + " and " + weightChild2);
    return Double
        .compare(Math.max(weightChild1, weightChild2), Math.max(weightParent1, weightParent2));
  }

  /**
   * Computes the weight of the path taken by the given test suite.
   */
  private double getPathWeight(TestSuiteChromosome chromosome) {
    return executionCountManager.avgExecCount(
        chromosome.getTestChromosomes().stream().map(ExecutableChromosome::getLastExecutionResult)
        .filter(Objects::nonNull).flatMap(result -> result.getTrace().getAllCoveredLines().stream())
        .collect(Collectors.toSet())
    );
  }

  @Override
  public double relativeChange(TestSuiteChromosome chromosome1, TestSuiteChromosome chromosome2) {
    double weight1 = getPathWeight(chromosome1);
    double weight2 = getPathWeight(chromosome2);

    logger.trace("Comparing path weights: " + weight1 + " and " + weight2);

    // To prevent division by 0
    if (weight2 == 0) {
      return 10d;
    }
    return weight1 / weight2;
  }
}
