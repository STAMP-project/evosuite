package org.evosuite.testcase.secondaryobjectives;

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import org.evosuite.Properties;
import org.evosuite.coverage.execcount.ClassExecutionCounts;
import org.evosuite.ga.SecondaryObjective;
import org.evosuite.testcase.TestChromosome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secondary objective that prefers test cases that execute a path in the SUT that has a smaller
 * execution count. The execution count can for example be retrieved from a file, and has to be in
 * the format of the {@link ClassExecutionCounts} class.
 */
public class MinimizePathExecutionCountSecondaryObjective extends
    SecondaryObjective<TestChromosome> {

  private static final Logger logger = LoggerFactory
      .getLogger(MinimizePathExecutionCountSecondaryObjective.class);
  private ClassExecutionCounts executionCounts;

  /**
   * Constructs this secondary objective using the given execution counts.
   */
  public MinimizePathExecutionCountSecondaryObjective(ClassExecutionCounts executionCounts) {
    this.executionCounts = executionCounts;
  }

  /**
   * Compares two test case chromosomes, preferring the one that covers the least execution count
   * weight.
   */
  @Override
  public int compareChromosomes(TestChromosome chromosome1, TestChromosome chromosome2) {
    int weight1 = getPathWeight(chromosome1);
    int weight2 = getPathWeight(chromosome2);

    logger.debug("Comparing execution count weight: " + weight1 + " and " + weight2);
    return weight1 - weight2;
  }

  /**
   * Compares two generations of test case chromosomes, and prefers the generation that contains the
   * test case that has the least weight.
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
    return Math.min(weightParent1, weightParent2) - Math.min(weightChild1, weightChild2);
  }

  /**
   * Computes the weight of the path taken by the given test case.
   */
  private int getPathWeight(TestChromosome chromosome) {
    return executionCounts.numberOfExecutions(
        chromosome.getLastExecutionResult().getTrace().getCoveredLines());
  }

  /**
   * Constructs this secondary objective using the execution counts from the given file.
   *
   * @param file a file containing execution counts. The file must exist.
   */
  public static MinimizePathExecutionCountSecondaryObjective fromExecutionCountFile(File file) {
    if (!file.exists()) {
      throw new IllegalArgumentException("Input file does not exist: " + file.getAbsolutePath());
    }
    try {
      return new MinimizePathExecutionCountSecondaryObjective(
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
