package won.owner.pojo;

import com.hp.hpl.jena.rdf.model.Model;
import won.owner.model.Draft;

import java.net.URI;

/**
 * User: Gabriel
 * Date: 19.12.12
 * Time: 11:44
 */
public class DraftPojo extends NeedPojo
{

  private int currentStep;
  private String userName;

  public DraftPojo(){

  }
  public DraftPojo(URI draftURI, Model content, Draft draftState ){
    super(draftURI, content);
  }
  public int getCurrentStep() {
    return currentStep;
  }

  public void setCurrentStep(final int currentStep) {
    this.currentStep = currentStep;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(final String userName) {
    this.userName = userName;
  }
}