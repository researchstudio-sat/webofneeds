package won.matcher.solr.evaluation;

import java.io.File;

import won.bot.framework.component.needproducer.impl.DirectoryBasedNeedProducer;
import won.bot.framework.component.needproducer.impl.TemplateBasedNeedProducer;

/**
 * Created by hfriedrich on 08.08.2016.
 */
public class MailDirNeedProducer extends TemplateBasedNeedProducer
{
  public String getCurrentFileName() {
    return ((DirectoryBasedNeedProducer) getWrappedProducer()).getCurrentFileName();
  }

  public File getDirectory() {
    return ((DirectoryBasedNeedProducer) getWrappedProducer()).getDirectory();
  }
}
