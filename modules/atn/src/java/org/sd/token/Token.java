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
package org.sd.token;


import java.io.Serializable;
import java.util.List;

/**
 * Container for a token.
 * <p>
 * @author Spence Koehler
 */
public class Token {
  
  private Tokenizer tokenizer;
  /**
   * This Token's tokenizer.
   */
  public Tokenizer getTokenizer() {
    return tokenizer;
  }

  private Features features;
  /**
   * This token's features.
   */
  public Features getFeatures() {
    return features;
  }
  public boolean hasFeatures() {
    return features != null && features.size() > 0;
  }

  private String text;
  /**
   * This token's full original text.
   */
  public String getText() {
    return text;
  }

  private String _preDelim;
  public String getPreDelim() {
    if (_preDelim == null) {
      _preDelim = tokenizer.getPreDelim(this);
    }
    return _preDelim;
  }

  private String _postDelim;
  public String getPostDelim() {
    if (_postDelim == null) {
      _postDelim = tokenizer.getPostDelim(this);
    }
    return _postDelim;
  }

  private String _textWithDelims = null;
  public String getTextWithDelims() {
    if (_textWithDelims == null) {
      final StringBuilder builder = new StringBuilder();

      // get token text
      builder.append(text);

      // prepend immediate pre-delims
      final String preDelim = getPreDelim();
      final int preDelimLen = preDelim.length();
      if (preDelimLen == startIndex) {
        // this is the first token, so include all delims
        builder.insert(0, preDelim);
      }
      else {
        for (int idx = preDelimLen - 1; idx >= 0; --idx) {
          final char delim = preDelim.charAt(idx);
          if (delim == ' ') {
            // insert all preDelims back to but not including a space
            builder.insert(0, preDelim.substring(idx + 1));
          }
          // if there is no "space" preDelim, then don't consider any preDelims part of this token
          // assume they are part of the postDelims of the prior token.
          else if (Character.isLetterOrDigit(delim)) break;
        }
      }

      // append immediate post-delims
      final String postDelim = getPostDelim();
      final int postLen = postDelim.length();
      for (int idx = 0; idx < postLen; ++idx) {
        final char delim = postDelim.charAt(idx);
        if (delim == ' ' || Character.isLetterOrDigit(delim)) break;
        builder.append(delim);
      }

      _textWithDelims = builder.toString();
    }
    return _textWithDelims;
  }

  private int startIndex;
  /**
   * The index of this token's start in the full text from which this token came.
   */
  public int getStartIndex() {
    return startIndex;
  }

  /**
   * The index just beyond this token's end in the full text from which this token came.
   */
  public int getEndIndex() {
    return startIndex + getLength();
  }

  /**
   * Get the number of characters in this token's full original text.
   */
  public int getLength() {
    return text == null ? 0 : text.length();
  }

  private TokenRevisionStrategy revisionStrategy;
  /**
   * This token's revision strategy.
   */
  public TokenRevisionStrategy getRevisionStrategy() {
    return revisionStrategy;
  }

  private int revisionNumber;
  /**
   * This token's revision number.
   */
  public int getRevisionNumber() {
    return revisionNumber;
  }

  private int sequenceNumber;
  /**
   * The 0-based sequence number of this token.
   */
  public int getSequenceNumber() {
    return sequenceNumber;
  }

  private int wordCount;
  public int getWordCount() {
    return wordCount;
  }

  /**
   * Construct a new token with the given text. Usually constructed by a Tokenizer.
   */
  Token(Tokenizer tokenizer, String text, int startIndex, TokenRevisionStrategy revisionStrategy, int revisionNumber, int sequenceNumber, int wordCount) {
			this.tokenizer = tokenizer;
			this.features = null;
			this.text = text;
			this.startIndex = startIndex;
			this.revisionStrategy = revisionStrategy;
			this.revisionNumber = revisionNumber;
			this.sequenceNumber = sequenceNumber;
			this.wordCount = wordCount;

      this._preDelim = tokenizer.initializing() ? null : tokenizer.getPreDelim(this);
      this._postDelim = tokenizer.initializing() ? null : tokenizer.getPostDelim(this);
		}

  /**
   * Assuming the token comes from the same tokenizer, determine whether this
   * token fully encompasses the other.
   */
  public boolean encompasses(Token other) {
    return encompasses(getStartIndex(), getEndIndex(), other.getStartIndex(), other.getEndIndex());
  }

  /**
   * Assuming the token comes from the same tokenizer, determine whether this
   * token overlaps (including fully encompassing) the other.
   */
  public boolean overlaps(Token other) {
    return overlaps(getStartIndex(), getEndIndex(), other.getStartIndex(), other.getEndIndex());
  }

