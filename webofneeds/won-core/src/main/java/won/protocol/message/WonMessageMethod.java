package won.protocol.message;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 04.08.2014
 */
public class WonMessageMethod
{

  private String methodUri;
  private Map<String, String> parameterMap = new HashMap<String, String>();
  private String textInfo;

  public WonMessageMethod() {
  }

  public WonMessageMethod(String methodUri) {
    this.methodUri = methodUri;
  }

  public String getMethodUri() {
    return methodUri;
  }

  public void setMethodUri(String methodUri) {
    this.methodUri = methodUri;
  }

  public Map<String, String> getParameterMap() {
    return parameterMap;
  }

  public void addParameter(String parameterUri, String parameterValue) {
    this.parameterMap.put(parameterUri, parameterValue);
  }

  public String getTextInfo() {
    return textInfo;
  }

  public void setTextInfo(String textInfo) {
    this.textInfo = textInfo;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof WonMessageMethod)) return false;

    final WonMessageMethod that = (WonMessageMethod) o;

    if (methodUri != null ? !methodUri.equals(that.methodUri) : that.methodUri != null) return false;
    if (!parameterMap.equals(that.parameterMap)) return false;
    if (textInfo != null ? !textInfo.equals(that.textInfo) : that.textInfo != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = methodUri != null ? methodUri.hashCode() : 0;
    result = 31 * result + parameterMap.hashCode();
    result = 31 * result + (textInfo != null ? textInfo.hashCode() : 0);
    return result;
  }
}
