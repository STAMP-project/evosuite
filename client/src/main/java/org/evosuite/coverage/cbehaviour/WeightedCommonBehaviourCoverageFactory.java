package org.evosuite.coverage.cbehaviour;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.evosuite.Properties;
import org.evosuite.testsuite.AbstractFitnessFactory;

public class WeightedCommonBehaviourCoverageFactory extends
    AbstractFitnessFactory<WeightedCommonBehaviourCoverageTestFitness> {

  @Override
  public List<WeightedCommonBehaviourCoverageTestFitness> getCoverageGoals() {
    return Collections.singletonList(WeightedCommonBehaviourCoverageTestFitness
        .fromExecutionCountFile(new File(Properties.EXE_COUNT_FILE)));
  }
}
