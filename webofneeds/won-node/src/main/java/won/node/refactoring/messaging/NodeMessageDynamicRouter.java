package won.node.refactoring.messaging;

import org.apache.camel.Header;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import won.node.refactoring.FacetCamel;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDynamicRouter;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

/**
 * User: syim
 * Date: 05.03.2015
 */
public class NodeMessageDynamicRouter implements WonMessageDynamicRouter,ApplicationContextAware
{

  private ArrayList<FacetCamel> facets= new ArrayList<FacetCamel>();
  private ApplicationContext applicationContext;

  @Override
  public void route(@Header("facetType")URI facetType,@Header("direction") URI direction,@Header("messageType")
  URI messageType,final WonMessage
    wonMessage) {

    Map facetMap = applicationContext.getBeansOfType(FacetCamel.class);
    facets = (ArrayList) facetMap.values();
    for(int i = 0;i<facets.size();i++){
      FacetCamel facet = facets.get(i);
      Annotation[] annotations = facet.getClass().getAnnotations();
    }
  }


  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
