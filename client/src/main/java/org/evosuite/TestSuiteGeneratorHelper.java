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
package org.evosuite;

import java.util.Map;
import java.util.Set;
import org.evosuite.Properties.AssertionStrategy;
import org.evosuite.Properties.Criterion;
import org.evosuite.Properties.SecondaryObjective;
import org.evosuite.assertion.AssertionGenerator;
import org.evosuite.assertion.CompleteAssertionGenerator;
import org.evosuite.assertion.SimpleMutationAssertionGenerator;
import org.evosuite.assertion.UnitAssertionGenerator;
import org.evosuite.contracts.ContractChecker;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchPool;
import org.evosuite.rmi.ClientServices;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.strategy.*;
import org.evosuite.symbolic.DSEStrategy;
import org.evosuite.testcase.execution.ExecutionTraceImpl;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.objectweb.asm.Opcodes;

/**
 * Created by sina on 06/04/2017.
 */
public class TestSuiteGeneratorHelper {

  static void printTestCriterion(Criterion criterion) {
    switch (criterion) {
      case WEAKMUTATION:
        LoggingUtils.getEvoLogger().info("  - Mutation testing (weak)");
        break;
      case ONLYMUTATION:
        LoggingUtils.getEvoLogger().info("  - Only Mutation testing (weak)");
        break;
      case STRONGMUTATION:
      case MUTATION:
        LoggingUtils.getEvoLogger().info("  - Mutation testing (strong)");
        break;
      case DEFUSE:
        LoggingUtils.getEvoLogger().info("  - All DU Pairs");
        break;
      case STATEMENT:
        LoggingUtils.getEvoLogger().info("  - Statement Coverage");
        break;
      case RHO:
        LoggingUtils.getEvoLogger().info("  - Rho Coverage");
        break;
      case AMBIGUITY:
        LoggingUtils.getEvoLogger().info("  - Ambiguity Coverage");
        break;
      case ALLDEFS:
        LoggingUtils.getEvoLogger().info("  - All Definitions");
        break;
      case EXCEPTION:
        LoggingUtils.getEvoLogger().info("  - Exception");
        break;
      case ONLYBRANCH:
        LoggingUtils.getEvoLogger().info("  - Only-Branch Coverage");
        break;
      case METHODTRACE:
        LoggingUtils.getEvoLogger().info("  - Method Coverage");
        break;
      case METHOD:
        LoggingUtils.getEvoLogger().info("  - Top-Level Method Coverage");
        break;
      case METHODNOEXCEPTION:
        LoggingUtils.getEvoLogger().info("  - No-Exception Top-Level Method Coverage");
        break;
      case LINE:
        LoggingUtils.getEvoLogger().info("  - Line Coverage");
        break;
      case ONLYLINE:
        LoggingUtils.getEvoLogger().info("  - Only-Line Coverage");
        break;
      case OUTPUT:
        LoggingUtils.getEvoLogger().info("  - Method-Output Coverage");
        break;
      case INPUT:
        LoggingUtils.getEvoLogger().info("  - Method-Input Coverage");
        break;
      case BRANCH:
        LoggingUtils.getEvoLogger().info("  - Branch Coverage");
        break;
      case CBRANCH:
        LoggingUtils.getEvoLogger().info("  - Context Branch Coverage");
        break;
      case IBRANCH:
        LoggingUtils.getEvoLogger().info("  - Interprocedural Context Branch Coverage");
        break;
      case TRYCATCH:
        LoggingUtils.getEvoLogger().info("  - Try-Catch Branch Coverage");
        break;
      case REGRESSION:
        LoggingUtils.getEvoLogger().info("  - Regression");
        break;
      case ONLYEXECUTED:
        LoggingUtils.getEvoLogger().info("  - Only executed lines coverage");
        break;
      case MAX_EXEC_COUNT:
        LoggingUtils.getEvoLogger().info("  - Maximum execution count coverage");
        break;
      case MIN_EXEC_COUNT:
        LoggingUtils.getEvoLogger().info("  - Minimum execution count coverage");
        break;
      default:
        throw new IllegalArgumentException("Unrecognized criterion: " + criterion);
    }
  }

