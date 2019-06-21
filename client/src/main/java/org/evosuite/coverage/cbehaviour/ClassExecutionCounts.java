package org.evosuite.coverage.cbehaviour;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.evosuite.coverage.cbehaviour.ClassExecutionCounts.Method.Line;

/**
 * The data structure for storing the execution counts, corresponding to the JSON structure of the
 * input file. This class is used by gson to deserialize the input file.
 */
@SuppressWarnings("unused")
public class ClassExecutionCounts implements Serializable {

  /**
   * Fully qualified class name
   */
  private final String className;
  private final List<Method> methods;

  /**
   * Constructs an execution counts object for the given class using the given list of counts for
   * methods in the class.
   *
   * @param className fully qualified class name
   */
  public ClassExecutionCounts(String className,
      List<Method> methods) {
    if (className == null) {
      throw new IllegalArgumentException("className parameter must not be null");
    }
    if (methods == null) {
      throw new IllegalArgumentException("methods parameter must not be null");
    }

    this.className = className;
    this.methods = methods;
  }

  /**
   * Constructs an execution counts object without any counts for the given class name.
   *
   * @param className fully qualified class name
   */
  public ClassExecutionCounts(String className) {
    this(className, Collections.emptyList());
  }

  /**
   * Returns the fully qualified class name of the class for which counts are stored.
   */
  public String getClassName() {
    return className;
  }

  public List<Method> getMethods() {
    return methods;
  }

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
    return methods.stream().flatMap(method -> method.executionCounts.stream())
        .collect(Collectors.toList());
  }

  /**
   * Computes the total number of executions over the given set of lines.
   */
  public int numberOfExecutions(Set<Integer> lineNumbers) {
    return lineList().stream().filter(line -> lineNumbers.contains(line.lineNumber))
        .mapToInt(line -> line.count).sum();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClassExecutionCounts that = (ClassExecutionCounts) o;
    return className.equals(that.className) &&
        methods.equals(that.methods);
  }

  @Override
  public int hashCode() {
    return Objects.hash(className, methods);
  }

  @Override
  public String toString() {
    return "ClassExecutionCounts{" +
        "className='" + className + '\'' +
        ", methods=" + methods +
        '}';
  }

  @SuppressWarnings("unused")
  public static class Method implements Serializable {

    /**
     * Plain method name without parentheses and arguments
     */
    private final String methodName;
    private final List<Line> executionCounts;

    /**
     * Instantiates a method with provided method name and execution counts for separate lines.
     * @param methodName simple method name without parentheses or arguments
     */
    public Method(String methodName,
        List<Line> executionCounts) {
      if (methodName == null) {
        throw new IllegalArgumentException("methodName parameter must not be null");
      }
      if (executionCounts == null) {
        throw new IllegalArgumentException("executionCounts parameter must not be null");
      }

      this.methodName = methodName;
      this.executionCounts = executionCounts;
    }

    /**
     * Returns the plain method name, without parentheses or arguments.
     */
    public String getMethodName() {
      return methodName;
    }

    public List<Line> getExecutionCounts() {
      return executionCounts;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Method method = (Method) o;
      return methodName.equals(method.methodName) &&
          executionCounts.equals(method.executionCounts);
    }

    @Override
    public int hashCode() {
      return Objects.hash(methodName, executionCounts);
    }

    @Override
    public String toString() {
      return "Method{" +
          "methodName='" + methodName + '\'' +
          ", executionCounts=" + executionCounts +
          '}';
    }

    @SuppressWarnings("unused")
    public static class Line implements Serializable {

      private final int lineNumber;
      private final int count;

      public Line(int lineNumber, int count) {
        if (lineNumber <= 0) {
          throw new IllegalArgumentException("lineNumber parameter must be larger than 0");
        }
        if (count <= 0) {
          throw new IllegalArgumentException("count parameter must be larger than 0");
        }

        this.lineNumber = lineNumber;
        this.count = count;
      }

      public int getLineNumber() {
        return lineNumber;
      }

      public int getCount() {
        return count;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (o == null || getClass() != o.getClass()) {
          return false;
        }
        Line line1 = (Line) o;
        return lineNumber == line1.lineNumber &&
            count == line1.count;
      }

      @Override
      public int hashCode() {
        return Objects.hash(lineNumber, count);
      }

      @Override
      public String toString() {
        return "Line{" +
            "lineNumber=" + lineNumber +
            ", count=" + count +
            '}';
      }
    }
  }
}
