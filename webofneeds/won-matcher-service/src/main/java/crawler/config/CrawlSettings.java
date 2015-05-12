package crawler.config;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.ExtensionIdProvider;

/**
 * Settings configuration class of the crawler
 *
 * User: hfriedrich
 * Date: 24.04.2015
 */
public class CrawlSettings extends AbstractExtensionId<CrawlSettingsImpl>
  implements ExtensionIdProvider
{
  public final static CrawlSettings SettingsProvider = new CrawlSettings();

  private CrawlSettings() {
  }

  public CrawlSettings lookup() {
    return CrawlSettings.SettingsProvider;
  }

  public CrawlSettingsImpl createExtension(ExtendedActorSystem system) {
    return new CrawlSettingsImpl(system.settings().config());
  }
}