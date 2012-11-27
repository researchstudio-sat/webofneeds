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

package won.node.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import won.node.service.LinkedDataService;
import won.node.service.impl.URIService;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 27.11.12
 */
@Controller
@RequestMapping("/")
public class LinkedDataWebController
{
  final Logger logger = LoggerFactory.getLogger(getClass());


  @Autowired
  private LinkedDataService linkedDataService;

  @Autowired
  private URIService uriService;

  //webmvc controller method
  @RequestMapping("/need/{identifier}")
  public String showNeedPage(@PathVariable String identifier, Model model, HttpServletResponse response) {
    try {
      URI needURI = uriService.createNeedURIForId(identifier);
      com.hp.hpl.jena.rdf.model.Model rdfModel = linkedDataService.getNeedModel(needURI);
      model.addAttribute("rdfModel", rdfModel);
      model.addAttribute("resourceURI", needURI.toString());
      model.addAttribute("dataURI", uriService.toDataURIIfPossible(needURI).toString());
      return "rdfModelView";
    } catch (NoSuchNeedException e) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return "notFoundView";
    }
  }

  //webmvc controller method
  @RequestMapping("/connection/{identifier}")
  public String showConnectionPage(@PathVariable String identifier, Model model, HttpServletResponse response) {
    try {
      URI connectionURI = uriService.createConnectionURIForId(identifier);
      com.hp.hpl.jena.rdf.model.Model rdfModel = linkedDataService.getConnectionModel(connectionURI);
      model.addAttribute("rdfModel", rdfModel);
      model.addAttribute("resourceURI", connectionURI.toString());
      model.addAttribute("dataURI", uriService.toDataURIIfPossible(connectionURI).toString());
      return "rdfModelView";
    } catch (NoSuchConnectionException e) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return "notFoundView";
    }
  }

  //webmvc controller method
  @RequestMapping("/need/")
  public String showNeedURIListPage(
      @RequestParam(defaultValue="-1") int page,
      HttpServletRequest request,
      Model model,
      HttpServletResponse response) {
      com.hp.hpl.jena.rdf.model.Model rdfModel = linkedDataService.listNeedURIs(page);
      model.addAttribute("rdfModel", rdfModel);
      model.addAttribute("resourceURI", uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
      model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
      return "rdfModelView";
  }


  //webmvc controller method
  @RequestMapping("/connection/")
  public String showConnectionURIListPage(
      @RequestParam(defaultValue="-1") int page,
      HttpServletRequest request,
      Model model,
      HttpServletResponse response) {
    com.hp.hpl.jena.rdf.model.Model rdfModel = linkedDataService.listConnectionURIs(page);
    model.addAttribute("rdfModel", rdfModel);
    model.addAttribute("resourceURI", uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
    model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
    return "rdfModelView";
  }

  //webmvc controller method
  @RequestMapping("/need/{identifier/connections/")
  public String showConnectionURIListPage(
      @PathVariable String identifier,
      @RequestParam(defaultValue="-1") int page,
      HttpServletRequest request,
      Model model,
      HttpServletResponse response) {
    URI needURI = uriService.createNeedURIForId(identifier);
    try{
      com.hp.hpl.jena.rdf.model.Model rdfModel = linkedDataService.listConnectionURIs(page,needURI);
      model.addAttribute("rdfModel", rdfModel);
      model.addAttribute("resourceURI", uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
      model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
      return "rdfModelView";
    } catch (NoSuchNeedException e) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return "notFoundView";
    }
  }


  public void setLinkedDataService(final LinkedDataService linkedDataService)
  {
    this.linkedDataService = linkedDataService;
  }

  public void setUriService(final URIService uriService)
  {
    this.uriService = uriService;
  }
}
