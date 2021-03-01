package won.owner.web.rest;

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
import won.owner.pojo.SocketToConnect;
import won.owner.repository.UserAtomRepository;
import won.owner.service.impl.UserService;
import won.owner.web.service.ServerSideActionService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    public ResponseEntity connectSockets(@RequestBody(required = true) SocketToConnect[] connectAction) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // cannot use user object from context since hw doesn't know about created in
        // this session atom,
        // therefore, we have to retrieve the user object from the user repository
        User user = userService.getByUsername(username);
        if (user == null) {
            return new ResponseEntity("Cannot process connect action: not logged in.", HttpStatus.FORBIDDEN);
        }
        List<SocketToConnect> sockets = Arrays.asList(connectAction);
        if (sockets == null || sockets.isEmpty()) {
            return new ResponseEntity("Cannot process connect action: no sockets specified to be connected.",
                            HttpStatus.CONFLICT);
        }
        if (sockets.size() > 2) {
            return new ResponseEntity("Cannot process connect action: too many sockets specified to be connected.",
                            HttpStatus.CONFLICT);
        }
        Set<UserAtom> atoms = user.getUserAtoms();
        List<SocketToConnect> socketsWithOwnership = sockets.stream().map(socket -> {
            // return false (not problematic) if the socket is pending (i.e., the atom it
            // belongs to is expected to be created shortly)
            if (socket.isPending()) {
                socket.setNonOwned(false);
            } else if (!atoms.stream().anyMatch(atom -> socket.getSocket().startsWith(atom.getUri().toString()))) {
                socket.setNonOwned(true);
            }
            return socket;
        }).collect(Collectors.toList());
        serverSideActionService.connect(socketsWithOwnership, SecurityContextHolder.getContext().getAuthentication());
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
