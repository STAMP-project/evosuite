package org.evosuite.coverage.cbehaviour;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.evosuite.Properties;
import org.evosuite.testsuite.AbstractFitnessFactory;

/**
 * Factory that supplies fitness functions of type {@link ExecutionCountCoverageTestFitness}.
 * Depending on how this factory is constructed, the supplied fitness functions will value either
 * test cases that exercise often executed code, or test cases that don't.
 */
public class ExecutionCountCoverageFactory extends
    AbstractFitnessFactory<ExecutionCountCoverageTestFitness> {

  private final boolean forCommonBehaviours;

  /**
   * Creates an instance supplying a fitness function favoring either common or uncommon behaviours
   * depending on the value specified.
   *
   * @param forCommonBehaviours favors common behaviours if {@code true}, and uncommon behaviours if
   * {@code false}
   */
  public ExecutionCountCoverageFactory(boolean forCommonBehaviours) {
    this.forCommonBehaviours = forCommonBehaviours;
  }

  /**
   * Creates an instance supplying a fitness function favoring common behaviours.
   */
  public ExecutionCountCoverageFactory() {
    this(true);
  }

  @Override
  public List<ExecutionCountCoverageTestFitness> getCoverageGoals() {
    return Collections.singletonList(ExecutionCountCoverageTestFitness
        .fromExecutionCountFile(new File(Properties.EXE_COUNT_FILE), forCommonBehaviours));
  }
}
