package won.protocol.util;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 20.05.13
 * Time: 20:19
 * To change this template use File | Settings | File Templates.
 */
public class RdfUtils {

    public String toString(Model model) {
        String ret = "";

        if(model != null) {
            StringWriter sw = new StringWriter();
            model.write(sw, "TTL");
            ret = sw.toString();
        }

        return ret;
    }

    public Model toModel(String content) {
      Model m = ModelFactory.createDefaultModel();

      if(content != null) {
        StringReader sr = new StringReader(content);
        m.read(sr, null, "TTL");
      }

      return m;
    }
}