  /**
   * Returns for each secondary objective defined in the enum {@link Properties.SecondaryObjective}
   * a short description of what it does.
   */
  private static String getSecondaryObjectiveDescription(SecondaryObjective objective) {
    switch (objective) {
      case AVG_LENGTH:
        return "Minimum average length";
      case MAX_LENGTH:
        return "Minimize maximum test length";
      case TOTAL_LENGTH:
        return "Minimize total suite length";
      case SIZE:
        return "Minimum test case size";
      case EXCEPTIONS:
        return "Minimize number of triggered exceptions";
      case IBRANCH:
        return "Maximize total branch coverage for all classes (not only CUT)";
      case RHO:
        return "Minimize Rho fitness value";
      case MAX_EXEC_COUNT:
        return "Maximize coverage of code lines that have been executed the most";
      case MIN_EXEC_COUNT:
        return "Minimize coverage of code lines that have been executed the most";
      default:
        assert false : "All possible cases should be listed in this switch statement";
        return null;
    }
  }

  private static int getBytecodeCount(RuntimeVariable v, Map<RuntimeVariable, Set<Integer>> m) {
    Set<Integer> branchSet = m.get(v);
    return (branchSet == null) ? 0 : branchSet.size();
  }

  static void getBytecodeStatistics() {
    if (Properties.TRACK_BOOLEAN_BRANCHES) {
      int gradientBranchCount = ExecutionTraceImpl.gradientBranches.size() * 2;
      ClientServices.track(RuntimeVariable.Gradient_Branches, gradientBranchCount);
    }
    if (Properties.TRACK_COVERED_GRADIENT_BRANCHES) {
      int coveredGradientBranchCount = ExecutionTraceImpl.gradientBranchesCoveredTrue.size()
          + ExecutionTraceImpl.gradientBranchesCoveredFalse.size();
      ClientServices.track(RuntimeVariable.Gradient_Branches_Covered, coveredGradientBranchCount);
    }
    if (Properties.BRANCH_COMPARISON_TYPES) {
      int cmp_intzero = 0, cmp_intint = 0, cmp_refref = 0, cmp_refnull = 0;
      int bc_lcmp = 0, bc_fcmpl = 0, bc_fcmpg = 0, bc_dcmpl = 0, bc_dcmpg = 0;
      for (Branch b : BranchPool
          .getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT())
          .getAllBranches()) {
        int branchOpCode = b.getInstruction().getASMNode().getOpcode();
        int previousOpcode = -2;
        if (b.getInstruction().getASMNode().getPrevious() != null) {
          previousOpcode = b.getInstruction().getASMNode().getPrevious().getOpcode();
        }
        switch (previousOpcode) {
          case Opcodes.LCMP:
            bc_lcmp++;
            break;
          case Opcodes.FCMPL:
            bc_fcmpl++;
            break;
          case Opcodes.FCMPG:
            bc_fcmpg++;
            break;
          case Opcodes.DCMPL:
            bc_dcmpl++;
            break;
          case Opcodes.DCMPG:
            bc_dcmpg++;
            break;
        }
        switch (branchOpCode) {
          // copmpare int with zero
          case Opcodes.IFEQ:
          case Opcodes.IFNE:
          case Opcodes.IFLT:
          case Opcodes.IFGE:
          case Opcodes.IFGT:
          case Opcodes.IFLE:
            cmp_intzero++;
            break;
          // copmpare int with int
          case Opcodes.IF_ICMPEQ:
          case Opcodes.IF_ICMPNE:
          case Opcodes.IF_ICMPLT:
          case Opcodes.IF_ICMPGE:
          case Opcodes.IF_ICMPGT:
          case Opcodes.IF_ICMPLE:
            cmp_intint++;
            break;
          // copmpare reference with reference
          case Opcodes.IF_ACMPEQ:
          case Opcodes.IF_ACMPNE:
            cmp_refref++;
            break;
          // compare reference with null
          case Opcodes.IFNULL:
          case Opcodes.IFNONNULL:
            cmp_refnull++;
            break;

        }
      }
      ClientServices.track(RuntimeVariable.Cmp_IntZero, cmp_intzero);
      ClientServices.track(RuntimeVariable.Cmp_IntInt, cmp_intint);
      ClientServices.track(RuntimeVariable.Cmp_RefRef, cmp_refref);
      ClientServices.track(RuntimeVariable.Cmp_RefNull, cmp_refnull);

      ClientServices.track(RuntimeVariable.BC_lcmp, bc_lcmp);
      ClientServices.track(RuntimeVariable.BC_fcmpl, bc_fcmpl);
      ClientServices.track(RuntimeVariable.BC_fcmpg, bc_fcmpg);
      ClientServices.track(RuntimeVariable.BC_dcmpl, bc_dcmpl);
      ClientServices.track(RuntimeVariable.BC_dcmpg, bc_dcmpg);

      RuntimeVariable[] bytecodeVarsCovered = new RuntimeVariable[]{RuntimeVariable.Covered_lcmp,
          RuntimeVariable.Covered_fcmpl, RuntimeVariable.Covered_fcmpg,
          RuntimeVariable.Covered_dcmpl,
          RuntimeVariable.Covered_dcmpg, RuntimeVariable.Covered_IntInt,
          RuntimeVariable.Covered_IntInt,
          RuntimeVariable.Covered_IntZero, RuntimeVariable.Covered_RefRef,
          RuntimeVariable.Covered_RefNull};

      for (RuntimeVariable bcvar : bytecodeVarsCovered) {
        ClientServices.track(bcvar,
            getBytecodeCount(bcvar, ExecutionTraceImpl.bytecodeInstructionCoveredFalse)
                + getBytecodeCount(bcvar, ExecutionTraceImpl.bytecodeInstructionCoveredTrue));
      }

      RuntimeVariable[] bytecodeVarsReached = new RuntimeVariable[]{RuntimeVariable.Reached_lcmp,
          RuntimeVariable.Reached_fcmpl, RuntimeVariable.Reached_fcmpg,
          RuntimeVariable.Reached_dcmpl,
          RuntimeVariable.Reached_dcmpg, RuntimeVariable.Reached_IntInt,
          RuntimeVariable.Reached_IntInt,
          RuntimeVariable.Reached_IntZero, RuntimeVariable.Reached_RefRef,
          RuntimeVariable.Reached_RefNull};

      for (RuntimeVariable bcvar : bytecodeVarsReached) {
        ClientServices.track(bcvar,
            getBytecodeCount(bcvar, ExecutionTraceImpl.bytecodeInstructionReached) * 2);
      }

    }

  }

  static void printTestCriterion() {
    if (Properties.CRITERION.length > 1) {
      LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier() + "Test criteria:");
    } else {
      LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier()+ "Test criterion:");
    }
    for (int i = 0; i < Properties.CRITERION.length; i++) {
      printTestCriterion(Properties.CRITERION[i]);
    }
  }

  /**
   * Prints a short description of all activated secondary objectives to the user command line
   * output.
   */
  static void printSecondaryObjectives() {
    if (Properties.SECONDARY_OBJECTIVE.length > 1) {
      LoggingUtils.getEvoLogger()
          .info("* " + ClientProcess.getPrettyPrintIdentifier() + "Secondary objectives:");
    } else {
      LoggingUtils.getEvoLogger()
          .info("* " + ClientProcess.getPrettyPrintIdentifier() + "Secondary objective:");
    }
    for (int i = 0; i < Properties.SECONDARY_OBJECTIVE.length; i++) {
      LoggingUtils.getEvoLogger()
          .info("  - " + getSecondaryObjectiveDescription(Properties.SECONDARY_OBJECTIVE[i]));
    }
  }

  static TestGenerationStrategy getTestGenerationStrategy() {
    switch (Properties.STRATEGY) {
    case EVOSUITE:
      return new WholeTestSuiteStrategy();
    case RANDOM:
      return new RandomTestStrategy();
    case RANDOM_FIXED:
      return new FixedNumRandomTestStrategy();
    case ONEBRANCH:
      return new IndividualTestStrategy();
    case REGRESSION:
      return new RegressionSuiteStrategy();
    case ENTBUG:
      return new EntBugTestStrategy();
    case MOSUITE:
      return new MOSuiteStrategy();
    case DSE:
      return new DSEStrategy();
    case NOVELTY:
      return new NoveltyStrategy();
    default:
      throw new RuntimeException("Unsupported strategy: " + Properties.STRATEGY);
    }
  }

  public static void addAssertions(TestSuiteChromosome tests) {
    AssertionGenerator asserter;
    ContractChecker.setActive(false);

    if (Properties.ASSERTION_STRATEGY == AssertionStrategy.MUTATION) {
      asserter = new SimpleMutationAssertionGenerator();
    } else if (Properties.ASSERTION_STRATEGY == AssertionStrategy.ALL) {
      asserter = new CompleteAssertionGenerator();
    } else
      asserter = new UnitAssertionGenerator();

    asserter.addAssertions(tests);

    if (Properties.FILTER_ASSERTIONS)
      asserter.filterFailingAssertions(tests);
  }
}
