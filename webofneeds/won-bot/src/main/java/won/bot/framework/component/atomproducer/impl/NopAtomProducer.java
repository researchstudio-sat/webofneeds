package won.bot.framework.component.atomproducer.impl;

import org.apache.jena.query.Dataset;

import won.bot.framework.component.atomproducer.AtomProducer;

/**
 * AtomProducer implementation that does nothing. All methods throw
 * {@link UnsupportedOperationException}s.
 */
public class NopAtomProducer implements AtomProducer {
    public NopAtomProducer() {
    }

    @Override
    public Dataset create() {
        throw new UnsupportedOperationException("This AtomProducer implementation does not expect to be used");
    }

    @Override
    public boolean isExhausted() {
        throw new UnsupportedOperationException("This AtomProducer implementation does not expect to be used");
    }
}
