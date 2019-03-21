package won.matcher.solr.evaluation;

import won.bot.framework.component.needproducer.impl.DirectoryBasedNeedProducer;
import won.bot.framework.component.needproducer.impl.TemplateBasedNeedProducer;

import java.io.File;

/**
 * Created by hfriedrich on 08.08.2016.
 */
public class MailDirNeedProducer extends TemplateBasedNeedProducer {
  public String getCurrentFileName() {
    return ((DirectoryBasedNeedProducer) getWrappedProducer()).getCurrentFileName();
  }

  public File getDirectory() {
    return ((DirectoryBasedNeedProducer) getWrappedProducer()).getDirectory();
  }
}
