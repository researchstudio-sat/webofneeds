package node.config;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.ExtensionIdProvider;

/**
 * Settings configuration class of the won node controller
 *
 * User: hfriedrich
 * Date: 24.04.2015
 */
public class WonNodeControllerSettings extends AbstractExtensionId<WonNodeControllerSettingsImpl>
  implements ExtensionIdProvider
{
  public final static WonNodeControllerSettings SettingsProvider = new WonNodeControllerSettings();

  private WonNodeControllerSettings() {
  }

  public WonNodeControllerSettings lookup() {
    return WonNodeControllerSettings.SettingsProvider;
  }

  public WonNodeControllerSettingsImpl createExtension(ExtendedActorSystem system) {
    return new WonNodeControllerSettingsImpl(system.settings().config());
  }
}