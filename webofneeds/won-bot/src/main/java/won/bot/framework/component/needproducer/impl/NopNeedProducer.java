package won.bot.framework.component.needproducer.impl;

import org.apache.jena.query.Dataset;

import won.bot.framework.component.needproducer.NeedProducer;

/**
 * NeedProducer implementation that does nothing. All methods throw
 * {@link UnsupportedOperationException}s.
 */
public class NopNeedProducer implements NeedProducer {
    public NopNeedProducer() {
    }

    @Override
    public Dataset create() {
        throw new UnsupportedOperationException("This NeedProducer implementation does not expect to be used");
    }

    @Override
    public boolean isExhausted() {
        throw new UnsupportedOperationException("This NeedProducer implementation does not expect to be used");
    }
}
