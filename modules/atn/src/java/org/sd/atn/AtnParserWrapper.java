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


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.token.StandardTokenizerOptions;
import org.sd.util.Usage;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;

/**
 * A container for an ATN Parser with its options and strategies.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes = "A container for an org.sd.atn.AtnParser with its options and strategies.")
public class AtnParserWrapper {
  
  public static AtnParserWrapper buildInstance(DomElement parserElement, ResourceManager resourceManager) {
    final String parserFileName = parserElement.getAttributeValue("file", null);
    if (parserFileName != null) {
      final File parserFile = resourceManager.getOptions().getWorkingFile(parserFileName, "workingDir");
      System.out.println(new Date() + ": AtnParserWrapper loading parserFile '" + parserFile.getAbsolutePath() + "'.");
      if (parserFile.exists()) {
        try {
          parserElement = (DomElement)XmlFactory.loadDocument(parserFile, false, resourceManager.getOptions()).getDocumentElement();
        }
        catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
      else {
        throw new IllegalStateException(new Date() +
                                        ": ERROR can't find parser file in CompoundParser: '" +
                                        parserFile.getAbsolutePath() + "'");
      }
    }

    final AtnParserWrapper parserWrapper = new AtnParserWrapper(parserElement, resourceManager);
    return parserWrapper;
  }


  private String id;
  public String getId() {
    return id;
  }

  private AtnParser parser;
  public AtnParser getParser() {
    return parser;
  }

  private DomElement tokenizerOverride;
  public DomElement getTokenizerOverride() {
    return tokenizerOverride;
  }

  private StandardTokenizerOptions tokenizerOptions;
  public StandardTokenizerOptions getTokenizerOptions() {
    return tokenizerOptions;
  }
  public void setTokenizerOptions(StandardTokenizerOptions tokenizerOptions) {
    this.tokenizerOptions = tokenizerOptions;
  }

  private AtnParseOptions parseOptions;
  public AtnParseOptions getParseOptions() {
    return parseOptions;
  }

  private AtnParsePrequalifier prequalifier;
  public AtnParsePrequalifier getPrequalifier() {
    return prequalifier;
  }
  public void setPrequalifier(AtnParsePrequalifier prequalifier) {
    this.prequalifier = prequalifier;
  }

  private AtnParseSelector parseSelector;
  public AtnParseSelector getParseSelector() {
    return parseSelector;
  }
  public void setParseSelector(AtnParseSelector parseSelector) {
    this.parseSelector = parseSelector;
  }

  private AmbiguityResolver ambiguityResolver;
  public AmbiguityResolver getAmbiguityResolver() {
    return ambiguityResolver;
  }

  private int minNumTokens;
  public int getMinNumTokens() {
    return minNumTokens;
  }

  private int maxWordCount;
  public int getMaxWordCount() {
    return maxWordCount;
  }

  private DomElement parserElement;
  public DomElement getParserElement() {
    return parserElement;
  }

  private DomElement prequalifierElement;
  public DomElement getPrequalifierElement() {
    return prequalifierElement;
  }

  private DomElement parseSelectorElement;
  public DomElement getParseSelectorElement() {
    return parseSelectorElement;
  }

  private DomElement ambiguityResolverElement;
  public DomElement getAmbiguityResolverElement() {
    return ambiguityResolverElement;
  }


  AtnParserWrapper(DomElement parserElement, ResourceManager resourceManager) {
    this.parserElement = parserElement;

    final DomElement idElement = (DomElement)parserElement.selectSingleNode("id");
    if (idElement != null) {
      this.id = idElement.getTextContent();
    }
    else {
      this.id = "<UNSPECIFIED>";
    }

    final DomElement grammarFileElement = (DomElement)parserElement.selectSingleNode("grammar");
    final DomElement grammarElement = AtnGrammar.getGrammarElement(grammarFileElement);

    if (grammarElement == null) {
      throw new IllegalArgumentException("Parser element must have 'grammar' child!");
    }

    this.parser = new AtnParser(grammarElement, resourceManager);

    this.prequalifierElement = (DomElement)parserElement.selectSingleNode("prequalifier");
    this.prequalifier = (prequalifierElement != null) ? (AtnParsePrequalifier)resourceManager.getResource(prequalifierElement) : null;

    this.parseSelectorElement = (DomElement)parserElement.selectSingleNode("parseSelector");
    this.parseSelector = (parseSelectorElement != null) ? (AtnParseSelector)resourceManager.getResource(parseSelectorElement) : null;

    this.ambiguityResolverElement = (DomElement)parserElement.selectSingleNode("ambiguityResolver");
    this.ambiguityResolver = (ambiguityResolverElement != null) ? (AmbiguityResolver)resourceManager.getResource(ambiguityResolverElement) : null;

    this.tokenizerOverride = (DomElement)parserElement.selectSingleNode("tokenizer");

    final DomElement tokenizerOptionsElement = (DomElement)parserElement.selectSingleNode("tokenizerOptions");
    this.tokenizerOptions = tokenizerOptionsElement == null ? new StandardTokenizerOptions() : new StandardTokenizerOptions(tokenizerOptionsElement);

    final DomElement parseOptionsElement = (DomElement)parserElement.selectSingleNode("parseOptions");
    this.parseOptions = parseOptionsElement == null ? new AtnParseOptions(resourceManager) : new AtnParseOptions(parseOptionsElement, resourceManager);

    this.minNumTokens = this.parseOptions.getAdjustInputForTokens() ? this.parser.getGrammar().computeMinNumTokens(this.parseOptions) : 1;
    this.maxWordCount = this.parser.getGrammar().computeMaxWordCount();

    // reconcile maxWordCount with tokenBreakLimit, which is considered to set a minimum for this when non-zero
    //todo: currently using maxWordCount as an estimate for tokenBreakLimit. Fix if/when this becomes a problem.
    final int tokenBreakLimit = tokenizerOptions.getTokenBreakLimit();
    if ((tokenBreakLimit == 0 && maxWordCount > 0) || (this.maxWordCount > tokenBreakLimit)) {
      tokenizerOptions.setTokenBreakLimit(maxWordCount);
    }
    System.out.println(new Date() + ": AtnParserWrapper (" + id + ") tokenBreakLimit=" + tokenizerOptions.getTokenBreakLimit());
  }

  /**
   * Convenience method to get the parser's grammar.
   */
  public AtnGrammar getGrammar() {
    AtnGrammar result = null;

    if (parser != null) {
      result = parser.getGrammar();
    }

    return result;
  }

  public List<AtnParseResult> seekAll(AtnParseBasedTokenizer tokenizer, Set<Integer> stopList,
                                      DataProperties overrides, AtomicBoolean die) {

    List<AtnParseResult> parseResults = null;
    tokenizer.setTokenizerOptions(tokenizerOptions);
    final boolean qualified = (prequalifier == null) ? true : prequalifier.prequalify(tokenizer);

    if (qualified) {
      parseResults = parser.seekAll(tokenizer, parseOptions, stopList, overrides, die);

      if (parseSelector != null && parseResults != null && parseResults.size() > 0) {
        for (AtnParseResult parseResult : parseResults) {
          parseSelector.selectParses(parseResult);
        }
      }

      // Add the parse results as tokens
      tokenizer.add(parseResults);
    }
    else {
      // return empty list when input not (pre)qualified for this parser
      parseResults = new ArrayList<AtnParseResult>();
    }

    return parseResults;
  }

  /**
   * Resolve ambiguities (if possible) within the parse results generated
   * through this wrapper.
   */
  public void resolveAmbiguities(List<AtnParseResult> parseResults) {
    if (this.ambiguityResolver == null) return;
    ambiguityResolver.resolve(parseResults);
  }
}
