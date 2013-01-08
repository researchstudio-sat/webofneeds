package won.owner.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import won.owner.pojo.NeedPojo;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.IllegalNeedContentException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 17.12.12
 * Time: 13:38
 */

@Controller
public class NeedController {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OwnerProtocolNeedService ownerService;

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ConnectionRepository connectionRepository;


    @RequestMapping(value = "createNeed", method = RequestMethod.GET)
    public String createNeedGet(Model model) {
        model.addAttribute("command", new NeedPojo());
        model.addAttribute("message", "Hello World!");
        return "createNeed";
    }

    @RequestMapping(value = "createNeed", method = RequestMethod.POST)
    public String createNeedPost(@ModelAttribute("SpringWeb") NeedPojo needPojo, Model model) {
        try {
            //TODO: DB insert & Owner URI
            ownerService.createNeed(new URI(""), null, needPojo.isActive());
            Need need = ownerService.readNeed(new URI(""));
            return viewNeed(need.getId().toString(), model);
        } catch (IllegalNeedContentException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        }

        model.addAttribute("command", new NeedPojo());
        model.addAttribute("message", "Hello World!");
        return "createNeed";
    }

    @RequestMapping(value = "need/{needId}", method = RequestMethod.GET)
    public String viewNeed(@PathVariable String needId, Model model) {

        model.addAttribute("needId", needId);

        List<Need> needs = needRepository.findById(Long.valueOf(needId));
        if(needs.isEmpty())
            return "noNeedFound";

        Need need = needs.get(0);
        model.addAttribute("active", (need.getState() != NeedState.ACTIVE ? "activate":"deactivate"));
        //TODO: Activate & Deactivate
        model.addAttribute("needURI", need.getNeedURI());
        //model.addAttribute("matches", matchRepository.findByFromNeed(need.getNeedURI()));
        try {
            model.addAttribute("matches", ownerService.getMatches(need.getNeedURI()));
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        }

        model.addAttribute("connections", connectionRepository.findByNeedURI(need.getNeedURI()));
        model.addAttribute("command", new NeedPojo());

        return "viewNeed";
    }

    @RequestMapping(value = "need/{needId}/connect", method = RequestMethod.POST)
    public String connect2Need(@PathVariable String needId, @ModelAttribute("SpringWeb") NeedPojo needPojo, Model model) {
        try {
            List<Need> needs = needRepository.findById(Long.valueOf(needId));
            if(needs.isEmpty())
                return "noNeedFound";
            Need need1 = needs.get(0);
            needs = needRepository.findByNeedURI(new URI(needPojo.getNeedURI()));
            if(needs.isEmpty())
                return "noNeedFound";
            Need need2 = needs.get(0);
            ownerService.connectTo(need1.getNeedURI(), need2.getNeedURI(), "");
            return viewNeed(need1.getId().toString(), model);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ConnectionAlreadyExistsException e) {
            e.printStackTrace();
        } catch (IllegalMessageForNeedStateException e) {
            e.printStackTrace();
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        }

        return "noNeedFound";
    }

    @RequestMapping(value = "need/{needId}/toggle", method = RequestMethod.GET)
    public String toggleNeed(@PathVariable String needId, Model model) {
        List<Need> needs = needRepository.findById(Long.valueOf(needId));
        if(needs.isEmpty())
            return "noNeedFound";
        Need need = needs.get(0);
        try {
            if(need.getState() == NeedState.ACTIVE) {
                ownerService.deactivate(need.getNeedURI());
            } else {
                ownerService.activate(need.getNeedURI());
            }
            //TODO: update own DB
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        }
        return viewNeed(need.getId().toString(), model);
    }

    @RequestMapping(value = "match/{matchId}/connect", method = RequestMethod.GET)
    public String connect(@PathVariable String matchId, Model model) {
        String ret = "noNeedFound";

        try {
            List<Match> matches = matchRepository.findById(Long.valueOf(matchId));
            if(!matches.isEmpty()) {
               Match match = matches.get(0);
               List<Need> needs = needRepository.findByNeedURI(match.getFromNeed());
               if(!needs.isEmpty())
                  ret = viewNeed(needs.get(0).getId().toString(), model);
               ownerService.connectTo(match.getFromNeed(), match.getToNeed(), "");
               //TODO: Delete Match from Repository?
            }
        } catch (ConnectionAlreadyExistsException e) {
            e.printStackTrace();
        } catch (IllegalMessageForNeedStateException e) {
            e.printStackTrace();
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
