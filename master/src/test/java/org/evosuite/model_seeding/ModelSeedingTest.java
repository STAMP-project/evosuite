package org.evosuite.model_seeding;

import org.evosuite.EvoSuite;
import org.evosuite.statistics.RuntimeVariable;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@NotThreadSafe
@Ignore
public class ModelSeedingTest {
    public static String user_dir = System.getProperty("user.dir");
    public static String test_dir = Paths.get(user_dir,"src","test","java","org","evosuite","model_seeding").toString();
    public static String  bin_path = Paths.get(test_dir, "projectCP","sat4j").toString();
    public static String model_dir = Paths.get(test_dir, "models","LANG-9b").toString();
    private static String separator = System.getProperty("path.separator");
//    private static String targetClass = "org.apache.commons.lang3.time.FastDateFormat";
    private static String targetClass = "org.sat4j.minisat.constraints.MixedDataStructureSingleWL";


    @Test
    public void lang9b(){
        String p_object_pool="0.2";
        String seed_clone="0.8";
        String[] command = {


//                "-generateTests",
//                "-generateSuite",
                "-generateMOSuite",
                "-Dalgorithm=DynaMOSA",
//                "-Dcriterion=BRANCH",
//                "-Dreport_dir="+test_dir, //later
                "-Doutput_variables=TARGET_CLASS,search_budget,Total_Time,Length,Size,LineCoverage,BranchCoverage,OutputCoverage,WeakMutationScore,Implicit_MethodExceptions,MutationScore",
                "-Dpopulation="+100,
                "-Dsearch_budget="+215000,
                "-Dtimeline_interval="+5*1000,
//                "-Dinitialization_timeout=500",
                "-Dp_object_pool="+p_object_pool,
                "-Dseed_clone="+seed_clone,
                "-Dstopping_condition=MAXFITNESSEVALUATIONS",
                "-Dshow_progress=FALSE",
//                "-Dcarve_object_pool=TRUE", // later
//                "-Dselected_junit="+junit, // later
//                "-Dreset_static_fields=FALSE",
//                "-Dvirtual_net=FALSE",
                "-projectCP",
                getListOfDeps(),
                "-class",
                targetClass,
                "-Dmodel_path="+model_dir,

                "-Donline_model_seeding=TRUE"


        };

        EvoSuite evosuite = new EvoSuite();
        Object result = evosuite.parseCommandLine(command);
    }

    public static String getListOfDeps(){
        String dependencies = "";
        File depFolder = new File(bin_path);
        File[] listOfFilesInSourceFolder = depFolder.listFiles();
        for(int i = 0; i < listOfFilesInSourceFolder.length; i++) {
            if (listOfFilesInSourceFolder[i].getName().charAt(0) != '.') {
                Path depPath = Paths.get(depFolder.getAbsolutePath(), listOfFilesInSourceFolder[i].getName());
                dependencies += (depPath.toString() + separator);
            }
        }
        return dependencies;
    }
}
