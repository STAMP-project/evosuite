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
package org.evosuite.ga.stoppingconditions;

import org.evosuite.Properties;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.utils.LoggingUtils;


/**
 * Stop search after a predefined amount of time
 *
 * @author Gordon Fraser
 */
public class MaxTimeStoppingCondition extends StoppingConditionImpl {

	private static final long serialVersionUID = -4524853279562896768L;

	/** Maximum number of seconds */
	protected long maxSeconds = Properties.SEARCH_BUDGET;

	protected long startTime;

	/** {@inheritDoc} */
	@Override
	public void searchStarted(GeneticAlgorithm<?> algorithm) {
		startTime = System.currentTimeMillis();
	}

	/**
	 * {@inheritDoc}
	 *
	 * We are finished when the time is up
	 */
	@Override
	public boolean isFinished() {


		long currentTime = System.currentTimeMillis();
		long diff = (currentTime - startTime) / 1000;
		if(Properties.SEED_MODEL && diff>maxSeconds/4){
			LoggingUtils.getEvoLogger().info("* Turning model seeding off");
			Properties.SEED_MODEL=false;
			Properties.ONLINE_MODEL_SEEDING=false;
			Properties.SEED_CLONE = 0;
			Properties.P_OBJECT_POOL = 0;
		}
		return diff > maxSeconds;
	}

	/**
	 * {@inheritDoc}
	 *
	 * Reset
	 */
	@Override
	public void reset() {
		startTime = System.currentTimeMillis();
	}

	/* (non-Javadoc)
	 * @see org.evosuite.ga.StoppingCondition#setLimit(int)
	 */
	/** {@inheritDoc} */
	@Override
	public void setLimit(long limit) {
		maxSeconds = limit;
	}

	/** {@inheritDoc} */
	@Override
	public long getLimit() {
		return maxSeconds;
	}

	/* (non-Javadoc)
	 * @see org.evosuite.ga.StoppingCondition#getCurrentValue()
	 */
	/** {@inheritDoc} */
	@Override
	public long getCurrentValue() {
		long currentTime = System.currentTimeMillis();
		return (currentTime - startTime) / 1000;
	}

	/** {@inheritDoc} */
	@Override
	public void forceCurrentValue(long value) {
		startTime = value;
	}

}
