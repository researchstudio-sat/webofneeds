/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.owner.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import won.owner.model.Draft;
import won.owner.model.User;
import won.owner.model.UserNeed;
import won.owner.pojo.CreateDraftPojo;
import won.owner.repository.DraftRepository;
import won.owner.repository.UserRepository;
import won.owner.service.impl.URIService;
import won.owner.service.impl.WONUserDetailService;
import won.protocol.model.NeedState;

@Controller
@RequestMapping("/rest/needs")
public class RestNeedController {
    final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private DraftRepository draftRepository;
    @Autowired
    private URIService uriService;
    @Autowired
    private WONUserDetailService wonUserDetailService;
    @Autowired
    UserRepository userRepository;

    /**
     * returns a List containing needs belonging to the user
     * 
     * @return JSON List of need objects
     */
    @ResponseBody
    @RequestMapping(value = { "/", "" }, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public List<URI> getAllNeedsOfUser(@RequestParam(value = "state", required = false) NeedState state) {
        User user = getCurrentUser();
        List<UserNeed> userNeeds = user.getUserNeeds();
        List<URI> needUris = new ArrayList(userNeeds.size());
        if (state == null) {
            logger.debug("Getting all needuris of user: " + user.getUsername());
            for (UserNeed userNeed : userNeeds) {
                needUris.add(userNeed.getUri());
            }
        } else {
            logger.debug("Getting all needuris of user: " + user.getUsername() + "filtered by state: " + state);
            for (UserNeed userNeed : userNeeds) {
                if (state.equals(userNeed.getState())) {
                    needUris.add(userNeed.getUri());
                }
            }
        }
        return needUris;
    }

    /**
     * Gets the current user. If no user is authenticated, an Exception is thrown
     * 
     * @return
     */
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null)
            throw new AccessDeniedException("client is not authenticated");
        return (User) userRepository.findByUsername(username);
    }

    @ResponseBody
    @RequestMapping(value = "/drafts", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    // TODO: move transactionality annotation into the service layer
    public List<CreateDraftPojo> getAllDrafts() {
        User user = getCurrentUser();
        List<CreateDraftPojo> createDraftPojos = new ArrayList<>();
        Set<URI> draftURIs = user.getDraftURIs();
        Iterator<URI> draftURIIterator = draftURIs.iterator();
        while (draftURIIterator.hasNext()) {
            URI draftURI = draftURIIterator.next();
            Draft draft = draftRepository.findByDraftURI(draftURI).get(0);
            CreateDraftPojo createDraftPojo = new CreateDraftPojo(draftURI.toString(), draft.getContent());
            createDraftPojos.add(createDraftPojo);
        }
        return createDraftPojos;
    }

    /**
     * saves draft of a draft
     * 
     * @param createDraftObject an object containing information of the need draft
     * @return a JSON object of the draft with its temprory id.
     */
    @ResponseBody
    @RequestMapping(value = "/drafts", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    // TODO: move transactionality annotation into the service layer
    @Transactional(propagation = Propagation.SUPPORTS)
    public CreateDraftPojo createDraft(@RequestBody CreateDraftPojo createDraftObject) throws ParseException {
        User user = getCurrentUser();
        URI draftURI = URI.create(createDraftObject.getDraftURI());
        user.getDraftURIs().add(draftURI);
        wonUserDetailService.save(user);
        Draft draft = null;
        draft = draftRepository.findOneByDraftURI(draftURI);
        if (draft == null) {
            draft = new Draft(draftURI, createDraftObject.getDraft());
        }
        draft.setContent(createDraftObject.getDraft());
        draftRepository.save(draft);
        return createDraftObject;
    }

    @ResponseBody
    @RequestMapping(value = "/drafts", method = RequestMethod.DELETE)
    // TODO: move transactionality annotation into the service layer
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity deleteDrafts() {
        try {
            /*
             * User user = getCurrentUser(); List<Draft> draftStates =
             * draftRepository.findByUserName(user.getUsername()); Iterator<Draft>
             * draftIterator = draftStates.iterator(); List<URI> draftURIs = new
             * ArrayList<>(); while(draftIterator.hasNext()){
             * draftURIs.add(draftIterator.next().getDraftURI()); }
             */
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.CONFLICT);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/drafts/draft", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public CreateDraftPojo getDraft(@RequestParam("uri") String uri) {
        logger.debug("getting draft: " + uri);
        URI draftURI = null;
        CreateDraftPojo draftPojo = null;
        try {
            draftURI = new URI(uri);
            Draft draft = draftRepository.findOneByDraftURI(draftURI);
            if (draft == null) {
                logger.warn("draft requested for delete was not found: " + uri);
            } else {
                draftPojo = new CreateDraftPojo(draft.getDraftURI().toString(), draft.getContent());
            }
        } catch (URISyntaxException e) {
            logger.warn("draft uri problem.", e);
        }
        return draftPojo;
    }

    @ResponseBody
    @RequestMapping(value = "/drafts/draft", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteDraft(@RequestParam("uri") String uri) {
        logger.debug("deleting draft: " + uri);
        URI draftURI = null;
        CreateDraftPojo draftPojo = null;
        User user = getCurrentUser();
        try {
            draftURI = new URI(uri);
            user.getDraftURIs().remove(draftURI);
            wonUserDetailService.save(user);
            Draft draft = draftRepository.findOneByDraftURI(draftURI);
            if (draft == null) {
                logger.warn("draft requested for delete was not found: " + uri);
            } else {
                draftRepository.delete(draft);
            }
            return ResponseEntity.ok().body("\"deleted draft: " + uri + "\"");
        } catch (URISyntaxException e) {
            logger.warn("draft uri problem.", e);
            return ResponseEntity.badRequest().body("draft uri problem");
        }
    }
}
