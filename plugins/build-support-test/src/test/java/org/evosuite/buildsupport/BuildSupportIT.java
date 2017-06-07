/**
 * Copyright (C) 2010-2017 Gordon Fraser, Andrea Arcuri and EvoSuite
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
package org.evosuite.buildsupport;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.Verifier;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.evosuite.runtime.InitializingListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

/**
 * Make sure that tests generated by EvoSuite can be run from different build tools.
 * In particular, here we are interested to see if instrumentation is properly done.
 *
 * Note: the tests do not have instrumentation code, and so they will pass only if
 * JUnit listener does it
 */
public class BuildSupportIT {

    private final Path simple = Paths.get("projects","simple");

    private String getEvoSuiteVersion(){
        //update version if run from IDE instead of Maven
        return System.getProperty("evosuiteVersion","1.0.5-SNAPSHOT");
    }

    @Before
    @After
    public void clean() throws Exception {
        Verifier maven = getMaven(simple);
        maven.executeGoal("clean");

        FileUtils.deleteQuietly(simple.resolve("log.txt").toFile());
        FileUtils.deleteQuietly(simple.resolve(InitializingListener.getScaffoldingListFilePath()).toFile());
        FileUtils.deleteQuietly(simple.resolve("build").toFile());
    }

    @Test
    public void testMaven() throws Exception{
        Verifier maven = getMaven(simple);
        maven.executeGoal("test");
        maven.verifyTextInLog("Running com.testbuild.support.FooTest");
        maven.verifyTextInLog("Tests run: 3, Failures: 0, Errors: 0, Skipped: 0");
    }


    @Ignore("Pass from IDE, but fail from Maven")
    @Test
    public void testAnt() throws Exception{

        /*
            Getting Ant to work is a mess :(
            No wonder no one is really using it anymore...

            This test passes on IDE, but fail when executed from Maven.
            Failed to try to get it work, as Ant as so many bugs and weird,
            counter-intuitive behaviors...

            A possible solution would be to call an external Ant from a spawn
            process. Doable, but overcomplicated, as anyway having such test
            in our Jenkins is not critical, ie breaking support for Ant
            is not so critical as it would be for Maven and Gradle.
         */

        Project project = new Project();
        project.setBasedir("projects/simple");

        project.setUserProperty("evosuiteVersion",getEvoSuiteVersion());
        //the setBasedir simply does not work, so need to re-define paths here :(
        project.setUserProperty(InitializingListener.COMPILED_TESTS_FOLDER_PROPERTY, "projects/simple/build/tests");
        //project.setUserProperty("mavenAntTasksCP","projects/simple/lib/maven-ant-tasks-2.1.3.jar");
        project.setUserProperty("mavenAntTasksCP","lib/maven-ant-tasks-2.1.3.jar");

        project.setUserProperty("compile.fork","false");
        project.init();

        Path buildFile = simple.resolve("build.xml");
        ProjectHelper.configureProject( project, buildFile.toFile());


        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(errBuffer);
        PrintStream err = new PrintStream(outBuffer);

        DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(err);
        consoleLogger.setOutputPrintStream(out);
        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
        project.addBuildListener(consoleLogger);

        String phase = "clean";
        try {
            project.executeTarget(phase);
            phase = "compile";
            project.executeTarget(phase);
            phase = "compile-tests";
            project.executeTarget(phase);
            phase = "test";
            project.executeTarget(phase);
        } catch (Exception e){
            String res = outBuffer.toString()+"\n" + errBuffer.toString();
            throw new RuntimeException("Failed to execute Ant on phase '"+phase+"': "+e.getMessage()+"\nLOGS:\n"+res, e);
        }

        String res = outBuffer.toString()+"\n" + errBuffer.toString();

        assertTrue(res, res.contains("[junit] Testcase: testInstrumentation took"));
        assertTrue(res, res.contains("[junit] Testcase: testDoesFileExist_deactivatedInstrumentation took"));
        assertTrue(res, res.contains("[junit] Testcase: testDoesFileExist_withInstrumentation took"));
    }



    private Verifier getMaven(Path targetProject) throws Exception{
        Verifier verifier  = new Verifier(targetProject.toAbsolutePath().toString());
        Properties props = new Properties(System.getProperties());
        props.put("evosuiteVersion", getEvoSuiteVersion());
        verifier.setSystemProperties(props);
        return verifier;
    }

}
