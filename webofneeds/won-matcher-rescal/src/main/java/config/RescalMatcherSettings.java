package config;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.ExtensionIdProvider;

/**
 * Settings configuration class of rescal matcher
 *
 * User: hfriedrich
 * Date: 24.04.2015
 */
public class RescalMatcherSettings extends AbstractExtensionId<RescalMatcherSettingsImpl>
  implements ExtensionIdProvider
{
  public final static RescalMatcherSettings SettingsProvider = new RescalMatcherSettings();

  private RescalMatcherSettings() {
  }

  public RescalMatcherSettings lookup() {
    return RescalMatcherSettings.SettingsProvider;
  }

  public RescalMatcherSettingsImpl createExtension(ExtendedActorSystem system) {
    return new RescalMatcherSettingsImpl(system.settings().config());
  }
}