  /**
   * Utility method encoding encompass logic.
   */
  public static boolean encompasses(int myStartIndex, int myEndIndex, int otherStartIndex, int otherEndIndex) {
    return otherStartIndex >= myStartIndex && otherEndIndex <= myEndIndex;
  }

  /**
   * Utility method encoding overlap logic.
   */
  public static boolean overlaps(int myStartIndex, int myEndIndex, int otherStartIndex, int otherEndIndex) {
    return
      (myStartIndex >= otherStartIndex && myStartIndex < otherEndIndex) ||
      (myEndIndex <= otherEndIndex && myEndIndex > otherStartIndex);
  }

  /**
   * Convenience method for setting a feature on this token with a
   * probability of 1.
   */
  public Feature setFeature(String type, Serializable value, Object source) {
    Feature result = new Feature(type, value, 1.0, source);
    if (this.features == null) this.features = new Features();
    this.features.add(result);
    return result;
  }

  /**
   * Set this token's features.
   * <p>
   * Note: all existing features will be replaced by the given features.
   */
  public void setFeatures(Features features) {
    this.features = features;
  }

  /**
   * Convenience method for getting the feature's value.
   */
  public Object getFeatureValue(String type, Object source) {
    return getFeatureValue(type, source, null);
  }

  /**
   * Convenience method for getting the feature.
   */
  public Feature getFeature(String type, Object source) {
    return getFeature(type, source, null);
  }

  /**
   * Convenience method for getting the feature's value.
   */
  public Object getFeatureValue(String type, Object source, Class featureValueType) {
    Feature feature = getFeature(type, source, featureValueType);
    return feature == null ? null : feature.getValue();
  }

  /**
   * Convenience method for getting the feature.
   */
  public Feature getFeature(String type, Object source, Class featureValueType) {
    Feature result = null;

    if (this.features != null) {
      final FeatureConstraint constraint = FeatureConstraint.getInstance(type, source, featureValueType);
      result = features.getFirst(constraint);
    }

    return result;
  }

  /**
   * Convenience method for getting the features.
   */
  public List<Feature> getFeatures(String type, Object source, Class featureValueType) {
    List<Feature> result = null;

    if (this.features != null) {
      final FeatureConstraint constraint = FeatureConstraint.getInstance(type, source, featureValueType);
      result = features.getFeatures(constraint);
    }

    return result;
  }

  /**
   * Determine whether this token follows a hard break.
   * <p>
   * A token follows a hard break if there is a hard break among the token's
   * preDelim characters. Therefore, the first token of a string is *not*
   * considered to follow a hard break.
   */
  public boolean followsHardBreak() {
    return tokenizer.followsHardBreak(this);
  }

  public String toString() {
    StringBuilder result = new StringBuilder();

    result.
      append("'").
      append(getText()).
      append("'[").
      append(getStartIndex()).
      append(',').
      append(getEndIndex()).
      append('{').
      append(getWordCount()).
      append("}](").
      append(getSequenceNumber()).
      append('.').
      append(getRevisionNumber()).
      append(')');
			
    return result.toString();
  }

  private boolean computedPrevToken = false;
  private Token _prevToken;
  public Token getPrevToken() {

    if (!computedPrevToken) {
      _prevToken = tokenizer.getPriorToken(this);
      computedPrevToken = true;

      if (_prevToken != null && !_prevToken.computedNextToken) {
        _prevToken._nextToken = this;
        _prevToken.computedNextToken = true;
      }
    }
    return _prevToken;
  }


  private boolean computedNextToken = false;
  private Token _nextToken;
  public Token getNextToken() {
    if (!computedNextToken) {
      _nextToken = tokenizer.getNextToken(this);
      computedNextToken = true;

      if (_nextToken != null && !_nextToken.computedPrevToken) {
        _nextToken._prevToken = this;
        _nextToken.computedPrevToken = true;
      }
    }
    return _nextToken;
  }


  private boolean computedRevisedToken = false;
  private Token _revisedToken;
  public Token getRevisedToken() {

    if (!computedRevisedToken) {
      _revisedToken = tokenizer.revise(this);
      computedRevisedToken = true;
    }
    return _revisedToken;
  }

  public Token getNextSmallestToken() {
    return tokenizer.getNextSmallestToken(this);
  }

  private boolean computedSmallestToken = false;
  private Token _smallestToken;
  public Token getSmallestToken() {
    if (!computedSmallestToken) {
      _smallestToken = tokenizer.getSmallestToken(startIndex);
      computedSmallestToken = true;
    }
    return _smallestToken;
  }
}
