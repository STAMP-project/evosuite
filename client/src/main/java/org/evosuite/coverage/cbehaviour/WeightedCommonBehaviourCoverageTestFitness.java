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

  public WeightedCommonBehaviourCoverageTestFitness(List<ClassExecutionCounts> executionCounts,
      LineCoverageFactory lineFactory) {
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

  @Override
  public double getFitness(TestChromosome individual, ExecutionResult result) {
    return lineGoals.stream().mapToDouble(goal -> goal.getFitness(individual, result) *
        getExecutionCount(goal)).sum();
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

  public static WeightedCommonBehaviourCoverageTestFitness fromExecutionCountFile(File file) {
    if (!file.exists()) {
      throw new IllegalArgumentException("Input file does not exist: " + file.getAbsolutePath());
    }
    try {
      return new WeightedCommonBehaviourCoverageTestFitness(
          ClassExecutionCounts.readCounts(new Scanner(file).useDelimiter("\\Z").next()),
          new LineCoverageFactory());
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Just checked if file exists, but not accessible anymore", e);
    } catch (JsonSyntaxException e) {
      logger.error("Execution count file malformed", e);
      throw new RuntimeException(e);
    }
  }
}
