package org.evosuite.testcase.secondaryobjectives;

import org.evosuite.ExecutionCountManager;
import org.evosuite.ga.RelativeChangeSecondaryObjective;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secondary objective that prefers test cases that execute a path in the CUT that has a larger
 * execution count. Execution counts are provided by an {@link ExecutionCountManager}.
 */
public class MaximizePathExecutionCountSecondaryObjective extends
    RelativeChangeSecondaryObjective<TestChromosome> {

  private static final Logger logger = LoggerFactory
      .getLogger(MaximizePathExecutionCountSecondaryObjective.class);

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
   * Compares two test case chromosomes, preferring the one that covers the most path weight.
   */
  @Override
  public int compareChromosomes(TestChromosome chromosome1, TestChromosome chromosome2) {
    double weight1 = getPathWeight(chromosome1);
    double weight2 = getPathWeight(chromosome2);

    logger.trace("Comparing path weight: " + weight1 + " and " + weight2);
    return Double.compare(weight2, weight1);
  }

  /**
   * Compares two generations of test case chromosomes, and prefers the generation that contains the
   * test case that has the largest weight.
   */
  @Override
  public int compareGenerations(TestChromosome parent1, TestChromosome parent2,
      TestChromosome child1, TestChromosome child2) {
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
   * Computes the weight of the path taken by the given test case.
   */
  private double getPathWeight(TestChromosome chromosome) {
    ExecutionResult executionResult = chromosome.getLastExecutionResult();
    if (executionResult == null) {
      logger.debug("No execution result available. Using path weight of 0.");
      return 0;
    }

    return executionCountManager.avgExecCount(executionResult.getTrace().getCoveredLines());
  }

  @Override
  public double relativeChange(TestChromosome chromosome1, TestChromosome chromosome2) {
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
