package org.evosuite.coverage.cbehaviour;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.evosuite.coverage.cbehaviour.ClassExecutionCounts.Method.Line;

/**
 * The data structure for storing the execution counts, corresponding to the JSON structure of the
 * input file. This class is used by gson to deserialize the input file.
 */
@SuppressWarnings("unused")
public class ClassExecutionCounts {

  public String className;
  public List<Method> methods;

  /**
   * Converts a JSON string to the structure defined in this class using gson.
   */
  public static List<ClassExecutionCounts> readCounts(String json) {
    Type listType = new TypeToken<List<ClassExecutionCounts>>() {
    }.getType();
    return new Gson().fromJson(json, listType);
  }

  /**
   * Returns the total amount of log messages in the imported log file.
   */
  public int totalCount() {
    return lineList().stream().mapToInt(line -> line.count).sum();
  }

  /**
   * Creates a flattened list of all log messages for a specific class, removing the categorization
   * by method.
   */
  public List<Line> lineList() {
    return methods.stream().flatMap(method -> method.executionCounts.stream()).collect(Collectors.toList());
  }

  /**
   * Computes the total number of executions over the given set of lines.
   */
  public int numberOfExecutions(Set<Integer> lineNumbers) {
    return lineList().stream().filter(line -> lineNumbers.contains(line.line)).mapToInt(line -> line.count).sum();
  }

  @SuppressWarnings("unused")
  public static class Method {

    public String methodName;
    public List<Line> executionCounts;

    @SuppressWarnings("unused")
    public static class Line {

      public int line;
      public int count;
    }
  }
}
