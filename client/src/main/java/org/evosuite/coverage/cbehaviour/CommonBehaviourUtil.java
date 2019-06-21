package org.evosuite.coverage.cbehaviour;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.evosuite.coverage.line.LineCoverageTestFitness;


class CommonBehaviourUtil {

  /**
   * Filter the list that is provided by the LineCoverageFactory so only lines are retained that
   * appear in the input execution counts. In more low-level terms: for each line goal, it is first
   * checked whether it corresponds to a class present in the execution counts, then whether it
   * corresponds to a method present in the execution counts, and finally whether it corresponds to
   * a line present in the execution counts. If all three are true, the line goal is retained,
   * otherwise it is discarded.
   */
  public static List<LineCoverageTestFitness> retainExecutedLines(
      List<LineCoverageTestFitness> lines,
      List<ClassExecutionCounts> executionCounts) {
    return lines.stream()
        .filter(line -> executionCounts.stream().anyMatch(
            count -> line.getClassName().equals(count.getClassName()) && count.getMethods().stream()
                .anyMatch(method ->
                    line.getMethod().split(Pattern.quote("("))[0].equals(method.getMethodName())
                        && method.getExecutionCounts().stream()
                        .anyMatch(executionCount ->
                            line.getLine() == executionCount.getLineNumber()
                        )))).collect(Collectors.toList());
  }
}
