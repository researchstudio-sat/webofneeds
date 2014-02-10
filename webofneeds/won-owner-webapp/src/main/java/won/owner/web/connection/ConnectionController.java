package won.owner.web.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import won.owner.pojo.TextMessagePojo;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.model.Connection;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.WonRdfUtils;

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
    @Qualifier("default")
    private OwnerProtocolNeedServiceClientSide ownerService;

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
        model.addAttribute("command", new TextMessagePojo());
        return "viewConnection";
    }


    @RequestMapping(value = "/{conId}/body", method = RequestMethod.GET)
    public String listMessages(@PathVariable String conId, Model model) throws NoSuchConnectionException {
        Connection con = DataAccessUtils.loadConnection(connectionRepository,Long.valueOf(conId));
     //   List<Connection> cons = connectionRepository.findById(Long.valueOf(conId));

       if(con==null)
            return "noNeedFound";
        //Connection con = cons.get(0);
        try {
            switch (con.getState()) {
                case REQUEST_RECEIVED:
                    model.addAttribute("connection", con);
                    return "manageConnection";
                case REQUEST_SENT:
                    model.addAttribute("message", "Pending....");
                    return "showMessage";
                case CLOSED:
                    model.addAttribute("message", "Connection Closed!");
                    return "showMessage";
                case CONNECTED:
                    model.addAttribute("messages", chatMessageRepository.findByLocalConnectionURI(con.getConnectionURI()));
                    return "listMessages";
            }
        } catch (Exception e) {
            logger.warn("error reading connection from won node");
            return "error reading connection from won node: " + e.getMessage();
        }
      return "noNeedFound";
    }

    @RequestMapping(value = "/{conId}/send", method = RequestMethod.POST)
    public String sendText(@PathVariable String conId, @ModelAttribute("SpringWeb") TextMessagePojo text, Model model) {
        List<Connection> cons = connectionRepository.findById(Long.valueOf(conId));
        if(cons.isEmpty())
            return "noNeedFound";
        Connection con = cons.get(0);

        try {

            ownerService.textMessage(con.getConnectionURI(), WonRdfUtils.MessageUtils.textMessage(text.getText()));
        } catch (Exception e) {
            logger.warn("error sending text message");
            return "error sending text message: " + e.getMessage();
        }

        return  "redirect:/connection/" + con.getId().toString();//"viewConnection";
    }


    @RequestMapping(value = "/{conId}/accept", method = RequestMethod.POST)
    public String accept(@PathVariable String conId, Model model) {
        List<Connection> cons = connectionRepository.findById(Long.valueOf(conId));
        if(cons.isEmpty())
            return "noNeedFound";
        Connection con = cons.get(0);
        try {
            //TODO: add rdf content here as soon as we support its creation in the owner app
            ownerService.open(con.getConnectionURI(), null);
        } catch (Exception e) {
          logger.warn("error during accept", e);
          return "error during accept: " + e.getMessage();
        }

        return  "redirect:/connection/" + con.getId().toString() + "/body";
    }

    @RequestMapping(value = "/{conId}/deny", method = RequestMethod.POST)
    public String deny(@PathVariable String conId, Model model) {
        List<Connection> cons = connectionRepository.findById(Long.valueOf(conId));
        if(cons.isEmpty())
            return "noNeedFound";
        Connection con = cons.get(0);
        try {
          //TODO: add rdf content here as soon as we support its creation in the owner app
            ownerService.close(con.getConnectionURI(), null);
        } catch (Exception e) {
          logger.warn("error during deny", e);
          return "error during deny: " + e.getMessage();
        }

        return  "redirect:/connection/" + con.getId().toString() + "/body";
    }

    @RequestMapping(value = "/{conId}/close", method = RequestMethod.POST)
    public String close(@PathVariable String conId, Model model) {
        List<Connection> cons = connectionRepository.findById(Long.valueOf(conId));
        if(cons.isEmpty())
            return "noNeedFound";
        Connection con = cons.get(0);
        try {
          //TODO: add rdf content here as soon as we support its creation in the owner app
            ownerService.close(con.getConnectionURI(), null);
        } catch (Exception e) {
          logger.warn("error during close", e);
          return "error during close: " + e.getMessage();
        }

        return  "redirect:/connection/" + con.getId().toString();
    }
}
