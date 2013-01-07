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
import won.owner.pojo.TextMessagePojo;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.model.Connection;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 19.12.12
 * Time: 15:19
 */
@Controller
@RequestMapping("/connection")
public class ConnectionController {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OwnerProtocolNeedService ownerService;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @RequestMapping(value = "/{conId}", method = RequestMethod.GET)
    public String viewConnection(@PathVariable String conId, Model model) {
        List<Connection> cons = connectionRepository.findById(Long.valueOf(conId));
        if(cons.isEmpty())
            return "noNeedFound";
        Connection con = cons.get(0);
        model.addAttribute("connection", con);
        model.addAttribute("messages", chatMessageRepository.findByLocalConnectionURI(con.getConnectionURI()));
        model.addAttribute("command", new TextMessagePojo());

        return "viewConnection";
    }

    @RequestMapping(value = "/{conId}/send", method = RequestMethod.POST)
    public String sendText(@PathVariable String conId, @ModelAttribute("SpringWeb") TextMessagePojo text, Model model) {
        List<Connection> cons = connectionRepository.findById(Long.valueOf(conId));
        if(cons.isEmpty())
            return "noNeedFound";
        Connection con = cons.get(0);

        try {
            ownerService.sendTextMessage(con.getConnectionURI(), text.getText());
            //TODO: update DB
        } catch (NoSuchConnectionException e) {
            e.printStackTrace();
        } catch (IllegalMessageForConnectionStateException e) {
            e.printStackTrace();
        }

        model.addAttribute("connection", con);
        model.addAttribute("messages", chatMessageRepository.findByLocalConnectionURI(con.getConnectionURI()));
        model.addAttribute("command", new TextMessagePojo());

        return "viewConnection";
    }
}
