/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.server.ws;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author Fabian Salcher
 * @version 2012/10/29
 */

@WebServiceClient(name = "NeedProtocolWSClientService", targetNamespace = "http://webofneeds.org/needProtocol", wsdlLocation = "/WEB-INF/wsdl/NeedProtocol.wsdl")
public class NeedProtocolWSClientService extends Service
{

  private final static URL NEEDPROTOCOLWEBSERVICE_WSDL_LOCATION;
  private final static Logger logger = Logger.getLogger(NeedProtocolWSClientService.class.getName());

  static {
    URL url = null;
    try {
      URL baseUrl;
      baseUrl = NeedProtocolWSClientService.class.getResource(".");
      url = new URL(baseUrl, "/WEB-INF/wsdl/NeedProtocol.wsdl");
    } catch (MalformedURLException e) {
      logger.warning("Failed to create URL for the wsdl Location: '/WEB-INF/wsdl/NeedProtocol.wsdl', retrying as a local file");
      logger.warning(e.getMessage());
    }
    NEEDPROTOCOLWEBSERVICE_WSDL_LOCATION = url;
  }

  public NeedProtocolWSClientService(URL wsdlLocation, QName serviceName)
  {
    super(wsdlLocation, serviceName);
  }

  public NeedProtocolWSClientService()
  {
    super(NEEDPROTOCOLWEBSERVICE_WSDL_LOCATION, new QName("http://webofneeds.org/needProtocol", "NeedProtocolWSClientService"));
  }

  /**
   * @return returns NeedProtocolWS
   */
  @WebEndpoint(name = "NeedProtocolWSImpl")
  public NeedProtocolWS getNeedProtocolWSImpl()
  {
    return super.getPort(new QName("http://webofneeds.org/needProtocol", "NeedProtocolWSImpl"), NeedProtocolWS.class);
  }
}
