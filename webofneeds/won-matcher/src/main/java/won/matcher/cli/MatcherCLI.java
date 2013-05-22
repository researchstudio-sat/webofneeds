package won.matcher.cli;

import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.rest.LinkedDataRestClient;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * User: gabriel
 * Date: 14.02.13
 * Time: 15:00
 */
public class MatcherCLI {

    public static void main(String[] args) {
        String need1 = "http://localhost:8080/won/ld/resource/need/1";
        String need2 = "http://localhost:8080/won/ld/resource/need/2";
        String org = "http://localhost:8080/matcher";
        int score = 1;

        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-h")) {
                System.out.println("USAGE: java MatcherCLI [-n1 need1] [-n2 need2] [-o originator] [-s score] [-h]");
                System.exit(0);
            } else if(args[i].equals("-n1")) {
                need1 = args[++i];
            } else if(args[i].equals("-n2")) {
                need2 = args[++i];
            } else if(args[i].equals("-o")) {
                org = args[++i];
            } else if(args[i].equals("-s")) {
                score = Integer.parseInt(args[++i]);
            }
        }

        MatcherProtocolNeedServiceClient client = new MatcherProtocolNeedServiceClient();
        client.setLinkedDataRestClient(new LinkedDataRestClient());

        try {
            //TODO: Add rdf content
            client.hint(new URI(need1), new URI(need2), score, new URI(org), null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IllegalMessageForNeedStateException e) {
            e.printStackTrace();
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        }
    }
}
