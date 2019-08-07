package won.owner.web.sitemap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;

@Controller
@RequestMapping("/")
public class SitemapController {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private SitemapService sitemapService;

    /**
     * Fetches the full sitemap.
     *
     * @return ResponseEntity the xml sitemap.
     */
    @ResponseBody
    @RequestMapping(value = "sitemap.xml", method = RequestMethod.GET)
    public void getFullSitemap(HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
        try (Writer writer = response.getWriter()) {
            writer.append(sitemapService.createSitemap());
        } catch (IOException e) {
            logger.info("could not create sitemap", e);
        }
    }
}
