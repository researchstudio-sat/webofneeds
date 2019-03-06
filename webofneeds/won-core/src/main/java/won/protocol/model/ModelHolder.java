/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates a jena model for storing it in a relational db.
 */
@Entity
@Table(name = "rdf_models")
public class ModelHolder
{
  private static final int DEFAULT_BYTE_ARRAY_SIZE = 500;

  @Transient
  private final Logger logger = LoggerFactory.getLogger(getClass());

  //the URI of the model
  @Id
  @Column( name = "modelURI", unique = true)
  @Convert( converter = URIConverter.class)
  private URI uri;

  //the model as a byte array
  @Lob @Column( name = "model", nullable = false, length = 100000)
  private byte[] modelBytes;

  //for multiple accesses to model, cache it.
  @Transient
  private Model cachedModel;

  ModelHolder(){}

  public ModelHolder(final URI uri, final Model model) {
    this.uri = uri;
    setModel(model);
    this.cachedModel = model;
  }

  public URI getUri() {
    return uri;
  }

  public void setUri(final URI uri) {
    this.uri = uri;
  }

  byte[] getModelBytes() {
    return modelBytes;
  }

  void setModelBytes(final byte[] modelBytes) {
    this.modelBytes = modelBytes;
    this.cachedModel = null;
  }

  /**
   * Careful, expensive operation: writes model to string.
   * @param model
   */
  public void setModel(Model model) {
    assert this.uri != null : "uri must not be null";
    assert this.modelBytes != null : "model must not be null";
    ByteArrayOutputStream out = new ByteArrayOutputStream(DEFAULT_BYTE_ARRAY_SIZE);
    synchronized(this){
      RDFDataMgr.write(out, model, Lang.TTL);
      this.modelBytes = out.toByteArray();
      if (logger.isDebugEnabled()){
        logger.debug("wrote model {} to byte array of length {}", this.uri, this.modelBytes.length);
      }
    }
  }

  /**
   * Careful, expensive operation: reads model from string.
   * @return
   */
  public Model getModel(){
    assert this.uri != null : "uri must not be null";
    assert this.modelBytes != null : "model must not be null";
    if (this.cachedModel != null) return cachedModel;
    synchronized (this) {
      if (this.cachedModel != null) return cachedModel;
      Model model = ModelFactory.createDefaultModel();
      InputStream is = new ByteArrayInputStream(this.modelBytes);
      try {
        RDFDataMgr.read(model, is,  this.uri.toString(), Lang.TTL);
      } catch (Exception e) {
        logger.warn("could not read model {} from byte array. Byte array is null: {}, has length {}",
          new Object[]{this.uri,
            this.modelBytes == null,
            this.modelBytes == null ? -1 : this.modelBytes.length}
        );
        logger.warn("caught exception while reading model", e);
      }
      this.cachedModel = model;
      return model;
    }
  }
}
