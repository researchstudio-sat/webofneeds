package won.owner.pojo;

/**
 * User: Gabriel
 * Date: 19.12.12
 * Time: 11:44
 */
public class DraftPojo extends NeedPojo
{

  private int currentStep;
  private String userName;

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