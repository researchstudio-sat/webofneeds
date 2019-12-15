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

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import won.protocol.util.linkeddata.LDPContainerPage;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

@Controller
@RequestMapping("/rest/atoms")
public class RestAtomController {
    private static final int DEFAULT_MAX_DISTANCE = 5000;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int FETCH_PAGE_SIZE_NO_LIMIT = 1000;
    private static final int MIN_FETCH_PAGE_SIZE_FILLING_UP = 10;
    private static final int MAX_FETCH_PAGE_SIZE_FILLING_UP = 2000;
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
        Set<UserAtom> userAtoms = user.getUserAtoms();
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
     * @return the current user
     */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null)
            throw new AccessDeniedException("client is not authenticated");
        return userRepository.findByUsername(username);
    }

    @ResponseBody
    @RequestMapping(value = "/drafts", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    // TODO: move transactionality annotation into the service layer
    public List<CreateDraftPojo> getAllDrafts() {
        User user = getCurrentUser();
        List<CreateDraftPojo> createDraftPojos = new ArrayList<>();
        Set<URI> draftURIs = user.getDraftURIs();
        for (URI draftURI : draftURIs) {
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
     * @param longitude geo-location
     * @param latitude geo-location
     * @param maxDistance max distance in meters that the location of an atom is
     * away from the given lat/lng coordinates
     * @param filterBySocketTypeUriString filter the results to only contain Atoms
     * with the given socketTypeUri
     * @param filterByAtomTypeUriString filter the results to only contain Atoms
     * with the given atomTypeUri
     * @param limit limit results to this size (if null, 0, or negative value do not
     * limit at all)
     * @return Map of AtomPojos {@literal ->} atoms with certain metadata @see
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
                    @RequestParam(value = "filterBySocketTypeUri", required = false) String filterBySocketTypeUriString,
                    @RequestParam(value = "filterByAtomTypeUri", required = false) String filterByAtomTypeUriString,
                    @RequestParam(value = "limit", required = false) Integer limit) {
        ZonedDateTime modifiedAfter = StringUtils.isNotBlank(modifiedAfterIsoString)
                        ? ZonedDateTime.parse(modifiedAfterIsoString, DateTimeFormatter.ISO_DATE_TIME)
                        : null;
        ZonedDateTime createdAfter = StringUtils.isNotBlank(createdAfterIsoString)
                        ? ZonedDateTime.parse(createdAfterIsoString, DateTimeFormatter.ISO_DATE_TIME)
                        : null;
        Coordinate nearLocation = (latitude != null && longitude != null) ? new Coordinate(latitude, longitude) : null;
        URI nodeURI = wonNodeInformationService.getDefaultWonNodeURI();
        URI filterBySocketTypeUri = null;
        if (filterBySocketTypeUriString != null) {
            filterBySocketTypeUri = URI.create(filterBySocketTypeUriString);
        }
        URI filterByAtomTypeUri = null;
        if (filterByAtomTypeUriString != null) {
            filterByAtomTypeUri = URI.create(filterByAtomTypeUriString);
        }
        Map<URI, AtomPojo> atomMap = new HashMap<>();
        int lastBatchSize = -1;
        int lastPageSize = -1;
        int pageSize = -1;
        Optional<URI> nextLink = Optional.empty();
        // Limit to a large number of paged requests, just so it's
        // never an infinite loop
        OUTER: for (int batch = 0; batch < 10000; batch++) {
            lastPageSize = pageSize;
            LDPContainerPage<List<URI>> fetchResult = null;
            if (batch == 0 || limit == null) {
                // two options here:
                // a) first batch. Just load the amount we want, hope we can use them all
                // b) no limit. just keep fetching large pages
                int page = batch + 1; // pages are 1-based
                pageSize = limit != null ? limit : FETCH_PAGE_SIZE_NO_LIMIT;
                fetchResult = WonLinkedDataUtils.getNodeAtomUrisPage(nodeURI, modifiedAfter, createdAfter, state,
                                filterBySocketTypeUri, filterByAtomTypeUri,
                                page, pageSize, linkedDataSource);
            } else {
                // * at least second batch
                // * we have a limit we want to reach
                // now we'll determine the page size and then figure out the page number we have
                // to use in order
                // to get approximately that many new atoms without missing one
                int page = batch + 1; // pages are 1-based
                pageSize = determinePageSize(atomMap.size(), lastBatchSize, lastPageSize, page, limit);
                fetchResult = WonLinkedDataUtils.getNodeAtomUrisPageAfter(nodeURI, modifiedAfter, createdAfter, state,
                                filterBySocketTypeUri, filterByAtomTypeUri,
                                nextLink.orElse(null), pageSize, linkedDataSource);
            }
            if (fetchResult.getContent().isEmpty()) {
                break;
            }
            nextLink = fetchResult.getNextPageLink();
            int beforeBatchSize = atomMap.size();
            List<URI> atomUris = fetchResult.getContent();
            for (URI atomUri : atomUris) {
                try {
                    Dataset atomDataset = WonLinkedDataUtils.getDataForResource(atomUri, linkedDataSource);
                    AtomPojo atom = new AtomPojo(atomDataset);
                    if (atom.getState() != AtomState.ACTIVE) {
                        continue;
                    }
                    if (nearLocation == null
                                    || isNearLocation(nearLocation, atom.getLocation(), maxDistance)
                                    || isNearLocation(nearLocation, atom.getJobLocation(), maxDistance)) {
                        atomMap.put(atom.getUri(), atom);
                        if (limit != null && limit > 0 && atomMap.size() >= limit)
                            break OUTER; // break fetching if the limit has been reached
                    }
                } catch (LinkedDataFetchingException e) {
                    logger.debug("Could not retrieve atom<" + atomUri + "> cause: " + e.getMessage());
                    continue;
                }
            }
            if (atomUris.size() < pageSize) {
                // We retrieved less than pageSize results.
                // This means that we are done, the next request would yield nothing.
                // Save a request and finish now.
                break;
            }
            lastBatchSize = atomMap.size() - beforeBatchSize;
        }
        return atomMap;
    }

    private int determinePageSize(int loaded, int lastBatchSize, int lastPageSize, int page,
                    int limit) {
        if (limit <= 0) {
            return FETCH_PAGE_SIZE_NO_LIMIT;
        }
        if (loaded == 0) {
            // we still haven't loaded anything.
            // seems like there are a lot of unusable atoms at the beginning of the list
            // still, we don't want to get more than the limit each time because we
            // care especially about fetching the freshest usable ones. If we fetch more
            // than the limit now, we cannot guarantee including those.
            return limit;
        }
        // we've loaded some, so the freshest ones are there. We can be more lenient
        // now.
        int desired = limit - loaded;
        if (desired <= 0) {
            // just in case
            return 0;
        }
        // determine how many more than desired to load
        int pageSize = desired * 2;
        if (lastBatchSize == 0) {
            // special case: did not use any in the last batch. Seems like a
            // portion of unusable ones, fetch more this time
            // (but only if this is more than we would otherwise)
            pageSize = Math.max(lastPageSize * 2, pageSize);
        }
        return Math.min(Math.max(pageSize, MIN_FETCH_PAGE_SIZE_FILLING_UP), MAX_FETCH_PAGE_SIZE_FILLING_UP);
    }

    /**
     * Calculated the distance between two coordinates in meters, and returns true
     * if the distance is below or equal to the maxDistance parameter, if the
     * parameter is null we use 5000meters as the maxDistance
     * 
     * @param nearLocation lat/lng Coordinates of any given location
     * @param atomLocation lat/lng Coordinates of an atom
     * @param maxDistance distance in meters
     * @return true if atomLocation is within the radius of nearLocation and the
     * maxDistance
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
    public CreateDraftPojo createDraft(@RequestBody CreateDraftPojo createDraftObject) {
        User user = getCurrentUser();
        URI draftURI = URI.create(createDraftObject.getDraftURI());
        user.getDraftURIs().add(draftURI);
        wonUserDetailService.save(user);
        Draft draft = draftRepository.findOneByDraftURI(draftURI);
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
        CreateDraftPojo draftPojo = null;
        try {
            URI draftURI = new URI(uri);
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
        // CreateDraftPojo draftPojo = null;
        User user = getCurrentUser();
        try {
            URI draftURI = new URI(uri);
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
