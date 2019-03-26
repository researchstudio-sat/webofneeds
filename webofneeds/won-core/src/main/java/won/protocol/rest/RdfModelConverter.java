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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.riot.WriterDatasetRIOTFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * HttpMessageConverter for using jena models with Spring WebMVC. Supports all
 * formats jena supports, plus JSON-LD
 */
public class RdfModelConverter extends AbstractHttpMessageConverter<Model> {
  private static final Logger logger = LoggerFactory.getLogger(RdfModelConverter.class);

  public RdfModelConverter() {
    this(buildMediaTypeArray());
  }

  public RdfModelConverter(MediaType supportedMediaType) {
    super(supportedMediaType);
  }

  public RdfModelConverter(MediaType... supportedMediaTypes) {
    super(supportedMediaTypes);
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return Model.class.isAssignableFrom(clazz);
  }

  @Override
  protected Model readInternal(Class<? extends Model> aClass, HttpInputMessage httpInputMessage)
      throws IOException, HttpMessageNotReadableException {
    Model model = ModelFactory.createDefaultModel();
    Lang rdfLanguage = mimeTypeToJenaLanguage(httpInputMessage.getHeaders().getContentType(), Lang.TURTLE);
    RDFDataMgr.read(model, httpInputMessage.getBody(), "", rdfLanguage);
    return model;
  }

  @Override
  protected void writeInternal(Model model, HttpOutputMessage httpOutputMessage)
      throws IOException, HttpMessageNotWritableException {
    Lang rdfLanguage = mimeTypeToJenaLanguage(httpOutputMessage.getHeaders().getContentType(), Lang.N3);
    RDFDataMgr.write(httpOutputMessage.getBody(), model, rdfLanguage);
    httpOutputMessage.getBody().flush();
  }

  private static Lang mimeTypeToJenaLanguage(MediaType mediaType, Lang defaultLanguage) {
    Lang lang = RDFLanguages.contentTypeToLang(mediaType.toString());
    if (lang == null)
      return defaultLanguage;
    return lang;
  }

  private static MediaType[] buildMediaTypeArray() {
    // now register the media types this converter can handle
    Collection<Lang> languages = RDFLanguages.getRegisteredLanguages();
    Set<MediaType> mediaTypeSet = new HashSet<MediaType>();
    for (Lang lang : languages) {
      if (datasetWriterExistsForLang(lang)) {
        ContentType ct = lang.getContentType();
        logger.debug("registering converter for rdf content type {}", lang.getContentType());
        MediaType mt = new MediaType(ct.getType(), ct.getSubType());
        mediaTypeSet.add(mt);
      }
    }
    return mediaTypeSet.toArray(new MediaType[mediaTypeSet.size()]);
  }

  private static boolean datasetWriterExistsForLang(Lang lang) {
    RDFFormat serialization = RDFWriterRegistry.defaultSerialization(lang);
    WriterDatasetRIOTFactory wf = RDFWriterRegistry.getWriterDatasetFactory(serialization);
    return wf != null;
  }

}
