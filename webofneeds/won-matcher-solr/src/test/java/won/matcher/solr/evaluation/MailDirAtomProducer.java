package won.matcher.solr.evaluation;

import java.io.File;

import won.bot.framework.component.atomproducer.impl.DirectoryBasedAtomProducer;
import won.bot.framework.component.atomproducer.impl.TemplateBasedAtomProducer;

/**
 * Created by hfriedrich on 08.08.2016.
 */
public class MailDirAtomProducer extends TemplateBasedAtomProducer {
    public String getCurrentFileName() {
        return ((DirectoryBasedAtomProducer) getWrappedProducer()).getCurrentFileName();
    }

    public File getDirectory() {
        return ((DirectoryBasedAtomProducer) getWrappedProducer()).getDirectory();
    }
}
