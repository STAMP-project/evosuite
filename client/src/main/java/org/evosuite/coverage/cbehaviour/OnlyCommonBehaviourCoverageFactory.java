package org.evosuite.coverage.cbehaviour;

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class containing logic to supply line coverage goals only for lines that are present in the
 * execution count JSON file.
 */
@SuppressWarnings("WeakerAccess")
public class OnlyCommonBehaviourCoverageFactory extends
    AbstractFitnessFactory<LineCoverageTestFitness> {

  private final LineCoverageFactory delegateFactory;
  private final List<ClassExecutionCounts> executionCounts;

  private static final Logger logger =
      LoggerFactory.getLogger(OnlyCommonBehaviourCoverageFactory.class);

  /**
   * Constructs a new factory using the execution counts in the list that is provided.
   */
  @SuppressWarnings("unused")
  public OnlyCommonBehaviourCoverageFactory(List<ClassExecutionCounts> executionCounts) {
    delegateFactory = new LineCoverageFactory();
    this.executionCounts = executionCounts;
  }

  /**
   * Supplies line coverage goals only for lines that appear in the input execution count JSON file,
   * of which the contents have been provided upon construction of this object.
   */
  @Override
  public List<LineCoverageTestFitness> getCoverageGoals() {
    logger.info("Generating coverage goals for executed lines");
    List<LineCoverageTestFitness> allLines = delegateFactory.getCoverageGoals();
    logger.trace("All lines in class: " + allLines);

    // Filter the list that is provided by the LineCoverageFactory so only lines are retained that
    // appear in the input execution counts.
    // In more low-level terms: for each line goal, it is first checked whether it corresponds to
    // a class present in the execution counts, then whether it corresponds to a method present in
    // the execution counts, and finally whether it corresponds to a line present in the execution
    // counts. If all three are true, the line goal is retained, otherwise it is discarded.
    logger.trace("Using execution counts: " + executionCounts);
    List<LineCoverageTestFitness> executedLines = allLines.stream()
        .filter(line -> executionCounts.stream().anyMatch(
            count -> line.getClassName().equals(count.getClassName()) && count.getMethods().stream()
                .anyMatch(method ->
                    line.getMethod().split(Pattern.quote("("))[0].equals(method.getMethodName())
                        && method.getExecutionCounts().stream()
                        .anyMatch(executionCount ->
                            line.getLine() == executionCount.getLine()
                        )))).collect(Collectors.toList());
    logger.trace("Executed lines: " + executedLines);
    return executedLines;
  }

  public static OnlyCommonBehaviourCoverageFactory fromExecutionCountFile(File file) {
    if (!file.exists()) {
      throw new IllegalArgumentException("Input file does not exist: " + file.getAbsolutePath());
    }
    try {
      return new OnlyCommonBehaviourCoverageFactory(
          ClassExecutionCounts.readCounts(new Scanner(file).useDelimiter("\\Z").next()));
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Just checked if file exists, but not accessible anymore", e);
    } catch (JsonSyntaxException e) {
      logger.error("Execution count file malformed", e);
      throw e;
    }
  }
}
