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
package org.sd.cluster.config;


import org.sd.cio.MessageHelper;
import org.sd.cluster.io.Context;
import org.sd.io.Publishable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A signed response that holds a publishable object.
 * <p>
 * @author Spence Koehler
 */
public class PublishableCollectionResponse extends SignedResponse {

  private Collection<? extends Publishable> publishableCollection;

  public PublishableCollectionResponse() {
    super();
    this.publishableCollection = null;
  }

  public PublishableCollectionResponse(Context context, Collection<? extends Publishable> publishableCollection) {
    super(context);
    this.publishableCollection = publishableCollection;
  }

  public Collection<? extends Publishable> getValue() {
    return publishableCollection;
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);

    if (publishableCollection == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(publishableCollection.size());

      for (Publishable publishable : publishableCollection) {
        MessageHelper.writePublishable(dataOutput, publishable);
      }
    }
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    super.read(dataInput);

    this.publishableCollection = null;

    final int numElts = dataInput.readInt();
    if (numElts >= 0) {
      final ArrayList<Publishable> pc = new ArrayList<Publishable>();

      for (int i = 0; i < numElts; ++i) {
        pc.add(MessageHelper.readPublishable(dataInput));
      }

      this.publishableCollection = pc;
    }
  }

  public String toString() {
    return getSignature() + ": " + publishableCollection;
  }
}
