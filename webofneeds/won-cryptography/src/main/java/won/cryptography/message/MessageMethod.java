package won.cryptography.message;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 04.08.2014
 */
public class MessageMethod
{


  //private MessageMethodId methodId;
  private String methodUri;
  //private Map<MessageParameterId,String> parameterMap = new HashMap<MessageParameterId,String>();
  private Map<String, String> parameterMap = new HashMap<String, String>();
  private String textInfo;

  public MessageMethod() {
  }

//    public MessageMethod(MessageMethodId methodId) {
//        this.methodId = methodId;
//    }
//
//    public MessageMethodId getMethodId() {
//        return methodId;
//    }
//
//    public void setMethodId(MessageMethodId methodId) {
//        this.methodId = methodId;
//    }

  public MessageMethod(String methodUri) {
    this.methodUri = methodUri;
  }

  public String getMethodUri() {
    return methodUri;
  }

  public void setMethodId(String methodUri) {
    this.methodUri = methodUri;
  }

  //    public Map<MessageParameterId, String> getParameterMap() {
//        return parameterMap;
//    }
  public Map<String, String> getParameterMap() {
    return parameterMap;
  }

  //    public void addParameter(MessageParameterId parameterId, String parameterValue) {
//        this.parameterMap.put(parameterId, parameterValue);
//    }
  public void addParameter(String parameterUri, String parameterValue) {
    this.parameterMap.put(parameterUri, parameterValue);
  }

  public String getTextInfo() {
    return textInfo;
  }

  public void setTextInfo(String textInfo) {
    this.textInfo = textInfo;
  }
}
