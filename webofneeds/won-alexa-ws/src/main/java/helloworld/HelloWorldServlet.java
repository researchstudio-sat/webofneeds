package helloworld;

import com.amazon.speech.speechlet.servlet.SpeechletServlet;

/**
 * Created by hfriedrich on 05.12.2016.
 */
public class HelloWorldServlet extends SpeechletServlet
{
  public HelloWorldServlet() {
    setSpeechlet(new HelloWorldSpeechlet());
  }
}
