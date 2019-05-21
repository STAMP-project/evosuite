/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.coverage.cbehaviour;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.evosuite.Properties;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.ga.archive.Archive;
import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonBehaviourCoverageSuiteFitness extends TestSuiteFitnessFunction {

	private static final long serialVersionUID = -6369027784777941998L;

	private final static Logger logger = LoggerFactory.getLogger(CommonBehaviourCoverageSuiteFitness.class);

	// Coverage targets
	private final Map<Integer, TestFitnessFunction> lineGoals = new LinkedHashMap<Integer, TestFitnessFunction>();
	private final int totalWeight;

	private final Set<Integer> removedLines = new LinkedHashSet<Integer>();
	private final Set<Integer> toRemoveLines = new LinkedHashSet<Integer>();

	// Some stuff for debug output
	private int maxCoveredLines = 0;
	private double bestFitness = Double.MAX_VALUE;

	private ClassExecutionCounts counts;

	public CommonBehaviourCoverageSuiteFitness(AbstractFitnessFactory<LineCoverageTestFitness> goalFactory,
			ClassExecutionCounts counts) {
		@SuppressWarnings("unused")
		String prefix = Properties.TARGET_CLASS_PREFIX;

		/* TODO: Would be nice to use a prefix here */
//		for(String className : LinePool.getKnownClasses()) {		
//			lines.addAll(LinePool.getLines(className));
//		}
//		logger.info("Total line coverage goals: " + lines);

		List<LineCoverageTestFitness> goals = goalFactory.getCoverageGoals();
		for (LineCoverageTestFitness goal : goals) {
			lineGoals.put(goal.getLine(), goal);
			if(Properties.TEST_ARCHIVE)
				Archive.getArchiveInstance().addTarget(goal);
		}

		this.counts = counts;
		this.totalWeight = counts.totalCount();
		logger.info("Total line coverage goals: " + this.totalWeight);
	}

	@Override
	public boolean updateCoveredGoals() {
		if(!Properties.TEST_ARCHIVE)
			return false;

		for (Integer goalID : this.toRemoveLines) {
			TestFitnessFunction ff = this.lineGoals.remove(goalID);
			if (ff != null) {
				this.removedLines.add(goalID);
			} else {
				throw new IllegalStateException("goal to remove not found");
			}
		}

		this.toRemoveLines.clear();
		logger.info("Current state of archive: " + Archive.getArchiveInstance().toString());

		return true;
	}
	
	/**
	 * Iterate over all execution results and summarize statistics
	 * 
	 * @param results
	 * @param coveredLines
	 * @return
	 */
	private boolean analyzeTraces(List<ExecutionResult> results, Set<Integer> coveredLines) {
		boolean hasTimeoutOrTestException = false;

		for (ExecutionResult result : results) {
			if (result.hasTimeout() || result.hasTestException()) {
				hasTimeoutOrTestException = true;
				continue;
			}

			TestChromosome test = new TestChromosome();
			test.setTestCase(result.test);
			test.setLastExecutionResult(result);
			test.setChanged(false);

			for (Integer goalID : this.lineGoals.keySet()) {
				TestFitnessFunction goal = this.lineGoals.get(goalID);

				double fit = goal.getFitness(test, result); // archive is updated by the TestFitnessFunction class

				if (fit == 0.0) {
					coveredLines.add(goalID); // helper to count the number of covered goals
					this.toRemoveLines.add(goalID); // goal to not be considered by the next iteration of the evolutionary algorithm
				}
			}
		}

		return hasTimeoutOrTestException;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Execute all tests and count covered branches
	 */
	@Override
	public double getFitness(
	        AbstractTestSuiteChromosome<? extends ExecutableChromosome> suite) {
		logger.trace("Calculating branch fitness");
		double fitness = 0.0;

		List<ExecutionResult> results = runTestSuite(suite);

		// Collect stats in the traces 
		Set<Integer> coveredLines = new LinkedHashSet<Integer>();
		boolean hasTimeoutOrTestException = analyzeTraces(results, coveredLines);

		int coveredWeight = counts.weightOfLines(coveredLines) + counts.weightOfLines(this.removedLines);
		
		logger.debug("Covered weight" + coveredWeight + " out of total weight" + this.totalWeight + ", "+removedLines.size() +" weight in archive");
		fitness += normalize(this.totalWeight - coveredWeight);
		
		printStatusMessages(suite, coveredWeight, fitness);

		if (this.totalWeight > 0)
			suite.setCoverage(this, (double) coveredWeight / (double) this.totalWeight);
        else
            suite.setCoverage(this, 1.0);

		suite.setNumOfCoveredGoals(this, coveredWeight);
		
		if (hasTimeoutOrTestException) {
			logger.info("Test suite has timed out, setting fitness to max value " + this.totalWeight);
			fitness = this.totalWeight;
			//suite.setCoverage(0.0);
		}

		updateIndividual(this, suite, fitness);

		assert (coveredWeight <= this.totalWeight) : "Covered " + coveredWeight + " vs total goals " + this.totalWeight;
		assert (fitness >= 0.0);
		assert (fitness != 0.0 || coveredWeight == this.totalWeight) : "Fitness: " + fitness + ", "
		        + "coverage: " + coveredWeight + "/" + this.totalWeight;
		assert (suite.getCoverage(this) <= 1.0) && (suite.getCoverage(this) >= 0.0) : "Wrong coverage value "
		        + suite.getCoverage(this);

		return fitness;
	}

	/**
	 * Some useful debug information
	 * 
	 * @param coveredLines
	 * @param fitness
	 */
	private void printStatusMessages(
	        AbstractTestSuiteChromosome<? extends ExecutableChromosome> suite,
	        int coveredLines, double fitness) {
		if (coveredLines > maxCoveredLines) {
			maxCoveredLines = coveredLines;
			logger.info("(Lines) Best individual covers " + coveredLines + "/"
			        + this.totalWeight + " lines");
			logger.info("Fitness: " + fitness + ", size: " + suite.size() + ", length: "
			        + suite.totalLengthOfTestCases());
		}

		if (fitness < bestFitness) {
			logger.info("(Fitness) Best individual covers " + coveredLines + "/"
			        + this.totalWeight + " lines");
			bestFitness = fitness;
			logger.info("Fitness: " + fitness + ", size: " + suite.size() + ", length: "
			        + suite.totalLengthOfTestCases());

		}
	}
}
