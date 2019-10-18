package org.evosuite;

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import org.evosuite.coverage.execcount.ClassExecutionCounts;
import org.evosuite.coverage.execcount.ClassExecutionCounts.Method.Line;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that manages execution counts originating from a file in the specified format. //TODO
 * specify format This class handles the matching of execution counts for specific lines to specific
 * code segments in the Control Flow Graph (CFG). It provides methods for getting execution count
 * information for specific lines. The file containing the execution counts and the {@link
 * GraphPool} have to be provided.
 */
public class ExecutionCountManager implements Serializable {

  protected static final Logger logger = LoggerFactory.getLogger(ExecutionCountManager.class);

  /**
   * The execution count manager for the current target class (CUT)
   */
  private static ExecutionCountManager targetClassExecutionCountManager;

  /**
   * Provides a mapping from single source code lines of the class to the corresponding {@link
   * BasicBlock} (code segment in the CFG). All code lines are in this map after initialization.
   */
  private Map<Integer, BasicBlock> lineNumberToBasicBlock;

  /**
   * Provides a mapping from basic blocks (code segments) to how many times that segment has been
   * executed according to the provided data in the execution count file.
   */
  private Map<BasicBlock, Integer> basicBlockToExecutionCount;

  /**
   * Constructs a new {@code ExecutionCountManager} using the provided execution count file and
   * {@code GraphPool}.
   *
   * @param file execution count file in the specified format
   * @param graphPool {@code GraphPool} for the same class as the execution count file
   */
  public ExecutionCountManager(File file, GraphPool graphPool) {
    lineNumberToBasicBlock = new HashMap<>();
    basicBlockToExecutionCount = new HashMap<>();

    initialize(file, graphPool);
  }

  /**
   * Returns the execution count of the code segment that the provided line belongs to. Note that in
   * certain cases, this may not give the actual amount of times this line was executed. This may
   * happen in case of a crash in the middle of executing a code segment, for example.
   */
  public int lineExecCount(int lineNumber) {
    return basicBlockToExecutionCount.get(lineNumberToBasicBlock.get(lineNumber));
  }

  /**
   * Returns the average of the execution counts of all code segments that contain at least one of
   * the provided lines.
   */
  public double avgExecCount(Set<Integer> lines) {
    Set<BasicBlock> executedBlocks = new HashSet<>();
    for (Integer line : lines) {
      executedBlocks.add(lineNumberToBasicBlock.get(line));
    }

    return executedBlocks.stream().mapToInt(
        block -> basicBlockToExecutionCount.get(block)).average().orElse(0d);
  }

  /**
   * Returns the average of the execution counts of all code segments that contain at least one of
   * the provided lines, weighted by the weights that are provided for each provided line.
   *
   * @param lineWeights a map from line numbers to weights for each line number which are to be used
   * in computing the returned average.
   */
  public double weightedAvgExecutionCount(Map<Integer, Double> lineWeights) {
    Map<BasicBlock, Double> blockWeights = new HashMap<>();
    for (Integer line : lineWeights.keySet()) {
      blockWeights.put(lineNumberToBasicBlock.get(line), lineWeights.get(line));
    }

    return blockWeights.keySet().stream().mapToDouble(
        block -> basicBlockToExecutionCount.get(block) * blockWeights.get(block))
        .average().orElse(0d);
  }

  /**
   * Set the {@code ExecutionCountManager} for the current target class (CUT)
   */
  public static void setTargetClassExecutionCountManager(ExecutionCountManager manager) {
    targetClassExecutionCountManager = manager;
  }

  /**
   * Returns the {@code ExecutionCountManager} that is set for the current target class (CUT). This
   * method may only be called after the {@code ExecutionCountManager} has been set using the {@link
   * ExecutionCountManager#setTargetClassExecutionCountManager(ExecutionCountManager)}.
   */
  public static ExecutionCountManager getTargetClassExecutionCountManager() {
    assert (targetClassExecutionCountManager != null);
    return targetClassExecutionCountManager;
  }

  /**
   * Initialize this instance by loading all basic blocks (code segments) and corresponding line
   * numbers from the provided {@code GraphPool} and by loading execution counts from the provided
   * file.
   */
  private void initialize(File file, GraphPool graphPool) {
    loadCodeSegments(graphPool);
    processExecutionCounts(loadExecutionCountFile(file));
  }

  /**
   * Loads all source code line numbers and basic blocks (code segments) for the CUT using the
   * provided graph pool. A mapping is created between line numbers and basic blocks for efficiency
   * purposes.
   */
  private void loadCodeSegments(GraphPool graphPool) {
    Set<Map<String, RawControlFlowGraph>> allCFGs =
        graphPool.getRawCFGsWithInner(Properties.TARGET_CLASS);
    Set<BasicBlock> basicBlocks = new HashSet<>();

    for (Map<String, RawControlFlowGraph> rawClassGraphs : allCFGs) {
      for (RawControlFlowGraph rawMethodGraph : rawClassGraphs.values()) {
        for (BytecodeInstruction instruction : rawMethodGraph.vertexSet()) {
          basicBlocks.add(
              graphPool.getActualCFG(rawMethodGraph.getClassName(), rawMethodGraph.getMethodName())
                  .getBlockOf(instruction));
        }
      }
    }

    for (BasicBlock block : basicBlocks) {
      for (int i = block.getFirstLine(); i <= block.getLastLine(); i++) {
        lineNumberToBasicBlock.put(i, block);
      }
    }
  }

  /**
   * Execution counts are loaded from the provided structure and a mapping is created from {@code
   * BasicBlock}s to how many times they have been executed.
   */
  private void processExecutionCounts(ClassExecutionCounts executionCounts) {
    for (Line line : executionCounts.lineList()) {
      BasicBlock block = lineNumberToBasicBlock.get(line.getLineNumber());
      basicBlockToExecutionCount.put(block, line.getCount());
    }
    // If the basic block does not appear in the loaded execution counts, its execution count is
    // apparently 0.
    for (BasicBlock block : lineNumberToBasicBlock.values()) {
      if (!basicBlockToExecutionCount.containsKey(block)) {
        basicBlockToExecutionCount.put(block, 0);
      }
    }
  }

  /**
   * Loads execution count from the provided file, which should be in the specified JSON format.
   */
  private ClassExecutionCounts loadExecutionCountFile(File file) {
    if (!file.exists()) {
      throw new IllegalArgumentException("Input file does not exist: " + file.getAbsolutePath());
    }
    try {
      return ClassExecutionCounts
          .readCounts(new Scanner(file).useDelimiter("\\Z").next()).stream()
          .filter(classCounts -> classCounts.getClassName().equals(
              Properties.TARGET_CLASS)).findAny().orElseGet(() -> {
                logger.warn("No execution count information found for " + Properties.TARGET_CLASS
                    + ". Using empty execution counts.");
                return new ClassExecutionCounts(Properties.TARGET_CLASS);
              }
          );
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Just checked if file exists, but not accessible anymore", e);
    } catch (JsonSyntaxException e) {
      logger.error("Execution count file malformed", e);
      throw new RuntimeException(e);
    }
  }
}
