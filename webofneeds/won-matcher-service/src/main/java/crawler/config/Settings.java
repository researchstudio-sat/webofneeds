package crawler.config;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.ExtensionIdProvider;

/**
 * User: hfriedrich
 * Date: 24.04.2015
 */
public class Settings extends AbstractExtensionId<SettingsImpl>
  implements ExtensionIdProvider
{
  public final static Settings SettingsProvider = new Settings();

  private Settings() {}

  public Settings lookup() {
    return Settings.SettingsProvider;
  }

  public SettingsImpl createExtension(ExtendedActorSystem system) {
    return new SettingsImpl(system.settings().config());
  }
}