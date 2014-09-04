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

package won.node.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.Header;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.node.service.impl.NeedFacingConnectionCommunicationServiceImpl;
import won.protocol.exception.*;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDecoder;
import won.protocol.need.NeedProtocolNeedService;
import won.protocol.service.NeedFacingNeedCommunicationService;
import won.protocol.util.RdfUtils;

import java.net.URI;

//import com.hp.hpl.jena.util.ModelQueryUtil;
//import com.sun.xml.internal.bind.v2.TODO;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class NeedProtocolNeedServiceImplJMSBased
{
  final Logger logger = LoggerFactory.getLogger(getClass());
  protected NeedFacingNeedCommunicationService needFacingNeedCommunicationService;
  protected NeedFacingConnectionCommunicationServiceImpl connectionCommunicationService;
  protected NeedProtocolNeedService delegate;

  public URI connect(
          @Header("needURI") final String needURI,
          @Header("otherNeedURI") final String otherNeedURI,
          @Header("otherConnectionURI") final String otherConnectionURI,
          @Header("content") final String content,
          @Header("wonMessage") final String wonMessageString)
    throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {

    logger.debug("NODE2: connect received for need {], otherNeed{},connectionURI {}, content {}");
    URI needURIConvert = URI.create(needURI);
    URI otherNeedURIConvert = URI.create(otherNeedURI);
    URI otherConnectionURIConvert = URI.create(otherConnectionURI);
    Model contentConvert = RdfUtils.toModel(content);
    WonMessage wonMessage = WonMessageDecoder.decode(Lang.TRIG, wonMessageString);

    return this.delegate.connect(
            needURIConvert,
            otherNeedURIConvert,
            otherConnectionURIConvert,
            contentConvert,
            wonMessage);
  }

  public void open(
          @Header("connectionURI") final String connectionURI,
          @Header("content") final String content,
          @Header("wonMessage") String wonMessageString)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException, IllegalMessageForNeedStateException {

    logger.debug("NODE2: open received for need {], otherNeed{},connectionURI {}, content {}");
    URI connectionURIConvert = URI.create(connectionURI);
    Model contentConvert = RdfUtils.toModel(content);
    WonMessage wonMessage = WonMessageDecoder.decode(Lang.TRIG, wonMessageString);

    delegate.open(connectionURIConvert, contentConvert, wonMessage);
  }

  public void close(
          @Header("connectionURI") final String connectionURI,
          @Header("content") final String content,
          @Header("wonMessage") String wonMessageString)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {

    logger.debug("NODE2: close received for need {], otherNeed{},connectionURI {}, content {}");
    URI connectionURIConvert = URI.create(connectionURI);
    Model contentConvert = RdfUtils.toModel(content);
    WonMessage wonMessage = WonMessageDecoder.decode(Lang.TRIG, wonMessageString);

    delegate.close(connectionURIConvert, contentConvert, wonMessage);
  }

  public void sendMessage(
          @Header("connectionURI") final String connectionURI,
          @Header("content") final String message,
          @Header("wonMessage") String wonMessageString)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {

    logger.debug("NODE2: text message received for connection {], message {}", connectionURI, message);
    URI connectionURIConvert = URI.create(connectionURI);
    Model messageConvert = RdfUtils.toModel(message);
    WonMessage wonMessage = WonMessageDecoder.decode(Lang.TRIG, wonMessageString);

    delegate.sendMessage(connectionURIConvert, messageConvert, wonMessage);
  }

  public void setNeedFacingNeedCommunicationService(final NeedFacingNeedCommunicationService needFacingNeedCommunicationService) {
    this.needFacingNeedCommunicationService = needFacingNeedCommunicationService;
  }

  public void setConnectionCommunicationService(final NeedFacingConnectionCommunicationServiceImpl connectionCommunicationService) {
    this.connectionCommunicationService = connectionCommunicationService;
  }

  public void setDelegate(NeedProtocolNeedService delegate) {
    this.delegate = delegate;
  }
}
