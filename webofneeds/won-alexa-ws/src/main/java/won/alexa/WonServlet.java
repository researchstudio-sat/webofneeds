package won.alexa;

import com.amazon.speech.speechlet.servlet.SpeechletServlet;

/**
 * Created by hfriedrich on 05.12.2016.
 */
public class WonServlet extends SpeechletServlet
{
  public WonServlet() {
    setSpeechlet(new WonSpeechlet());
  }
}
