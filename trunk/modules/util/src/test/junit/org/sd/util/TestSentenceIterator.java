/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.util;


import java.util.Locale;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the SentenceIterator class.
 * <p>
 * @author Spence Koehler
 */
public class TestSentenceIterator extends TestCase {

  public TestSentenceIterator(String name) {
    super(name);
  }
  

  public void doTest(String input, String[] expected, int[][] expectedIndexes) {
    final SentenceIterator iter = new SentenceIterator(input);
    doTest(iter, expected, expectedIndexes);
  }

  public void doTest(SentenceIterator iter, String[] expected, int[][] expectedIndexes) {
    int count = 0;
    while (iter.hasNext()) {
      final String text = iter.next();
      if (expected == null) {
        System.out.println(count + "(" + iter.getStartIndex() + "," + iter.getEndIndex() + "): " + text);
      }
      else {
        assertEquals("(" + count + ")", expected[count], text);
        assertEquals(expectedIndexes[count][0], iter.getStartIndex());
        assertEquals(expectedIndexes[count][1], iter.getEndIndex());
      }
      ++count;
    }

    if (expected != null) {
      assertEquals(expected.length, count);
    }
  }

  public void testSimple() {
    doTest("This is a test. This is only a test.",
           new String[] {
             "This is a test.",
             "This is only a test.",
           },
           new int[][] {
             {0, 16},
             {16, 36},
           });
  }

  public void testBadAbbreviation1() {
    // NOTE: This doesn't work quite right. If we find that it is fixed in the
    //       future, we can set this test to rights!
    doTest("Try parsing beyond tokens like Ph.D. and Dr. Smith, if you please.",
           new String[] {
             "Try parsing beyond tokens like Ph.D. and Dr.",
             "Smith, if you please.",
           },
           new int[][] {
             {0, 45},
             {45, 66},
           });
  }

  public void testComplex1() {
    doTest("\"What is Machine Translation? Machine translation (MT) is the application of computers to the task of translating texts from one natural language to another. One of the very earliest pursuits in computer science, MT has proved to be an elusive goal, but today a number of systems are available which produce output which, if not perfect, is of sufficient quality to be useful in a number of specific domains.\" A definition from the European Association for Machine Translation (EAMT), \"an organization that serves the growing community of people interested in MT and translation tools, including users, developers, and researchers of this increasingly viable technology.\"",
           new String[] {
             "\"What is Machine Translation?",
             "Machine translation (MT) is the application of computers to the task of translating texts from one natural language to another.",
             "One of the very earliest pursuits in computer science, MT has proved to be an elusive goal, but today a number of systems are available which produce output which, if not perfect, is of sufficient quality to be useful in a number of specific domains.\"",
             "A definition from the European Association for Machine Translation (EAMT), \"an organization that serves the growing community of people interested in MT and translation tools, including users, developers, and researchers of this increasingly viable technology.\"",
           },
           new int[][] {
             {0, 30},
             {30, 158},
             {158, 410},
             {410, 671},
           });
  }

  public void testComplex2() {
    doTest("&quot;What is Machine Translation? Machine translation (MT) is the application of computers to the task of translating texts from one natural language to another. One of the very earliest pursuits in computer science, MT has proved to be an elusive goal, but today a number of systems are available which produce output which, if not perfect, is of sufficient quality to be useful in a number of specific domains.&quot; A definition from the European Association for Machine Translation (EAMT), &quot;an organization that serves the growing community of people interested in MT and translation tools, including users, developers, and researchers of this increasingly viable technology.&quot;",
           new String[] {
             "&quot;What is Machine Translation?",
             "Machine translation (MT) is the application of computers to the task of translating texts from one natural language to another.",
             "One of the very earliest pursuits in computer science, MT has proved to be an elusive goal, but today a number of systems are available which produce output which, if not perfect, is of sufficient quality to be useful in a number of specific domains.",
             "&quot; A definition from the European Association for Machine Translation (EAMT), &quot;an organization that serves the growing community of people interested in MT and translation tools, including users, developers, and researchers of this increasingly viable technology.",
             "&quot;",
           },
           new int[][] {
             {0, 35},
             {35, 163},
             {163, 413},
             {413, 685},
             {685, 691},
           });
  }

  /** Test behavior with empty input. */
  public void testEmptyInput() {
    doTest("", new String[0], null);
  }

  public void testEnglishChineseMix() {
    doTest(new SentenceIterator(
             "NAND flash 不是有寫入次數的限制嗎？ 這對 ioDrive™ 的使用壽命會有什麼影響？",
             Locale.CHINESE),
           new String[] {
             "NAND flash 不是有寫入次數的限制嗎？",
             "這對 ioDrive™ 的使用壽命會有什麼影響？"
           },
           new int[][] {
             {0, 24},
             {24, 48},
           });
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestSentenceIterator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
