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

package won.protocol.rest;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Writer for rdf graph serialization.
 */
@Provider
@Produces("application/trig,application/ld+json,application/n-quads")
@Consumes("application/trig,application/ld+json,application/n-quads")
public class DatasetReaderWriter implements MessageBodyWriter<Dataset>, MessageBodyReader<Dataset>
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public DatasetReaderWriter()
  {
  }


  @Override
  public long getSize(Dataset t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
  {
    return -1;
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
  {
    return Dataset.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(Dataset dataset, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
  {
    logger.debug("writeTo called on GraphWriter, mediaType={}", mediaType);
    try {
      Lang rdfLanguage = mimeTypeToJenaLanguage(mediaType.toString(), Lang.TRIG);
      RDFDataMgr.write(entityStream, dataset, rdfLanguage);
    } finally {
      entityStream.flush();
    }
  }

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
  {
    return true;
  }

  @Override
  public Dataset readFrom(Class<Dataset> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException
  {
    Dataset dataset = DatasetFactory.createMem();
    logger.debug("readFrom called on GraphWriter, mediaType={}", mediaType);
    Lang jenaLanguage = mimeTypeToJenaLanguage(mediaType.toString(), Lang.TRIG);
    logger.debug("converted mediaType {} to jena language {}", mediaType, jenaLanguage);
    RDFDataMgr.read(dataset, entityStream, jenaLanguage);
    return dataset;
  }

  private static Lang mimeTypeToJenaLanguage(String mediaType, Lang defaultLanguage) {
    Lang lang = RDFLanguages.contentTypeToLang(mediaType);
    if (lang == null) return defaultLanguage;
    return lang;
  }

}
