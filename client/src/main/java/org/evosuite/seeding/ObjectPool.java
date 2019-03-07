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
/**
 *
 */
package org.evosuite.seeding;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.*;

import be.vibes.dsl.io.Xml;
import be.vibes.dsl.selection.Dissimilar;
import be.vibes.ts.Action;
import be.vibes.ts.TestSet;
import be.vibes.ts.Transition;
import be.vibes.ts.UsageModel;
import org.apache.commons.lang3.StringUtils;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.setup.TestClusterUtils;
import org.evosuite.testcarver.extraction.CarvingRunListener;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFactory;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.DebuggingObjectOutputStream;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.generic.GenericClass;
import org.evosuite.utils.Randomness;
import org.evosuite.utils.generic.GenericConstructor;
import org.evosuite.utils.generic.GenericMethod;
import org.junit.runner.JUnitCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pool of interesting method sequences for different objects
 *
 * @author Gordon Fraser
 */
public class ObjectPool implements Serializable {

	private static final long serialVersionUID = 2016387518459994272L;

	/** The actual object pool */
	protected final Map<GenericClass, Set<TestCase>> pool = new HashMap<GenericClass, Set<TestCase>>();
	protected final Map<GenericClass, Set<TestCase>> usedPool = new HashMap<GenericClass, Set<TestCase>>();
	public final Map<String, TestSet> abstractTests = new HashMap<String, TestSet>();

	protected static final Logger logger = LoggerFactory.getLogger(ObjectPool.class);

	/**
	 * Insert a new sequence for given Type
	 *
	 * @param clazz
	 *            a {@link java.lang.reflect.Type} object.
	 * @param sequence
	 *            a {@link org.evosuite.testcase.TestCase} object.
	 */
	public void addSequence(GenericClass clazz, TestCase sequence) {
		ObjectSequence seq = new ObjectSequence(clazz, sequence);
		addSequence(seq);
	}

	public void addAbstractTest(String className, TestSet abstractTests){
		this.abstractTests.put(className,abstractTests);
	}

	protected void concretizeTest(be.vibes.ts.TestCase abstractTestCase, HashSet<String> alreadyConcretized){
		if (alreadyConcretized == null){
			alreadyConcretized = new HashSet<>();
		}
		TestCase newTestCase = new DefaultTestCase();
		GenericClass genericClass = null;
		for (Transition transition : abstractTestCase) {
			Action sequence = transition.getAction();
			if (sequence.getName().indexOf(".") != -1) {
				// Class name:
				String className = sequence.getName().substring(0, sequence.getName().indexOf("("));
				className = className.substring(0, className.lastIndexOf('.'));
				// Method name:`
				String methodName = StringUtils.substringAfterLast(sequence.getName().substring(0, sequence.getName().indexOf("(")), ".");
				String paramString = sequence.getName().substring(sequence.getName().indexOf("(") + 1);
				// Params:
				paramString = paramString.substring(0, paramString.indexOf(")"));
				String[] paramArr = paramString.split(",");
				//								try {
				//Getting the Class
				Class<?> sequenceClass = null;
				try {
					sequenceClass = Class.forName(className, true, TestGenerationContext.getInstance().getClassLoaderForSUT());
				} catch (ClassNotFoundException | ExceptionInInitializerError | NoClassDefFoundError e) {
					logger.debug("could not load " + className);
				}
				if (sequenceClass != null) {
					genericClass = new GenericClass(sequenceClass);
					//Getting methods
					Set<Method> methods = TestClusterUtils.getMethods(sequenceClass);
					//Getting Constructors
					Set<Constructor<?>> constructors = TestClusterUtils.getConstructors(sequenceClass);

					// find the method that we want
					Method target = null;
					for (Method m : methods) {
						if (m.getName().equals(methodName)) {
							target = m;
							break;
						} else {
							target = null;
						}
					}

					// Find the constructor that we want
					Constructor targetC = null;
					for (Constructor c : constructors) {
						boolean same = true;
						int counter = 0;

						for (Class<?> cl : c.getParameterTypes()) {
							if (paramArr.length > counter && !cl.getName().equals(paramArr[counter])) {
								same = false;
							}
							counter++;
						}
						if (same) {
							targetC = c;
							break;
						}
					}


					if (target != null) {
						GenericMethod genericMethod = new GenericMethod(target, sequenceClass);
						try {
							TestFactory.getInstance().addMethod(newTestCase, genericMethod, newTestCase.size(), 0);
							logger.debug("method call {} is added", genericMethod.getName());
						} catch (Exception e) {
							logger.debug("Error in addidng " + genericMethod.getName() + "  " + e.getMessage());
						}
					} else if (targetC != null) {
						GenericConstructor genericConstructor = new GenericConstructor(targetC, sequenceClass);
						try {
							TestFactory.getInstance().addConstructor(newTestCase, genericConstructor, newTestCase.size(), 0);
							logger.debug("constructor {} is added", genericConstructor.getName());
						} catch (Exception e) {
							logger.debug("Error in addidng " + genericConstructor.getName() + "  " + e.getMessage());
						}

					} else {
						logger.debug("Fail to add the call to add!");
					}
				}
			}

		}

		// Add test case to pool
		if (genericClass != null){
			logger.debug("New test case added for class {}",genericClass.getClassName());
			try{
				String testCode = newTestCase.toCode();
				logger.debug("Add the following tests case to the object pool of class {}: {}",genericClass.getClassName(),testCode);
				if(!alreadyConcretized.contains(testCode)) {
					this.addSequence(genericClass, newTestCase);
					alreadyConcretized.add(testCode);
				}
			}catch (Exception e){
				logger.debug("The generated test case is not valid.");
			}

		}
	}

