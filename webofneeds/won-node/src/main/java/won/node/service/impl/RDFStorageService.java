package won.node.service.impl;

import com.hp.hpl.jena.rdf.model.Model;
import won.protocol.model.Need;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 15.02.13
 * Time: 11:22
 * To change this template use File | Settings | File Templates.
 */
public interface RDFStorageService {
    public void storeContent(Need need, Model graph);
    public Model loadContent(Need need);
}
