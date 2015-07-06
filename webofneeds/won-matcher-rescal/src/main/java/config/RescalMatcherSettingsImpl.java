package config;

import akka.actor.Extension;
import com.typesafe.config.Config;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Settings configuration class of rescal matcher
 *
 * User: hfriedrich
 * Date: 24.04.2015
 */
public class RescalMatcherSettingsImpl implements Extension
{

  public final String EXECUTION_DIRECTORY;
  public final String NLP_RESOURCE_DIRECTORY;
  public final String PYTHON_SCRIPT_DIRECTORY;
  public final FiniteDuration EXECUTION_DURATION;
  public final double THRESHOLD;
  public final int RANK;

  public RescalMatcherSettingsImpl(Config config) {

    EXECUTION_DIRECTORY = config.getString("matcher.rescal.executionDir");
    NLP_RESOURCE_DIRECTORY = config.getString("matcher.rescal.nlpResourceDir");
    PYTHON_SCRIPT_DIRECTORY = config.getString("matcher.rescal.pythonScriptDir");
    EXECUTION_DURATION = Duration.create(config.getDuration(
      "matcher.rescal.executionDuration", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
    THRESHOLD = config.getDouble("matcher.rescal.threshold");
    RANK = config.getInt("matcher.rescal.rank");
  }
}