	private void reConcretizeTests(String clazz){
		HashSet<String> alreadyConcretized = new HashSet<>();
		TestSet ts = this.abstractTests.get(clazz);
		for (int i=0; i<ts.size();i++) {
			be.vibes.ts.TestCase abstractTestCase=this.abstractTests.get(clazz).get(i);
			concretizeTest(abstractTestCase,alreadyConcretized);
		}
	}




	/**
	 * Helper method to add sequences
	 *
	 * @param sequence
	 */
	private void addSequence(ObjectSequence sequence) {
		if (!pool.containsKey(sequence.getGeneratedClass()))
			pool.put(sequence.getGeneratedClass(), new HashSet<TestCase>());

		pool.get(sequence.getGeneratedClass()).add(sequence.getSequence());
		logger.info("Added new sequence for " + sequence.getGeneratedClass());
		logger.info(sequence.getSequence().toCode());

	}


	public void resetUsedSequences(GenericClass clazz){
		for (TestCase seq: usedPool.get(clazz)){
			getSequences(clazz).add(seq);
		}
		usedPool.get(clazz).clear();
//		LoggingUtils.getEvoLogger().info(clazz.getClassName()+" pool resetting.");
	}

	/**
	 * Randomly choose a sequence for a given Type
	 *
	 * @param clazz
	 *            a {@link java.lang.reflect.Type} object.
	 * @return a {@link org.evosuite.testcase.TestCase} object.
	 */
	public TestCase getRandomSequence(GenericClass clazz) {
		if(Properties.MODEL_PATH != null){
			if (getSequences(clazz).size() == 0) {
				resetUsedSequences(clazz);
//				refillSequences(clazz);
//				reConcretizeTests(clazz.getClassName());
			}
			TestCase result = Randomness.choice(getSequences(clazz));
			getSequences(clazz).remove(result);
			if (!usedPool.containsKey(clazz))
				usedPool.put(clazz, new HashSet<TestCase>());
			usedPool.get(clazz).add(result);
//			LoggingUtils.getEvoLogger().info("using sequence {} of object {}.",result.toCode(),clazz.getClassName());
			return result;
		}else{
			return Randomness.choice(getSequences(clazz));
		}

	}

	private void refillSequences(GenericClass clazz) {
		File folder = new File(Properties.MODEL_PATH);
		String modelPath=Paths.get(folder.getAbsolutePath(), clazz.getClassName()+".xml").toString();
		fillObjectPool(clazz,modelPath);
	}


	/**
	 * Retrieve all possible sequences for a given Type
	 *
	 * @param clazz
	 *            a {@link java.lang.reflect.Type} object.
	 * @return a {@link java.util.Set} object.
	 */
	public Set<TestCase> getSequences(GenericClass clazz) {
		if (pool.containsKey(clazz))
			return pool.get(clazz);

		Set<Set<TestCase>> candidates = new LinkedHashSet<Set<TestCase>>();
		for (GenericClass poolClazz : pool.keySet()) {
			if (poolClazz.isAssignableTo(clazz))
				candidates.add(pool.get(poolClazz));
		}

		return Randomness.choice(candidates);

	}

	public Set<GenericClass> getClasses() {
		return pool.keySet();
	}

	/**
	 * Check if there are sequences for given Type
	 *
	 * @param clazz
	 *            a {@link java.lang.reflect.Type} object.
	 * @return a boolean.
	 */
	public boolean hasSequence(GenericClass clazz) {
		if (pool.containsKey(clazz))
			return true;

		for (GenericClass poolClazz : pool.keySet()) {
			if (poolClazz.isAssignableTo(clazz))
				return true;
		}

		return false;
	}

