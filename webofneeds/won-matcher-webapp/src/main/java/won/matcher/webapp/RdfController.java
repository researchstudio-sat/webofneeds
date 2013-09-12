package won.matcher.webapp;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Created with IntelliJ IDEA.
 * User: moru
 * Date: 12/09/13
 * Time: 12:50
 * To change this template use File | Settings | File Templates.
 */
@Controller
@RequestMapping("/")
public class RdfController {
    /*@RequestMapping(method = RequestMethod.GET)
    public String testGet(HttpServletRequest requ, ModelMap model) {
        model.addAttribute("results", "here be results");
        return "matcherresults"; //TODO deleteme (we don't need to use a jsp)
    }*/
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public com.hp.hpl.jena.rdf.model.Model getSearchResults() {
        System.out.println("GOT HERE! 1 -------------------------");
        Model model = ModelFactory.createDefaultModel(); //TODO temp changme
        System.out.println("GOT HERE! 2 -------------------------");

        return model;
    }


    public void notifyOfNewNeed() {

    }

    public void postSearchQuery() {

    }

    /*private BookCase bookCase;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public BookCase getBookCase() {
        return this.bookCase;
    }

    @RequestMapping(method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setBookCase(@RequestBody BookCase bookCase) {
        this.bookCase = bookCase;
    }*/
}
