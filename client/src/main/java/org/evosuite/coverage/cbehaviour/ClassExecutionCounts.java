package org.evosuite.coverage.cbehaviour;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.evosuite.coverage.cbehaviour.ClassExecutionCounts.Method.Line;

@SuppressWarnings("unused")
public class ClassExecutionCounts {

  public String className;
  public List<Method> methods;

  public static List<ClassExecutionCounts> readCounts(String json) {
    Type listType = new TypeToken<List<ClassExecutionCounts>>() {
    }.getType();
    return new Gson().fromJson(json, listType);
  }

  public int totalCount() {
    return lineList().stream().mapToInt(line -> line.count).sum();
  }

  public List<Line> lineList() {
    return methods.stream().flatMap(method -> method.executionCounts.stream()).collect(Collectors.toList());
  }

  public int weightOfLines(Set<Integer> lineNumbers) {
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
