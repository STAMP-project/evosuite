package org.evosuite.testcase.secondaryobjectives;

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import org.evosuite.Properties;
import org.evosuite.coverage.execcount.ClassExecutionCounts;
import org.evosuite.ga.SecondaryObjective;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secondary objective that prefers test cases that execute a path in the SUT that has a larger
 * execution count. The execution count can for example be retrieved from a file, and has to be in
 * the format of the {@link org.evosuite.coverage.execcount.ClassExecutionCounts} class.
 */
public class MaximizePathExecutionCountSecondaryObjective extends
    SecondaryObjective<TestChromosome> {

  private static final Logger logger = LoggerFactory
      .getLogger(MaximizePathExecutionCountSecondaryObjective.class);
  private ClassExecutionCounts executionCounts;

  /**
   * Constructs this secondary objective using the given execution counts.
   */
  public MaximizePathExecutionCountSecondaryObjective(ClassExecutionCounts executionCounts) {
    this.executionCounts = executionCounts;
  }

  /**
   * Compares two test case chromosomes, preferring the one that covers the most execution count
   * weight.
   */
  @Override
  public int compareChromosomes(TestChromosome chromosome1, TestChromosome chromosome2) {
    int weight1 = getPathWeight(chromosome1);
    int weight2 = getPathWeight(chromosome2);

    logger.debug("Comparing execution count weight: " + weight1 + " and " + weight2);
    return weight2 - weight1;
  }

  /**
   * Compares two generations of test case chromosomes, and prefers the generation that contains the
   * test case that has the largest weight.
   */
  @Override
  public int compareGenerations(TestChromosome parent1, TestChromosome parent2,
      TestChromosome child1, TestChromosome child2) {
    int weightParent1 = getPathWeight(parent1);
    int weightParent2 = getPathWeight(parent2);
    int weightChild1 = getPathWeight(child1);
    int weightChild2 = getPathWeight(child2);

    logger.debug(
        "Comparing execution count weight: parents: " + weightParent1 + " and " + weightParent2
            + "; children: " + weightChild1 + " and " + weightChild2);
    return Math.max(weightChild1, weightChild2) - Math.max(weightParent1, weightParent2);
  }

  /**
   * Computes the weight of the path taken by the given test case.
   */
  private int getPathWeight(TestChromosome chromosome) {
    ExecutionResult executionResult = chromosome.getLastExecutionResult();
    if (executionResult == null) {
      logger.debug("No execution result available. Using path weight of 0.");
      return 0;
    }

    return executionCounts.numberOfExecutions(
        executionResult.getTrace().getCoveredLines());
  }

  /**
   * Constructs this secondary objective using the execution counts from the given file.
   *
   * @param file a file containing execution counts. The file must exist.
   */
  public static MaximizePathExecutionCountSecondaryObjective fromExecutionCountFile(File file) {
    if (!file.exists()) {
      throw new IllegalArgumentException("Input file does not exist: " + file.getAbsolutePath());
    }
    try {
      return new MaximizePathExecutionCountSecondaryObjective(
          ClassExecutionCounts.readCounts(new Scanner(file).useDelimiter("\\Z").next())
              .stream().filter(counts -> counts.getClassName().equals(Properties.TARGET_CLASS))
              .findAny().orElseGet(() -> {
            logger.warn("No execution count information found for " + Properties.TARGET_CLASS
                + ". Using empty execution counts.");
            return new ClassExecutionCounts(Properties.TARGET_CLASS);
          }));
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Just checked if file exists, but not accessible anymore", e);
    } catch (JsonSyntaxException e) {
      logger.error("Execution count file malformed", e);
      throw new JsonSyntaxException(e);
    }
  }
}
