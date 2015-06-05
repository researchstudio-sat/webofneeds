package commons.config;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.ExtensionIdProvider;

/**
 * Settings configuration class of the crawler
 *
 * User: hfriedrich
 * Date: 24.04.2015
 */
public class CommonSettings extends AbstractExtensionId<CommonSettingsImpl>
  implements ExtensionIdProvider
{
  public final static CommonSettings SettingsProvider = new CommonSettings();

  private CommonSettings() {
  }

  public CommonSettings lookup() {
    return CommonSettings.SettingsProvider;
  }

  public CommonSettingsImpl createExtension(ExtendedActorSystem system) {
    return new CommonSettingsImpl(system.settings().config());
  }
}