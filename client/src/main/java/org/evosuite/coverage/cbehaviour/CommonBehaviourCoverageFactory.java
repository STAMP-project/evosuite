package org.evosuite.coverage.cbehaviour;

import java.util.List;
import java.util.stream.Collectors;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.testsuite.AbstractFitnessFactory;

/**
 * Class containing logic to supply line coverage goals only for lines that are present in the
 * execution count JSON file.
 */
@SuppressWarnings("WeakerAccess")
public class CommonBehaviourCoverageFactory extends
    AbstractFitnessFactory<LineCoverageTestFitness> {

  private final LineCoverageFactory delegateFactory;
  private final List<ClassExecutionCounts> executionCounts;

  /**
   * Constructs a new factory using the execution counts in the list that is provided.
   */
  @SuppressWarnings("unused")
  public CommonBehaviourCoverageFactory(List<ClassExecutionCounts> executionCounts) {
    delegateFactory = new LineCoverageFactory();
    this.executionCounts = executionCounts;
  }

  /**
   * Supplies line coverage goals only for lines that appear in the input execution count JSON file,
   * of which the contents have been provided upon construction of this object.
   */
  @Override
  public List<LineCoverageTestFitness> getCoverageGoals() {
    List<LineCoverageTestFitness> allLines = delegateFactory.getCoverageGoals();

    // Filter the list that is provided by the LineCoverageFactory so only lines are retained that
    // appear in the input execution counts.
    // In more low-level terms: for each line goal, it is first checked whether it corresponds to
    // a class present in the execution counts, then whether it corresponds to a method present in
    // the execution counts, and finally whether it corresponds to a line present in the execution
    // counts. If all three are true, the line goal is retained, otherwise it is discarded.
    return allLines.stream().filter(line -> executionCounts.stream().anyMatch(
        count -> line.getClassName().equals(count.className) && count.methods.stream()
            .anyMatch(method ->
                line.getMethod().equals(method.methodName) && method.executionCounts.stream()
                    .anyMatch(executionCount ->
                        line.getLine() == executionCount.line
                    )))).collect(Collectors.toList());
  }
}
