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
package org.evosuite.seeding;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

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
import org.evosuite.testcarver.extraction.CarvingManager;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestFactory;
import org.evosuite.utils.generic.GenericClass;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.generic.GenericConstructor;
import org.evosuite.utils.generic.GenericMethod;

public class ObjectPoolManager extends ObjectPool {

	private static final long serialVersionUID = 6287216639197977371L;

	private static ObjectPoolManager instance = null;

	private ObjectPoolManager() {
		initialisePool();
	}

	public boolean usable = false;

	public static ObjectPoolManager getInstance() {
		if(instance == null)
			instance = new ObjectPoolManager();
		return instance;
	}

	public void addPool(ObjectPool pool) {
		for(GenericClass clazz : pool.getClasses()) {
			Set<TestCase> tests = pool.getSequences(clazz);
			if(this.pool.containsKey(clazz))
				this.pool.get(clazz).addAll(tests);
			else
				this.pool.put(clazz, tests);
		}
	}

	public void initialisePool() {
		if(!Properties.OBJECT_POOLS.isEmpty()) {
			String[] poolFiles = Properties.OBJECT_POOLS.split(File.pathSeparator);
			if(poolFiles.length > 1)
				LoggingUtils.getEvoLogger().info("* Reading object pools:");
			else
				LoggingUtils.getEvoLogger().info("* Reading object pool:");
			for(String fileName : poolFiles) {
				logger.info("Adding object pool from file "+fileName);
				ObjectPool pool = ObjectPool.getPoolFromFile(fileName);
				if(pool==null){
					logger.error("Failed to load object from "+fileName);
				} else {
					LoggingUtils.getEvoLogger().info(" - Object pool "+fileName+": "+pool.getNumberOfSequences()+" sequences for "+pool.getNumberOfClasses()+" classes");
					addPool(pool);
				}
			}
			if(logger.isDebugEnabled()) {
				for(GenericClass key : pool.keySet()) {
					logger.debug("Have sequences for "+key+": "+pool.get(key).size());
				}
			}
		}
		if(Properties.CARVE_OBJECT_POOL) {
			CarvingManager manager = CarvingManager.getInstance();
			for(Class<?> targetClass : manager.getClassesWithTests()) {
				List<TestCase> tests = manager.getTestsForClass(targetClass);
				logger.info("Carved tests for {}: {}", targetClass.getName(), tests.size());
				GenericClass cut = new GenericClass(targetClass);
				for(TestCase test : tests) {
					this.addSequence(cut, test);
				}
			}
			logger.info("Pool after carving: "+this.getNumberOfClasses()+"/"+this.getNumberOfSequences());
		}

		if(Properties.SEED_MODEL){
			File folder = new File(Properties.MODEL_PATH);
			if(folder.exists()){
				seedModel(folder);
			}else{
				// If the given model directory does not exist, we will print a warning and continue the search without model seeding.
				LoggingUtils.getEvoLogger().warn("The model directory {} is not right!",Properties.MODEL_PATH);
			}
		}

		usable= true;
	}

	private void seedModel(File folder) {
		// Here we are sure that the folder is exists
		File[] listOfFiles = folder.listFiles();
		LoggingUtils.getEvoLogger().info("Start carving model in directory: {}",folder.getAbsolutePath());
		for (File file : listOfFiles){
			if (file.isFile() && !file.getName().startsWith(".") && file.getName().endsWith(".xml") ) {
				String xmlClassName = file.getName().substring(0, file.getName().length() - 4);
				if (xmlClassName.indexOf('.')== -1 || xmlClassName.contains(".java.")){
					UsageModel um = Xml.loadUsageModel(Paths.get(folder.getAbsolutePath(), file.getName()).toString());
					TestSet ts = Dissimilar.from(um).withGlobalMaxDistance(Dissimilar.jaccard()).during(Properties.ABSTRACT_TESTCASE_SELECTION_DURATION).generate(Properties.ABSTRACT_TESTCASE_PER_MODEL);
					for (be.vibes.ts.TestCase abstractTestCase : ts) {
						concretizeAbstractTestCase(abstractTestCase);
					}
				}
			}
		}
	}

	private void concretizeAbstractTestCase(be.vibes.ts.TestCase abstractTestCase) {
		TestCase newTestCase = new DefaultTestCase();
		GenericClass genericClass = null;
		for (Transition transition : abstractTestCase) {
			Action transitionAction = transition.getAction();
			if (transitionAction.getName().indexOf(".") != -1) {
				// Class name:
				String className = transitionAction.getName().substring(0, transitionAction.getName().indexOf("("));
				className = className.substring(0, className.lastIndexOf('.'));
				// Method name:
				String methodName = StringUtils.substringAfterLast(transitionAction.getName().substring(0, transitionAction.getName().indexOf("(")), ".");
				String paramString = transitionAction.getName().substring(transitionAction.getName().indexOf("(") + 1);
				// Params:
				paramString = paramString.substring(0, paramString.indexOf(")"));
				String[] paramArr = paramString.split(",");

				//Getting the Class
				Class<?> actionClass = null;
				try {
					actionClass = Class.forName(className, true, TestGenerationContext.getInstance().getClassLoaderForSUT());
				} catch (ClassNotFoundException | ExceptionInInitializerError | NoClassDefFoundError e) {
					LoggingUtils.getEvoLogger().info("could not load " + className);
				}

				if (actionClass != null) {
					genericClass = new GenericClass(actionClass);
					//Getting methods
					Set<Method> methods = TestClusterUtils.getMethods(actionClass);
					//Getting Constructors
					Set<Constructor<?>> constructors = TestClusterUtils.getConstructors(actionClass);

					Method target = findMethod(methodName,methods);
					Constructor targetC = findConstructor(paramArr, constructors);


					if (target != null) {
						GenericMethod genericMethod = new GenericMethod(target, actionClass);
						try {
							TestFactory.getInstance().addMethod(newTestCase, genericMethod, newTestCase.size(), 0);
							LoggingUtils.getEvoLogger().info("method call {} is added", genericMethod.getName());
						} catch (Exception e) {
							LoggingUtils.getEvoLogger().warn("Error in addidng " + genericMethod.getName() + "  " + e.getMessage());
						}
					} else if (targetC != null) {
						GenericConstructor genericConstructor = new GenericConstructor(targetC, actionClass);
						try {
							TestFactory.getInstance().addConstructor(newTestCase, genericConstructor, newTestCase.size(), 0);
							LoggingUtils.getEvoLogger().info("constructor {} is added", genericConstructor.getName());
						} catch (Exception e) {
							LoggingUtils.getEvoLogger().warn("Error in addidng " + genericConstructor.getName() + "  " + e.getMessage());
						}
					} else {
						LoggingUtils.getEvoLogger().warn("Fail to add the call to add: class:{}, method:{}",className,methodName);
					}

				}
			}
		}
		// Add test case to pool
		if (genericClass != null){
			this.addSequence(genericClass, newTestCase);
		}
	}

	private Constructor findConstructor(String[] paramArr, Set<Constructor<?>> constructors) {
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
		return targetC;
	}

	private Method findMethod(String methodName, Set<Method> methods) {
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
		return target;
	}

	public void reset() {
		pool.clear();
		ObjectPoolManager.instance = null;
	}

}
