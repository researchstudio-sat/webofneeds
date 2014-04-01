package won.matcher.cli;

import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * User: gabriel
 * Date: 14.02.13
 * Time: 15:00
 */
public class MatcherCLI implements CommandLineRunner
{

  private static final Logger logger = LoggerFactory.getLogger(MatcherCLI.class);
    @Autowired
    private MatcherProtocolNeedServiceClient client;


    public static void main(String[] args)
  {
      String need1 = "http://localhost:8080/won/ld/resource/need/1";
      String need2 = "http://localhost:8080/won/ld/resource/need/2";
      String org = "http://localhost:8080/matcher";
      double score = 1;

      for (int i = 0; i < args.length; i++) {
          if (args[i].equals("-h")) {
              System.out.println("USAGE: java MatcherCLI [-n1 need1] [-n2 need2] [-o originator] [-s score] [-h]");
              System.exit(0);
          } else if (args[i].equals("-n1")) {
              need1 = args[++i];
          } else if (args[i].equals("-n2")) {
              need2 = args[++i];
          } else if (args[i].equals("-o")) {
              org = args[++i];
          } else if (args[i].equals("-s")) {
              score = Double.parseDouble(args[++i]);
          }
      }

      MatcherProtocolNeedServiceClient client = new MatcherProtocolNeedServiceClient();
      client.initializeDefault();

      try {
          //TODO: Add rdf content
          client.hint(new URI(need1), new URI(need2), score, new URI(org), null);
      } catch (URISyntaxException e) {
          logger.error("Exception caught:", e);
      } catch (IllegalMessageForNeedStateException e) {
          logger.error("Exception caught:", e);
      } catch (NoSuchNeedException e) {
          logger.error("Exception caught:", e);
      } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
  }

    @Override
    public void run(String... args) throws Exception {
        String need1 = "http://localhost:8080/won/resource/need/1";
        String need2 = "http://localhost:8080/won/resource/need/2";
        String org = "http://localhost:8080/matcher";
        double score = 1;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h")) {
                System.out.println("USAGE: java MatcherCLI [-n1 need1] [-n2 need2] [-o originator] [-s score] [-h]");
                System.exit(0);
            } else if (args[i].equals("-n1")) {
                need1 = args[++i];
            } else if (args[i].equals("-n2")) {
                need2 = args[++i];
            } else if (args[i].equals("-o")) {
                org = args[++i];
            } else if (args[i].equals("-s")) {
                score = Double.parseDouble(args[++i]);
            }
        }


        try {
            //TODO: Add rdf content
            client.hint(new URI(need1), new URI(need2), score, new URI(org), null);
        } catch (URISyntaxException e) {
            logger.error("Exception caught:", e);
        } catch (IllegalMessageForNeedStateException e) {
            logger.error("Exception caught:", e);
        } catch (NoSuchNeedException e) {
            logger.error("Exception caught:", e);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void setClient(MatcherProtocolNeedServiceClient client) {
        this.client = client;
    }
}
