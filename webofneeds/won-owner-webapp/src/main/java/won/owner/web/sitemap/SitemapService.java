package won.owner.web.sitemap;

import com.redfin.sitemapgenerator.WebSitemapGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import won.owner.model.UserNeed;
import won.owner.repository.UserNeedRepository;
import won.owner.service.impl.URIService;

import java.net.MalformedURLException;

@Service public final class SitemapService {
  @Autowired private URIService uriService;

  @Autowired private UserNeedRepository userNeedRepository;

  public void setUriService(URIService uriService) {
    this.uriService = uriService;
  }

  public void setUserNeedRepository(UserNeedRepository userNeedRepository) {
    this.userNeedRepository = userNeedRepository;
  }

  @Transactional(propagation = Propagation.SUPPORTS) public String createSitemap() throws MalformedURLException {
    WebSitemapGenerator sitemap = new WebSitemapGenerator(uriService.getOwnerProtocolOwnerURI().toString());
    for (UserNeed need : userNeedRepository.findAll()) {
      sitemap.addUrl(uriService.getOwnerProtocolOwnerURI() + "/#!post/?postUri=" + need.getUri());
    }
    return String.join("", sitemap.writeAsStrings());
  }
}