package org.evosuite.coverage.execcount;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import org.evosuite.coverage.execcount.ClassExecutionCounts.Method;
import org.evosuite.coverage.execcount.ClassExecutionCounts.Method.Line;
import org.junit.Test;

/**
 * Contains JUnit tests for the {@link ClassExecutionCounts} class. It reads input stored as files
 * for testing.
 */
public class ClassExecutionCountsTest {

  @Test
  public void readOneLineCount() {
    String inputJson = null;
    try {
      inputJson = new Scanner(
          new File("src/test/resources/one-line-count.json"))
          .useDelimiter("\\Z").next();
    } catch (FileNotFoundException e) {
      assert false : "file should be present";
    }

    List<ClassExecutionCounts> loadedCounts = ClassExecutionCounts.readCounts(inputJson);
    List<ClassExecutionCounts> expectedCounts = Collections.singletonList(
        new ClassExecutionCounts(
            "org.apache.commons.math3.geometry.euclidean.threed.Line", Collections.singletonList(
            new Method("reset", Collections.singletonList(
                new Line(97, 1)
            ))
        ))
    );
    assertEquals(expectedCounts, loadedCounts);
  }

  @Test
  public void readMultiMethodAndLineCount() {
    String inputJson = null;
    try {
      inputJson = new Scanner(
          new File("src/test/resources/multi-method-and-line-count.json"))
          .useDelimiter("\\Z").next();
    } catch (FileNotFoundException e) {
      assert false : "file should be present";
    }

    List<ClassExecutionCounts> loadedCounts = ClassExecutionCounts.readCounts(inputJson);
    List<ClassExecutionCounts> expectedCounts = Collections.singletonList(
        getMultiMethodAndLineExecutionCounts()
    );
    assertEquals(expectedCounts, loadedCounts);
  }

  @Test
  public void readMultiClassCount() {
    String inputJson = null;
    try {
      inputJson = new Scanner(
          new File("src/test/resources/multi-class-count.json"))
          .useDelimiter("\\Z").next();
    } catch (FileNotFoundException e) {
      assert false : "file should be present";
    }

    List<ClassExecutionCounts> loadedCounts = ClassExecutionCounts.readCounts(inputJson);
    List<ClassExecutionCounts> expectedCounts = Arrays.asList(
        new ClassExecutionCounts(
            "org.jabref.gui.DefaultInjector", Collections.singletonList(
            new Method("injectMembers", Collections.singletonList(
                new Line(73, 3)
            ))
        )),
        new ClassExecutionCounts("org.jabref.gui.JabRefDialogService", Collections.singletonList(
            new Method("notify", Collections.singletonList(
                new Line(274, 5)
            ))
        )),
        new ClassExecutionCounts("org.jabref.gui.util.ThemeLoader", Collections.singletonList(
            new Method("addAndWatchForChanges", Arrays.asList(
                new Line(94, 1),
                new Line(99, 1)
            ))
        )),
        new ClassExecutionCounts("org.jabref.logic.citationstyle.CitationStyle",
            Collections.singletonList(
                new Method("createCitationStyleFromFile", Collections.singletonList(
                    new Line(98, 1)
                ))
            ))
    );
    assertEquals(expectedCounts, loadedCounts);
  }

  @Test
  public void totalCountEmpty() {
    ClassExecutionCounts counts = getEmptyExecutionCounts();
    assertEquals(0, counts.totalCount());
  }

  @Test
  public void totalCountOneLine() {
    ClassExecutionCounts counts = getOneLineExecutionCounts();
    assertEquals(4, counts.totalCount());
  }

  @Test
  public void totalCountMultiMethodAndLine() {
    ClassExecutionCounts counts = getMultiMethodAndLineExecutionCounts();
    assertEquals(11, counts.totalCount());
  }

  @Test
  public void lineListEmpty() {
    ClassExecutionCounts counts = getEmptyExecutionCounts();
    assertEquals(Collections.emptyList(), counts.lineList());
  }

  @Test
  public void lineListOneLine() {
    ClassExecutionCounts counts = getOneLineExecutionCounts();
    assertEquals(Collections.singletonList(new Line(97, 4)), counts.lineList());
  }

  @Test
  public void lineListMultiMethodAndLine() {
    ClassExecutionCounts counts = getMultiMethodAndLineExecutionCounts();

    assertEquals(Arrays.asList(
        new Line(109, 2),
        new Line(97, 5),
        new Line(99, 1),
        new Line(117, 3)
    ), counts.lineList());
  }

  @Test
  public void numberOfExecutionsEmpty() {
    ClassExecutionCounts counts = getEmptyExecutionCounts();
    assertEquals(0, counts.numberOfExecutions(new HashSet<>(Arrays.asList(2, 4, 3))));
  }

  @Test
  public void numberOfExecutionsOneLineMatch() {
    ClassExecutionCounts counts = getOneLineExecutionCounts();
    assertEquals(4, counts.numberOfExecutions(Collections.singleton(97)));
  }

  @Test
  public void numberOfExecutionsOneLineNoMatch() {
    ClassExecutionCounts counts = getOneLineExecutionCounts();
    assertEquals(0, counts.numberOfExecutions(Collections.singleton(98)));
  }

  @Test
  public void numberOfExecutionsMultiMethodAndLineNoneMatch() {
    ClassExecutionCounts counts = getMultiMethodAndLineExecutionCounts();
    assertEquals(0, counts.numberOfExecutions(new HashSet<>(Arrays.asList(98, 108, 118, 200, 1))));
  }

  @Test
  public void numberOfExecutionsMultiMethodAndLineSomeMatch() {
    ClassExecutionCounts counts = getMultiMethodAndLineExecutionCounts();
    assertEquals(3, counts.numberOfExecutions(new HashSet<>(Arrays.asList(99, 109, 118, 200, 1))));
  }

  @Test
  public void numberOfExecutionsMultiMethodAndLineAllMatch() {
    ClassExecutionCounts counts = getMultiMethodAndLineExecutionCounts();
    assertEquals(11, counts.numberOfExecutions(new HashSet<>(Arrays.asList(99, 97, 117, 109))));
  }

  private ClassExecutionCounts getEmptyExecutionCounts() {
    return new ClassExecutionCounts(
        "org.apache.commons.math3.geometry.euclidean.threed.Line", Collections.singletonList(
        new Method("reset", Collections.emptyList())
    ));
  }

  private ClassExecutionCounts getOneLineExecutionCounts() {
    return new ClassExecutionCounts(
        "org.apache.commons.math3.geometry.euclidean.threed.Line", Collections.singletonList(
        new Method("reset", Collections.singletonList(
            new Line(97, 4)
        ))
    ));
  }

  private ClassExecutionCounts getMultiMethodAndLineExecutionCounts() {
    return new ClassExecutionCounts(
        "org.apache.commons.math3.geometry.euclidean.threed.Line", Arrays.asList(
        new Method("getTolerance", Collections.singletonList(
            new Line(109, 2)
        )),
        new Method("reset", Arrays.asList(
            new Line(97, 5),
            new Line(99, 1)
        )),
        new Method("revert", Collections.singletonList(
            new Line(117, 3)
        ))
    ));
  }
}
