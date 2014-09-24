package won.matcher.solr;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import won.matcher.Matcher;
import won.matcher.processor.HintSender;
import won.matcher.service.ScoreTransformer;
import won.protocol.Config;
import won.protocol.matcher.MatcherProtocolNeedServiceClientSide;
import won.protocol.service.WonNodeInformationService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

/**
 * User: atus
 * Date: 21.02.13
 * Time: 13:53
 */
public class UpdateListener implements SolrEventListener
{
  private Logger logger = LoggerFactory.getLogger(getClass());
  private static final String PROPERTY_FILE_NAME = "matcher.properties";

  private DocumentStorage documentStorage;
  private URI originatorURI;
  private MatcherProtocolNeedServiceClientSide client;
  private WonNodeInformationService wonNodeInformationService;

  @Override
  public void init(NamedList namedList)
  {
    logger.debug("initiating matcher");
    documentStorage = DocumentStorage.getInstance();
    Properties props = new Properties();
    try {
      InputStream is = Config.getInputStreamForConfigFile(Config.PROPERTIES_FILE_MATCHER);
      props.load(is);
      if (is != null)
      try {
        is.close();
      } catch (Exception e) {
        logger.debug("error closing input stream on " + Config.PROPERTIES_FILE_MATCHER);
      }
    } catch (IOException e) {
      logger.error("could not load WON matcher property file {}", PROPERTY_FILE_NAME, e);
    }
    try {
      originatorURI = URI.create(props.getProperty("matcher.uri"));
    } catch (Exception e){
      logger.error("could not generate macher uri from property file {}", PROPERTY_FILE_NAME, e);
    }
    logger.debug("loading spring context for WON matcher protocol client");
    try {
      //we need to replace the classloader spring uses, otherwise it doesn't find our xml config
      final ClassLoader myClassLoader = this.getClass().getClassLoader();
      ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext
        ("classpath:/spring/matcher-jmsonly.xml"){
        protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
          super.initBeanDefinitionReader(reader);
          reader.setBeanClassLoader(myClassLoader);
          setClassLoader(myClassLoader);
        }
      };


      //setup matcher client
      this.client = (MatcherProtocolNeedServiceClientSide) ctx.getBean("matcherProtocolNeedServiceClient");
      this.client.initializeDefault();
      this.wonNodeInformationService = (WonNodeInformationService) ctx.getBean("wonNodeInformationService");
    } catch (Exception e) {
      logger.warn("could not set up matcher client",e);
    }
  }

  public static void main(String... args){
    UpdateListener listener = new UpdateListener();
    listener.init(new NamedList());
    System.out.println("updateListener:" + listener);
  }

  @Override
  public void postCommit()
  {
    logger.debug("postCommit called");
  }

  @Override
  public void newSearcher(SolrIndexSearcher solrIndexSearcher, SolrIndexSearcher solrIndexSearcher2)
  {
    logger.debug("newSearcher called");

    Matcher matcher = new Matcher(solrIndexSearcher, new ScoreTransformer(), this.originatorURI);
    matcher.addMatchProcessor(new HintSender(this.client, this.wonNodeInformationService));

    while (documentStorage.hasNext())
      try {
        matcher.processDocument(documentStorage.pop());
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      }

    matcher.processMatches();
  }

}
