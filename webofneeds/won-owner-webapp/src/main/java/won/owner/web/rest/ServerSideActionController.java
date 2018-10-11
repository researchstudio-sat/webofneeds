package won.owner.web.rest;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import won.owner.model.User;
import won.owner.model.UserNeed;
import won.owner.pojo.ConnectAction;
import won.owner.pojo.FacetToConnect;
import won.owner.repository.UserNeedRepository;
import won.owner.service.impl.UserService;

@Controller
@RequestMapping("/rest/action")
public class ServerSideActionController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserNeedRepository userNeedRepository;

    @Autowired
    private UserService userService;

    
    //rsponses: 204 (no content) or 409 (conflict)
    @RequestMapping(
            value = "/connect",
            method = RequestMethod.POST
    )
    public ResponseEntity connectFacets(@RequestBody(required = true) ConnectAction connectAction) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // cannot use user object from context since hw doesn't know about created in this session need,
        // therefore, we have to retrieve the user object from the user repository
        User user = userService.getByUsername(username);
        if (user == null) {
            return new ResponseEntity("Cannot process connect action: not logged in.", HttpStatus.FORBIDDEN);
        }
        List<FacetToConnect> facets = connectAction.getFacets();
        if (facets == null || facets.isEmpty()) {
            return new ResponseEntity("Cannot process connect action: no facets specified to be connected.", HttpStatus.CONFLICT);
        }
        if (facets.size() > 2) {
            return new ResponseEntity("Cannot process connect action: too many facets specified to be connected.", HttpStatus.CONFLICT);
        }
        List<UserNeed> needs = user.getUserNeeds();
        //keep facets we can't process:
        
        Optional<FacetToConnect> problematicFacet = facets.stream().filter( facet -> {
            //return false (not problematic) if the facet is pending (i.e., the need it belongs to is expected to be created shortly)
            if (facet.isPending()) {
                return false;
            }
            //return true (=problematic) if we don't find a need the facet belongs to
            return !needs.stream().anyMatch( need -> facet.getFacet().startsWith(need.getUri().toString()));  
        }).findFirst();
        if (problematicFacet.isPresent()) {
            return new ResponseEntity("Cannot process connect action: facet " + problematicFacet.get().getFacet() + " does not belong to any of the user's needs.", HttpStatus.CONFLICT);
        }
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
}

