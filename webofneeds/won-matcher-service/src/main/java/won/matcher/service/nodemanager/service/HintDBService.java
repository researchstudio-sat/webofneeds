package won.matcher.service.nodemanager.service;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import won.matcher.service.common.event.HintEvent;

/**
 * Created by hfriedrich on 06.07.2016.
 *
 * Currently the hint "database" is just implemented as an in memory bloom filter so that (best guess) duplicate
 * checks are possible to avoid sending duplicate hints. In a future version this might be changed to a persistent
 * hint database.
 */
@Component
@Scope("singleton")
public class HintDBService
{
  private BloomFilter savedHints;

  public HintDBService() {
    savedHints = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), 100000000, 0.001);
  }

  public void saveHint(HintEvent hint) {
    savedHints.put(getHintIdentificationString(hint));
  }

  public boolean mightHintSaved(HintEvent hint) {
    return savedHints.mightContain(getHintIdentificationString(hint));
  }

  private String getHintIdentificationString(HintEvent hint) {
    return hint.getFromNeedUri() + hint.getFromWonNodeUri() + hint.getToNeedUri() + hint.getToWonNodeUri();
  }
}
