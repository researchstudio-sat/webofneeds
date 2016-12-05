package won.alexa;

import com.amazon.speech.speechlet.*;

/**
 * Created by hfriedrich on 05.12.2016.
 */
public class WonSpeechlet implements Speechlet
{
  @Override
  public void onSessionStarted(final SessionStartedRequest sessionStartedRequest, final Session session)
    throws SpeechletException {

  }

  @Override
  public SpeechletResponse onLaunch(final LaunchRequest launchRequest, final Session session)
    throws SpeechletException {
    return null;
  }

  @Override
  public SpeechletResponse onIntent(final IntentRequest intentRequest, final Session session)
    throws SpeechletException {
    return null;
  }

  @Override
  public void onSessionEnded(final SessionEndedRequest sessionEndedRequest, final Session session)
    throws SpeechletException {

  }
}