	public int getNumberOfClasses() {
		return pool.size();
	}
	public int getNumberOfSequences(GenericClass clazz) {
		int nonUsed = 0;
		int used = 0;
		if(pool.containsKey(clazz)){
			nonUsed = pool.get(clazz).size();
		}

		if(usedPool.containsKey(clazz)){
			used = usedPool.size();
		}
		return used+nonUsed;
	}

	public int getNumberOfSequences() {
		int num = 0;
		for (Set<TestCase> p : pool.values()) {
			num += p.size();
		}
		return num;
	}

	public boolean isEmpty() {
		return pool.isEmpty();
	}

	/**
	 * Read a serialized pool
	 *
	 * @param fileName
	 */
	public static ObjectPool getPoolFromFile(String fileName) {
		try {
			InputStream in = new FileInputStream(fileName);
			ObjectInputStream objectIn = new ObjectInputStream(in);
			ObjectPool pool = (ObjectPool) objectIn.readObject();
			in.close();
			// TODO: Do we also need to call that in the other factory methods?
			pool.filterUnaccessibleTests();
			return pool;
		} catch (Exception e) {
			logger.error("Exception while trying to get object pool from " + fileName
			        + " , " + e.getMessage(), e);
		}
		return null;
	}

	protected void filterUnaccessibleTests() {
		for(Set<TestCase> testSet : pool.values()) {
			Iterator<TestCase> testIterator = testSet.iterator();
			while(testIterator.hasNext()) {
				TestCase currentTest = testIterator.next();
				if(!currentTest.isAccessible()) {
					logger.info("Removing test containing inaccessible elements");
					testIterator.remove();
				}
			}
		}
	}

	/**
	 * Convert a test suite to a pool
	 *
	 * @param testSuite
	 */
	public static ObjectPool getPoolFromTestSuite(TestSuiteChromosome testSuite) {
		ObjectPool pool = new ObjectPool();

		for (TestChromosome testChromosome : testSuite.getTestChromosomes()) {
			TestCase test = testChromosome.getTestCase().clone();
			test.removeAssertions();
			/*
			if (testChromosome.hasException()) {
				// No code including or after an exception should be in the pool
				Integer pos = testChromosome.getLastExecutionResult().getFirstPositionOfThrownException();
				if (pos != null) {
					test.chop(pos);
				} else {
					test.chop(test.size() - 1);
				}
			}
			*/
			Class<?> targetClass = Properties.getTargetClassAndDontInitialise();

			if (!testChromosome.hasException()
			        && test.hasObject(targetClass, test.size())) {
				pool.addSequence(new GenericClass(targetClass), test);
			}
		}

		return pool;
	}

	/**
	 * Execute all tests in a JUnit test suite and add resulting sequences from
	 * carver
	 *
	 * @param targetClass
	 * @param testSuite
	 */
	public static ObjectPool getPoolFromJUnit(GenericClass targetClass, Class<?> testSuite) {
		final JUnitCore runner = new JUnitCore();
		final CarvingRunListener listener = new CarvingRunListener();
		runner.addListener(listener);

		final org.evosuite.testcarver.extraction.CarvingClassLoader classLoader = new org.evosuite.testcarver.extraction.CarvingClassLoader();

		try {
			// instrument target class
			classLoader.loadClass(Properties.TARGET_CLASS);
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		ObjectPool pool = new ObjectPool();
		//final Result result =
		runner.run(testSuite);


		for (TestCase test : listener.getTestCases().get(Properties.getTargetClassAndDontInitialise())) {
			// TODO: Maybe we would get the targetClass from the last object generated in the sequence?
			pool.addSequence(targetClass, test);
		}

		// TODO: Some messages based on result

		return pool;

	}

	public void writePool(String fileName) {
		try {
			ObjectOutputStream out = new DebuggingObjectOutputStream(
			        new FileOutputStream(fileName));
			out.writeObject(this);
			out.close();
		} catch (IOException e) {
			logger.warn("Error while writing pool to file "+fileName+": "+e);
		}
	}


	protected void fillObjectPool(GenericClass clazz, String modelPath) {
		Properties.ALLOW_OBJECT_POOL_USAGE=false;
		try {
			UsageModel um = Xml.loadUsageModel(modelPath);
			TestSet ts = Dissimilar.from(um).withGlobalMaxDistance(Dissimilar.jaccard()).during(500).generate(Properties.POPULATION);
			HashSet<String> alreadyConcretized = new HashSet<>();
			for (be.vibes.ts.TestCase abstractTestCase : ts) {
				concretizeTest(abstractTestCase,alreadyConcretized);
			}
		}catch (Exception e) {
			logger.debug("Could not load model " + clazz.getClassName());
		}finally {
			Properties.ALLOW_OBJECT_POOL_USAGE=true;
		}

	}

}
