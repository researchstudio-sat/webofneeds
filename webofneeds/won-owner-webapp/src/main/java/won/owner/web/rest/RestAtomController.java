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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.jena.query.Dataset;
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
import won.owner.model.UserAtom;
import won.owner.pojo.AtomPojo;
import won.owner.pojo.CreateDraftPojo;
import won.owner.repository.DraftRepository;
import won.owner.repository.UserRepository;
import won.owner.service.impl.WONUserDetailService;
import won.protocol.model.AtomState;
import won.protocol.model.Coordinate;
import won.protocol.rest.LinkedDataFetchingException;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

@Controller
@RequestMapping("/rest/atoms")
public class RestAtomController {
    private static final int DEFAULT_MAX_DISTANCE = 5000;
    final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private DraftRepository draftRepository;
    @Autowired
    private WONUserDetailService wonUserDetailService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    private LinkedDataSource linkedDataSource;
    @Autowired
    private WonNodeInformationService wonNodeInformationService;

    /**
     * returns a List containing atoms belonging to the user
     * 
     * @return JSON List of atom objects
     */
    @ResponseBody
    @RequestMapping(value = { "/", "" }, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public Map<URI, AtomPojo> getAllAtomsOfUser(@RequestParam(value = "state", required = false) AtomState state) {
        User user = getCurrentUser();
        List<UserAtom> userAtoms = user.getUserAtoms();
        Map<URI, AtomPojo> atomMap = new HashMap<>();
        for (UserAtom userAtom : userAtoms) {
            if (state == null || state.equals(userAtom.getState())) {
                try {
                    Dataset atomDataset = WonLinkedDataUtils.getDataForResource(userAtom.getUri(), linkedDataSource); // FIXME:
                                                                                                                      // SOMEHOW
                                                                                                                      // DerivedData
                                                                                                                      // is
                                                                                                                      // not
                                                                                                                      // retrieved
                                                                                                                      // with
                                                                                                                      // getDataForResource...
                    AtomPojo atom = new AtomPojo(atomDataset);
                    atomMap.put(atom.getUri(), atom);
                } catch (LinkedDataFetchingException e) {
                    logger.debug("Could not retrieve atom<" + userAtom.getUri() + "> cause: " + e.getMessage());
                }
            }
        }
        return atomMap;
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
     * Returns a Map of atoms (key is the atomUri), value is the atom including
     * (meta)data
     * 
     * @param state only return atoms with the given AtomState
     * @param modifiedAfterIsoString only return atoms that have been modified after
     * this timestamp (ISO 8601 format (UTC): yyyy-MM-dd'T'HH:mm:ss.SSS'Z')
     * @param createdAfterIsoString only return atoms that have been created after
     * this timestamp (ISO 8601 format (UTC): yyyy-MM-dd'T'HH:mm:ss.SSS'Z')
     * @param limit limit results to this size (if null, 0, or negative value do not
     * limit at all)
     * @return Map of AtomPojos -> atoms with certain metadata @see
     * won.owner.pojo.AtomPojo
     */
    @ResponseBody
    @RequestMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public Map<URI, AtomPojo> getAllAtoms(@RequestParam(value = "state", required = false) AtomState state,
                    @RequestParam(value = "modifiedafter", required = false) String modifiedAfterIsoString,
                    @RequestParam(value = "createdafter", required = false) String createdAfterIsoString,
                    @RequestParam(value = "latitude", required = false) Float latitude,
                    @RequestParam(value = "longitude", required = false) Float longitude,
                    @RequestParam(value = "maxDistance", required = false) Integer maxDistance,
                    @RequestParam(value = "limit", required = false) int limit) {
        // the #atomList and fetch these as well
        // TODO: fetch with modifiedafter parameter and not only the uri
        ZonedDateTime modifiedAfter = StringUtils.isNotBlank(modifiedAfterIsoString)
                        ? ZonedDateTime.parse(modifiedAfterIsoString, DateTimeFormatter.ISO_DATE_TIME)
                        : null;
        ZonedDateTime createdAfter = StringUtils.isNotBlank(createdAfterIsoString)
                        ? ZonedDateTime.parse(createdAfterIsoString, DateTimeFormatter.ISO_DATE_TIME)
                        : null;
        Coordinate nearLocation = (latitude != null && longitude != null) ? new Coordinate(latitude, longitude) : null;
        URI nodeURI = wonNodeInformationService.getDefaultWonNodeURI();
        List<URI> atomUris = WonLinkedDataUtils.getNodeAtomUris(nodeURI, modifiedAfter, createdAfter, state,
                        linkedDataSource);
        Map<URI, AtomPojo> atomMap = new HashMap<>();
        for (URI atomUri : atomUris) {
            try {
                Dataset atomDataset = WonLinkedDataUtils.getDataForResource(atomUri, linkedDataSource);
                AtomPojo atom = new AtomPojo(atomDataset);
                if (state == null || atom.getState().equals(state)
                                && ((modifiedAfter == null) || modifiedAfter.isBefore(atom.getModifiedZonedDateTime()))
                                && ((createdAfter == null) || createdAfter.isBefore(atom.getCreationZonedDateTime()))
                                && ((nearLocation == null)
                                                || isNearLocation(nearLocation, atom.getLocation(), maxDistance)
                                                || isNearLocation(nearLocation, atom.getJobLocation(), maxDistance))) {
                    atomMap.put(atom.getUri(), atom);
                    if (limit > 0 && atomMap.size() >= limit)
                        break; // break fetching if the limit has been reached
                }
            } catch (LinkedDataFetchingException e) {
                logger.debug("Could not retrieve atom<" + atomUri + "> cause: " + e.getMessage());
            }
        }
        return atomMap;
    }

    /**
     * Calculated the distance between two coordinates in meters, and returns true
     * if the distance is below or equal to the maxDistance parameter, if the
     * parameter is null we use 5000meters as the maxDistance
     * 
     * @param nearLocation
     * @param atomLocation
     * @param maxDistance
     * @return
     */
    private boolean isNearLocation(Coordinate nearLocation, Coordinate atomLocation, Integer maxDistance) {
        if (atomLocation != null) {
            if (maxDistance == null) {
                maxDistance = DEFAULT_MAX_DISTANCE;
            }
            double earthRadius = 6371000; // meters
            double dLat = Math.toRadians(nearLocation.getLatitude() - atomLocation.getLatitude());
            double dLng = Math.toRadians(nearLocation.getLongitude() - atomLocation.getLongitude());
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(atomLocation.getLatitude()))
                            * Math.cos(Math.toRadians(nearLocation.getLatitude())) * Math.sin(dLng / 2)
                            * Math.sin(dLng / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = earthRadius * c;
            return distance <= maxDistance;
        }
        return false;
    }

    /**
     * saves draft of a draft
     * 
     * @param createDraftObject an object containing information of the atom draft
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

    public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }
}
