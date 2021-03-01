package won.owner.web.rest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import won.owner.model.User;
import won.owner.model.UserAtom;
import won.owner.pojo.ServerSideConnectPayload;
import won.owner.repository.UserAtomRepository;
import won.owner.service.impl.UserService;
import won.owner.web.service.ServerSideActionService;

import java.util.Set;

@Controller
@RequestMapping("/rest/action")
public class ServerSideActionController {
    @Autowired
    private UserAtomRepository userAtomRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private ServerSideActionService serverSideActionService;

    // rsponses: 204 (no content) or 409 (conflict)
    @RequestMapping(value = "/connect", method = RequestMethod.POST)
    public ResponseEntity connectSockets(@RequestBody(required = true) ServerSideConnectPayload connectAction) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // cannot use user object from context since hw doesn't know about created in
        // this session atom,
        // therefore, we have to retrieve the user object from the user repository
        User user = userService.getByUsername(username);
        if (user == null) {
            return new ResponseEntity("Cannot process connect action: not logged in.", HttpStatus.FORBIDDEN);
        }
        if (StringUtils.isEmpty(connectAction.getFromSocket())) {
            return new ResponseEntity("Cannot process connect action: from socket not specified.",
                            HttpStatus.CONFLICT);
        }
        if (StringUtils.isEmpty(connectAction.getToSocket())) {
            return new ResponseEntity("Cannot process connect action: to socket not specified.",
                            HttpStatus.CONFLICT);
        }
        Set<UserAtom> atoms = user.getUserAtoms();
        if (!(connectAction.isFromPending() || atoms.stream()
                        .anyMatch(atom -> connectAction.getFromSocket().startsWith(atom.getUri().toString())))) {
            return new ResponseEntity("Cannot process connect action: from atom is not owned nor in pending.",
                            HttpStatus.CONFLICT);
        }
        if (connectAction.isAutoOpen() && !(connectAction.isToPending() || atoms.stream()
                        .anyMatch(atom -> connectAction.getToSocket().startsWith(atom.getUri().toString())))) {
            return new ResponseEntity("Cannot process connect autoOpen action: to atom is not owned nor in pending.",
                            HttpStatus.CONFLICT);
        }
        serverSideActionService.connect(connectAction, SecurityContextHolder.getContext().getAuthentication());
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    public void setServerSideActionService(ServerSideActionService serverSideActionService) {
        this.serverSideActionService = serverSideActionService;
    }

    public void setUserAtomRepository(UserAtomRepository userAtomRepository) {
        this.userAtomRepository = userAtomRepository;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
