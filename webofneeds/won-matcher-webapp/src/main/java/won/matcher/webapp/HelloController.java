package won.matcher.webapp;


import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Controller
@RequestMapping("/hello")
public class HelloController {
	/*@RequestMapping(method = RequestMethod.GET)
	public String printWelcome(ModelMap model) {
		model.addAttribute("message", "Hello, Mr(s) Worlds!");
		return "hello";

	}*/

    /*
    //@RequestMapping(value = "/echo", method = RequestMethod.GET)
    @RequestMapping(method = RequestMethod.GET)
    //public String echoContent(InputStream is, OutputStream os, ModelMap model) {
    public String echoContent(InputStream is, OutputStream os) {
     //   model.addAttribute("message", "Hello echo!");
        int c = 0;
        try {
            c = is.read();
            while(c != -1) {
                os.write(c);
                c = is.read();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return "hello"; //determines the jsp file used?
    }*/
    @RequestMapping(method = RequestMethod.GET)
    public String echoContent(HttpServletRequest requ, ModelMap model,
                              @RequestParam(value = "q", required = true) String query) {
                              //@RequestParam(value = "q", required = false) String query) { // query == null if not passed
        model.addAttribute("results", "The query you send me was: " + query);





        return "matcherresults";
    }

}