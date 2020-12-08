package won.shacl2java.validation;

import org.apache.jena.riot.system.ErrorHandler;

import java.util.concurrent.atomic.AtomicBoolean;

public class ResettableErrorHandler implements ErrorHandler {
    private AtomicBoolean warning = new AtomicBoolean(false);
    private AtomicBoolean error = new AtomicBoolean(false);
    private AtomicBoolean fatal = new AtomicBoolean(false);

    @Override
    public void warning(String s, long l, long l1) {
        warning.set(true);
    }

    @Override
    public void error(String s, long l, long l1) {
        error.set(true);
    }

    @Override
    public void fatal(String s, long l, long l1) {
        fatal.set(true);
    }

    public void reset() {
        warning.set(false);
        error.set(false);
        fatal.set(false);
    }

    public boolean isWarning() {
        return warning.get();
    }

    public boolean isError() {
        return error.get();
    }

    public boolean isFatal() {
        return fatal.get();
    }
}
