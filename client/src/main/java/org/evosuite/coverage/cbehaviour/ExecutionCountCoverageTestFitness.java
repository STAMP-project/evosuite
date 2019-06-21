package org.evosuite.coverage.cbehaviour;

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.testcase.TestFitnessFunction;

/**
 * Fitness function that looks values test cases based on the execution count of the code they
 * exercise. It can be configured to either value test cases that exercise more often executed code,
 * or those that exercise less often executed code.
 */
public abstract class ExecutionCountCoverageTestFitness extends TestFitnessFunction {

  protected List<ClassExecutionCounts> executionCounts;

  /**
   * Contains line goals only for lines that appear in the list of execution counts.
   */
  protected List<LineCoverageTestFitness> lineGoals;

  /**
   * Constructs this fitness function using the given list of execution counts and the given line
   * goals factory.
   */
  protected ExecutionCountCoverageTestFitness(
      List<ClassExecutionCounts> executionCounts, LineCoverageFactory lineFactory) {
    if (executionCounts == null) {
      throw new IllegalArgumentException("executionCounts parameter must be non-null");
    }
    if (lineFactory == null) {
      throw new IllegalArgumentException("lineFactory parameter must be non-null");
    }
    this.executionCounts = executionCounts;
    this.lineGoals =
        CommonBehaviourUtil.retainExecutedLines(lineFactory.getCoverageGoals(), executionCounts);
  }

  /**
   * Creates an instance of this fitness function using the execution count file specified. Common
   * behaviours will be favored.
   *
   * @param file an execution count file. It must exist.
   */
  public static ExecutionCountCoverageTestFitness fromExecutionCountFile(File file) {
    return ExecutionCountCoverageTestFitness.fromExecutionCountFile(file, true);
  }

  /**
   * Creates an instance of this fitness function using the execution count file specified. It will
   * favor either common or uncommon behaviours depending on the value of {@code
   * forCommonBehaviours}.
   *
   * @param file an execution count file. It must exist.
   * @param forCommonBehaviours {@code true} to favor common behaviours, {@code false} to favor
   * uncommon behaviours.
   */
  public static ExecutionCountCoverageTestFitness fromExecutionCountFile(File file,
      boolean forCommonBehaviours) {
    if (!file.exists()) {
      throw new IllegalArgumentException("Input file does not exist: " + file.getAbsolutePath());
    }
    try {
      List<ClassExecutionCounts> executionCounts = ClassExecutionCounts
          .readCounts(new Scanner(file).useDelimiter("\\Z").next());
      if (forCommonBehaviours) {
        return new HighExecutionCountCoverageTestFitness(
            executionCounts, new LineCoverageFactory());
      }
      return new LowExecutionCountCoverageTestFitness(
          executionCounts, new LineCoverageFactory());
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Just checked if file exists, but not accessible anymore", e);
    } catch (JsonSyntaxException e) {
      logger.error("Execution count file malformed", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the execution count for the speicified line as determined by the execution count field.
   */
  protected int getExecutionCount(LineCoverageTestFitness goal) {
    return executionCounts.stream().filter(count ->
        count.getClassName().equals(goal.getClassName()))
        .findAny().get()
        .getMethods().stream().filter(method ->
            method.getMethodName().equals(goal.getMethod().split(
                Pattern.quote("("))[0]))
        .findAny().get()
        .getExecutionCounts().stream().filter(line -> line.getLine() == goal.getLine())
        .findAny().get().getCount();
  }

  @Override
  public int compareTo(TestFitnessFunction other) {
    return compareClassName(other);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExecutionCountCoverageTestFitness that = (ExecutionCountCoverageTestFitness) o;
    return executionCounts.equals(that.executionCounts) &&
        lineGoals.equals(that.lineGoals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(executionCounts, lineGoals);
  }

  @Override
  public String getTargetClass() {
    return lineGoals.get(0).getTargetClass();
  }

  @Override
  public String getTargetMethod() {
    return lineGoals.get(0).getTargetMethod();
  }
}
