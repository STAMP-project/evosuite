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
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

public class WeightedCommonBehaviourCoverageTestFitness extends TestFitnessFunction {

  private List<ClassExecutionCounts> executionCounts;
  private List<LineCoverageTestFitness> lineGoals;

  /**
   * Indicates whether the fitness function should favor common or uncommon behaviours.
   */
  private boolean forCommonBehaviours;

  /**
   * Constructs this fitness function with the given execution counts and factory for line coverage
   * goals. It will favor common behaviours.
   *
   * @param executionCounts the execution counts containing counts for the class under test
   */
  public WeightedCommonBehaviourCoverageTestFitness(List<ClassExecutionCounts> executionCounts,
      LineCoverageFactory lineFactory) {
    this(executionCounts, lineFactory, true);
  }

  /**
   * Constructs this fitness function with the given execution counts and factory for line coverage
   * goals. It can be constructed for favoring either common or uncommon behaviours.
   *
   * @param executionCounts the execution counts containing counts for the class under test
   * @param forCommonBehaviours {@code true} if the function should favor common behaviours, {@code
   * false} if it should favor uncommon behaviours
   */
  public WeightedCommonBehaviourCoverageTestFitness(List<ClassExecutionCounts> executionCounts,
      LineCoverageFactory lineFactory, boolean forCommonBehaviours) {
    if (executionCounts == null) {
      throw new IllegalArgumentException("executionCounts parameter must be non-null");
    }
    if (lineFactory == null) {
      throw new IllegalArgumentException("lineFactory parameter must be non-null");
    }

    this.executionCounts = executionCounts;
    this.lineGoals =
        CommonBehaviourUtil.retainExecutedLines(lineFactory.getCoverageGoals(), executionCounts);
    this.forCommonBehaviours = forCommonBehaviours;
  }

  @Override
  public double getFitness(TestChromosome individual, ExecutionResult result) {
    return lineGoals.stream().mapToDouble(goal -> goal.getFitness(individual, result) *
        getExecutionCount(goal)).sum()
        + Double.MIN_VALUE; //This is added so the goal is never satisfied, which is most often
    // what one wants when using this goal.
  }

  private int getExecutionCount(LineCoverageTestFitness goal) {
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
    WeightedCommonBehaviourCoverageTestFitness that = (WeightedCommonBehaviourCoverageTestFitness) o;
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

  @Override
  public boolean isMaximizationFunction() {
    return !forCommonBehaviours;
  }

  /**
   * Creates an instance of this fitness function using the execution count file specified. Common
   * behaviours will be favored.
   * @param file an execution count file. It must exist.
   */
  public static WeightedCommonBehaviourCoverageTestFitness fromExecutionCountFile(File file) {
    return WeightedCommonBehaviourCoverageTestFitness.fromExecutionCountFile(file, true);
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
  public static WeightedCommonBehaviourCoverageTestFitness fromExecutionCountFile(File file,
      boolean forCommonBehaviours) {
    if (!file.exists()) {
      throw new IllegalArgumentException("Input file does not exist: " + file.getAbsolutePath());
    }
    try {
      return new WeightedCommonBehaviourCoverageTestFitness(
          ClassExecutionCounts.readCounts(new Scanner(file).useDelimiter("\\Z").next()),
          new LineCoverageFactory(), forCommonBehaviours);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Just checked if file exists, but not accessible anymore", e);
    } catch (JsonSyntaxException e) {
      logger.error("Execution count file malformed", e);
      throw new RuntimeException(e);
    }
  }
}
