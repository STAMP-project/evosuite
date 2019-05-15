package org.evosuite.coverage.cbehaviour;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

@SuppressWarnings("unused")
public class ClassExecutionCounts {

  public String className;
  public List<Method> methods;

  public static List<ClassExecutionCounts> readCounts(String json) {
    Type listType = new TypeToken<List<ClassExecutionCounts>>() {
    }.getType();
    return new Gson().fromJson(json, listType);
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
