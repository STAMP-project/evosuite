package org.evosuite.coverage.cbehaviour;

import java.util.List;
import java.util.stream.Collectors;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.testsuite.AbstractFitnessFactory;

@SuppressWarnings("WeakerAccess")
public class CommonBehaviourCoverageFactory extends
    AbstractFitnessFactory<LineCoverageTestFitness> {

  private final LineCoverageFactory delegateFactory;
  private final List<ClassExecutionCounts> executionCounts;

  @SuppressWarnings("unused")
  public CommonBehaviourCoverageFactory(List<ClassExecutionCounts> executionCounts) {
    delegateFactory = new LineCoverageFactory();
    this.executionCounts = executionCounts;
  }

  @Override
  public List<LineCoverageTestFitness> getCoverageGoals() {
    List<LineCoverageTestFitness> allLines = delegateFactory.getCoverageGoals();
    return allLines.stream().filter(line -> executionCounts.stream().anyMatch(
        count -> line.getClassName().equals(count.className) && count.methods.stream()
            .anyMatch(method ->
                line.getMethod().equals(method.methodName) && method.executionCounts.stream()
                    .anyMatch(executionCount ->
                        line.getLine() == executionCount.line
                    )))).collect(Collectors.toList());
  }
}
