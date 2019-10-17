package won.protocol.util;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

public class UriChecker {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: UriChecker [uri]");
            System.err.println(
                            " checks if the specified URI conforms to the URI specification by testing it against java.net.URI");
            System.exit(1);
        }
        String uri = args[0];
        System.out.println("checking URI '" + uri + "'");
        URI asURI = URI.create(uri); // throws an exception if it's not a valid URI.
        System.out.println("valid");
        System.out.println("scheme                : " + asURI.getScheme());
        System.out.println("rawAuthority          : " + asURI.getRawAuthority());
        System.out.println("rawPath               : " + asURI.getRawPath());
        System.out.println("rawQuery              : " + asURI.getRawQuery());
        System.out.println("rawFragment           : " + asURI.getRawFragment());
        System.out.println("rawSchemeSpecificPart : " + asURI.getRawSchemeSpecificPart());
        System.out.println("authority             : " + asURI.getAuthority());
        System.out.println("path         : " + asURI.getPath());
        System.out.println("query        : " + asURI.getQuery());
        System.out.println("fragment     : " + asURI.getFragment());
        System.out.println("making a TTL with it...");
        Model m = ModelFactory.createDefaultModel();
        m.add(m.getResource(uri), RDF.type, m.getResource("http://example.com/Test"));
        RDFDataMgr.write(System.out, m, Lang.TTL);
        System.out.println("done");
    }
}
