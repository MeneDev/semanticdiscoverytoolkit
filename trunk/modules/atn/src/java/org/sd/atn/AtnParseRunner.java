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
package org.sd.atn;


import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import org.sd.io.FileUtil;
import org.sd.util.ExecUtil;
import org.sd.util.FileContext;
import org.sd.util.InputContext;
import org.sd.util.InputContextIterator;
import org.sd.util.WhitespacePolicy;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomContextIterator;
import org.sd.xml.DomContextIteratorFactory;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.DomIterationStrategy;
import org.sd.xml.DomNode;
import org.sd.xml.DomTextBlockIterationStrategy;
import org.sd.xml.DomTextIterationStrategy;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class AtnParseRunner {
  
  private enum InputUpdateStrategy { RESET, BROADEN, XML };


  protected DataProperties options;
  protected ParseConfig parseConfig;
  protected boolean verbose;

  private Map<String, ParserFlow> id2parserFlow;
  private String activeFlowSpec;
  private List<ParserFlow> activeFlow;

  public AtnParseRunner(DataProperties dataProperties) throws IOException {
    this.options = dataProperties;
    this.parseConfig = ParseConfig.buildInstance(options);
    this.verbose = false;
    this.id2parserFlow = createParserFlow();
    this.activeFlowSpec = null;
    this.activeFlow = new ArrayList<ParserFlow>();

    updateOptions();
  }

  public AtnParseOptions getParseOptions(String compoundParserId, String parserId) {
    AtnParseOptions result = null;

    if (parseConfig != null) {
      final CompoundParser compoundParser = parseConfig.getCompoundParser(compoundParserId);
      if (compoundParser != null) {
        final AtnParserWrapper parserWrapper = compoundParser.getParserWrapper(parserId);
        if (parserWrapper != null) {
          result = parserWrapper.getParseOptions();
        }
      }
    }

    return result;
  }

  /**
   * Update internal variables according to the current state of the
   * options.
   */
  public final void updateOptions() {
    setVerbose(options.getBoolean("verbose", false));
    activateParseFlow(options.getString("parseFlow", null));
  }

  public DataProperties getOptions() {
    return options;
  }

  public ParseConfig getParseConfig() {
    return parseConfig;
  }

  public boolean getVerbose() {
    return verbose;
  }

  public final void setVerbose(boolean verbose) {
    this.verbose = verbose;
    this.parseConfig.setVerbose(verbose);
  }

  public String getActiveFlowSpec() {
    return activeFlowSpec;
  }

  public List<ParserFlow> getActiveFlow() {
    return activeFlow;
  }

  /**
   * Activate parse flow given a flow specification of the form:
   * <p>
   * compoundParserId1:parserId1,parserId2,...;compoundParserId2:...
   * <p>
   * Or, more formally:
   * <p>
   * compoundParserFlowSpec1;compoundParserFlowSpec2;...
   * <p>
   * Where compoundParserFlowSpec is of the form:
   * <p>
   * compoundParserId:parserFlowSpec
   * <p>
   * Where parserFlowSpec is of the form:
   * <p>
   * parserId1,parserId2,...
   * <p>
   * If flowSpec is null, then ALL parsers will be activated.
   *
   * @return the prior active parse flow specification.
   */
  public String activateParseFlow(String flowSpec) {
    final String result = activeFlowSpec;

    this.activeFlow.clear();

    if (flowSpec == null || "".equals(flowSpec)) {
      final String[] compoundParserIds = parseConfig.getCompoundParserIds();
      for (String compoundParserId : compoundParserIds) {
        final ParserFlow parserFlow = id2parserFlow.get(compoundParserId);
        parserFlow.setActive(true);            // activate flow
        parserFlow.setActive((String[])null);  // activate all flow ids
        activeFlow.add(parserFlow);
      }
    }
    else {
      for (ParserFlow parserFlow : id2parserFlow.values()) {
        parserFlow.setActive(false);  // deactivate all flows
      }

      final String[] compoundParserFlowSpecs = flowSpec.split("\\s*;\\s*");
      for (String compoundParserFlowSpec : compoundParserFlowSpecs) {
        final String[] idSpec = compoundParserFlowSpec.split("\\s*:\\s*");
        final String id = idSpec[0];
        final String[] parserIds = (idSpec.length == 1 || "".equals(idSpec[1])) ? null : idSpec[1].split("\\s*,\\s*");

        final ParserFlow parserFlow = id2parserFlow.get(id);
        if (parserFlow != null) {
          parserFlow.setActive(true);      // activate flow
          parserFlow.setActive(parserIds); // activate only specified flow ids
          activeFlow.add(parserFlow);
        }
      }
    }

    return result;
  }

  public boolean isActive(String compoundParserId) {
    return id2parserFlow.get(compoundParserId).isActive();
  }

  public boolean isActive(String compoundParserId, String parserId) {
    final ParserFlow parserFlow = id2parserFlow.get(compoundParserId);
    return parserFlow.isActive(parserId);
  }

  private final Map<String, ParserFlow> createParserFlow() {
    final Map<String, ParserFlow> result = new HashMap<String, ParserFlow>();

    final String[] compoundParserIds = parseConfig.getCompoundParserIds();
    for (String compoundParserId : compoundParserIds) {
      final CompoundParser compoundParser = parseConfig.getCompoundParser(compoundParserId);
      final String[] parserIds = compoundParser.getParserIds();
      final ParserFlow parserFlow = new ParserFlow(compoundParserId, parserIds);
      result.put(compoundParserId, parserFlow);
    }

    return result;
  }


  public void close() {
    parseConfig.close();
  }

  public void run() throws IOException {
    final ParseOutputCollector output = buildOutput();
    if (output != null) {
      final ExtractionGroups extractionGroups = new ExtractionGroups(output);
      handleOutput(output, extractionGroups);
    }
  }

  public ParseOutputCollector buildOutput() throws IOException {
    return buildOutput(null);
  }

  public ParseOutputCollector buildOutput(DataProperties overrides) throws IOException {
    ParseOutputCollector output = null;

    final String inputString = options.getString("inputString", null);
    if (inputString != null) {
      output = parseInputString(inputString, overrides);
    }
    else {
      final String inputLines = options.getString("inputLines", null);
      if (inputLines != null) {
        output = parseLines(new File(inputLines), overrides);
      }
      else {
        final String inputHtml = options.getString("inputHtml", null);
        if (inputHtml != null) {
          final String diffHtml = options.getString("diffHtml", null);
          output = parseHtml(new File(inputHtml), diffHtml == null ? null : new File(diffHtml), overrides);
        }
      }
    }

    if (output == null) {
      System.out.println("WARNING: AtnParseRunner.buildOutput failed to locate parsing option!");
    }

    return output;
  }

  public void handleOutput(ParseOutputCollector output, ExtractionGroups extractionGroups) throws IOException {

    final String outputXml = options.getString("outputXml", null);
    if (outputXml != null) {

      final String outputXmlName = FileUtil.buildOutputFilename(outputXml, ".xml");
      final File outputXmlFile = new File(outputXmlName);

      final boolean dumpGroups = options.getBoolean("dumpGroups", true);
      if (dumpGroups && extractionGroups != null) {
        // dump extraction groups as xml
        writeExtractionGroupsXml(outputXmlFile, output, extractionGroups);
      }
      else {
        // dump parse results as xml
        writeParseResultsXml(outputXmlFile, output);
      }
    }

    final boolean showResults = options.getBoolean("showResults", true);
    final boolean writeMarkup = options.getBoolean("writeMarkup", true);
    final boolean showMarkup = options.getBoolean("showMarkup", false);
    final boolean briefResults = options.getBoolean("briefResults", true);
    if ((showResults || briefResults || writeMarkup || showMarkup) && output != null) {
      showOutput(output, extractionGroups);
    }
  }

  private final void writeParseResultsXml(File outputXmlFile, ParseOutputCollector output) throws IOException {
    final BufferedWriter writer = FileUtil.getWriter(outputXmlFile);

    System.out.println("\nWriting xml parse results output to '" + outputXmlFile.getAbsolutePath() + "'");

    final boolean showOnlySelected = options.getBoolean("showOnlySelected", true);
    final boolean showOnlyInterpreted = options.getBoolean("showOnlyInterpreted", true);
    writer.write(output.asXml(true, showOnlySelected, showOnlyInterpreted, 0, 2));

    writer.close();
  }

  private final void writeExtractionGroupsXml(File outputXmlFile, ParseOutputCollector output, ExtractionGroups extractionGroups) throws IOException {
    System.out.println("\nWriting xml extraction groups output to '" + outputXmlFile.getAbsolutePath() + "'");

    final XmlParseOutput xmlOutput = new XmlParseOutput(output.getParseSourceInfo(), 0, 2);
    xmlOutput.addExtractionGroups(extractionGroups);
    final String xml = xmlOutput.getOutput(true);

    final BufferedWriter writer = FileUtil.getWriter(outputXmlFile);
    writer.write(xml);
    writer.close();
  }

  public void showOutput(ParseOutputCollector output, ExtractionGroups extractionGroups) throws IOException {
    if (output == null) return;

    final String source = getSourceString(output.getParseSourceInfo());

    final boolean showResults = options.getBoolean("showResults", true);
    final boolean briefResults = options.getBoolean("briefResults", true);

    final boolean showOnlyInterpreted = options.getBoolean("showOnlyInterpreted", false);
    // force executing show markup
    final List<AtnParse> parses = output.getTopInterpretedParses(showOnlyInterpreted);

    final LinkedHashSet<DomDocument> domDocuments = output.getDomDocuments();
    if (domDocuments != null && domDocuments.size() > 0) {

      final boolean showMarkup = options.getBoolean("showMarkup", false);
      final boolean writeMarkup = options.getBoolean("writeMarkup", showMarkup);

      for (DomDocument domDocument : domDocuments) {
        if (showMarkup || writeMarkup) {

          final String outputFile = writeModifiedDocument(domDocument);

          if (showMarkup) {
            ExecUtil.executeProcess(new String[]{"/usr/bin/firefox", "file://" + outputFile});
          }
        }
      }
    }

    if (showResults || briefResults) {
      if (!briefResults) {
        final boolean showInterpretations = options.getBoolean("showInterpretations", true);
        if (showInterpretations) {
          System.out.println("\nOutput: ");
          output.showParseResults(showOnlyInterpreted);
        }
      }
      else {
        // show brief results
        final boolean numberKeys = options.getBoolean("numberKeys", true);
        if (extractionGroups != null) {
          extractionGroups.showExtractionGroups(briefResults, source, numberKeys);
        }
      }
    }
  }

  private final String getSourceString(ParseSourceInfo sourceInfo) {
    String result = sourceInfo.getUrl();

    if (result == null || "".equals(result)) {
      result = sourceInfo.getInputString();
    }

    if (result == null || "".equals(result)) {
      result = "[unknown-source]";
    }

    return result;
  }

  public ParseOutputCollector parseInputString(String inputString, DataProperties overrides) throws IOException {
    final FileContext fileContext = new FileContext(new String[]{inputString}, WhitespacePolicy.HYPERTRIM);
    final ParseOutputCollector output = parseInput(fileContext.getLineIterator(), InputUpdateStrategy.RESET, null, overrides);
    final ParseSourceInfo sourceInfo = new ParseSourceInfo(inputString, false, false, false, null, null, null);
    output.setParseSourceInfo(sourceInfo);
    return output;
  }

  public ParseOutputCollector parseLines(File inputLines, DataProperties overrides) throws IOException {
    final FileContext fileContext = new FileContext(inputLines, WhitespacePolicy.HYPERTRIM);

    //todo: parameterize whether to broaden the scope of the iterator
    //      - if line records, then RESET.
    //      - if paragraphs, then BROADEN
    final InputUpdateStrategy inputUpdateStrategy = InputUpdateStrategy.RESET;

    final ParseOutputCollector output = parseInput(fileContext.getLineIterator(), inputUpdateStrategy, null, overrides);

    final ParseSourceInfo sourceInfo = new ParseSourceInfo(inputLines, false, false);
    output.setParseSourceInfo(sourceInfo);

    return output;
  }

  public ParseOutputCollector parseHtml(File inputHtml, File diffHtml, DataProperties overrides) throws IOException {
    //todo: parameterize textBlock flag
    final boolean textBlock = false;

    final DomIterationStrategy strategy = textBlock ? DomTextBlockIterationStrategy.INSTANCE : DomTextIterationStrategy.INSTANCE;

    //todo: parameterize (and implement) whether to broaden the scope of the iterator
    final InputUpdateStrategy inputUpdateStrategy = InputUpdateStrategy.XML;

    final DomContextIterator inputContextIterator = DomContextIteratorFactory.getDomContextIterator(inputHtml, diffHtml, true, strategy);
    final ParseOutputCollector result = parseInput(inputContextIterator, inputUpdateStrategy, null, overrides);

    final ParseSourceInfo sourceInfo = new ParseSourceInfo(inputHtml, true, true);
    if (diffHtml != null) sourceInfo.setDiffString(diffHtml.getAbsolutePath());
    result.setParseSourceInfo(sourceInfo);

    return result;
  }

  public ParseOutputCollector parseDomNode(DomNode domNode, boolean isHtml, ParseOutputCollector priorOutput, DataProperties overrides) {
    final DomContextIterator inputContextIterator = DomContextIteratorFactory.getDomContextIterator(domNode);
    final ParseOutputCollector result = parseInput(inputContextIterator, InputUpdateStrategy.XML, priorOutput, overrides);
    if (priorOutput == null) {
      final ParseSourceInfo sourceInfo = new ParseSourceInfo(domNode.getTextContent(), true, isHtml, false, null, null, null);
      result.setParseSourceInfo(sourceInfo);
    }
    return result;
  }

  protected ParseOutputCollector parseInput(InputContextIterator inputContextIterator, InputUpdateStrategy inputUpdateStrategy, ParseOutputCollector result, DataProperties overrides) {

    boolean didOne = false;

    for (ParserFlow parserFlow : activeFlow) {

      if (didOne) {
        // reset the inputContextIterator appropriately
        inputContextIterator = updateInput(inputContextIterator, inputUpdateStrategy, result);
      }

      result = parseConfig.parse(inputContextIterator, parserFlow.getFlowId(), parserFlow.getParserIds(true), result, overrides);
      didOne = true;
    }

    return result;
  }

  private InputContextIterator updateInput(InputContextIterator inputContextIterator, InputUpdateStrategy inputUpdateStrategy, ParseOutputCollector output) {
    InputContextIterator result = inputContextIterator;

    switch (inputUpdateStrategy) {
      case XML :
        result = DomContextIteratorFactory.getDomContextIterator(new ExtractionGroups(output).getInputNodes());
        break;

      case BROADEN :
        result = result.broaden();
        break;

      case RESET :
        result.reset();
        break;
    }

    return result;
  }

  private String writeModifiedDocument(DomDocument domDocument) throws IOException {
    final String outputFilename = buildOutputFilename(domDocument);

    System.out.println("\nWriting output to '" + outputFilename + "'...");

    ParseOutputCollector.pruneNodes(domDocument);

    final BufferedWriter writer = FileUtil.getWriter(outputFilename);
    XmlLite.writeXml(domDocument.getDocumentDomElement().asTree(), writer);
    writer.close();

    return outputFilename;
  }

  private String buildOutputFilename(DomDocument domDocument) {

    final Tree<XmlLite.Data> domTree = domDocument.getDocumentDomElement().asTree();
    final File inputFile = (File)domTree.getAttributes().get(XmlFactory.XML_FILENAME_ATTRIBUTE);

    final String inputFilename =  (inputFile != null) ? inputFile.getAbsolutePath() : "output.html";

    return FileUtil.buildOutputFilename(inputFilename, ".html");
  }


  public static void main(String[] args) throws IOException {

    //
    // Properties
    //
    //   parseConfig -- (required) path to data properties (config) file (xml)
    //   supplementalConfig -- (optional) semicolon delimited list of paths to supplemental parse config files
    //   resourcesDir -- (required) path to resources (e.g. "${HOME}/co/ancestry/resources")
    //
    //   inputLines -- path to input file whose lines are to be parsed
    //   inputHtml -- path to input html file to parse
    //   diffHtml -- path to html file to use as mask for inputHtml
    //   
    //   verbose -- (optional, default=true)
    //   trace -- (optional, default=false) true to trace/debug AtnStates
    //
    //   parseFlow -- (optional, default uses all) cpId1:pId1,...,pIdN;cpId2:...
    //                semi-colon delimited list of compound parser flows of the form:
    //                compoundParserId : parserId1, parserId2, ...
    //                If absent, then all parsers within all compound parsers will be executed.
    //
    //   showMarkup -- (optional, default=false)
    //   writeMarkup -- (optional, default=true)
    //   showInterpretations -- (optional, default=true)
    //   showOnlyInterpreted -- (optional, default=false)
    // 
    //
    //   showOnlySelected -- (optional, default=true)
    //   outputXml -- (optional) path to which xml output is to be written
    //   dumpGroups -- (optional, default=true) true to dump extraction groups instead of (raw/ungrouped) parse results
    //
    //   showResults -- (optional, default=true) true to show results on console
    //   briefResults -- (optional, default=true) true to show brief (instead of full) result output on console
    //   numberKeys -- (optional, default=true) true to show numbered group and extraction keys instead of xpaths
    //
    //   args -- (optional) path to .properties file containing options
    //

    final DataProperties dataProperties = new DataProperties(args);

    if (dataProperties.getBoolean("trace", false)) AtnState.setTrace(true);

    final AtnParseRunner runner = new AtnParseRunner(dataProperties);
    try {
      runner.run();
    }
    finally {
      runner.close();
    }
  }
}
