package won.owner.web.sitemap;

import java.net.MalformedURLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.redfin.sitemapgenerator.WebSitemapGenerator;

import won.owner.model.UserAtom;
import won.owner.repository.UserAtomRepository;
import won.owner.service.impl.URIService;

@Service
public final class SitemapService {
    @Autowired
    private URIService uriService;
    @Autowired
    private UserAtomRepository userAtomRepository;

    public void setUriService(URIService uriService) {
        this.uriService = uriService;
    }

    public void setUserAtomRepository(UserAtomRepository userAtomRepository) {
        this.userAtomRepository = userAtomRepository;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public String createSitemap() throws MalformedURLException {
        WebSitemapGenerator sitemap = new WebSitemapGenerator(uriService.getOwnerProtocolOwnerURI().toString());
        for (UserAtom atom : userAtomRepository.findAll()) {
            sitemap.addUrl(uriService.getOwnerProtocolOwnerURI() + "/#!/post?postUri=" + atom.getUri());
        }
        return String.join("", sitemap.writeAsStrings());
    }
}