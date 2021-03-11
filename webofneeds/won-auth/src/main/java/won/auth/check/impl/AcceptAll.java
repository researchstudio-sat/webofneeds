package won.auth.check.impl;

import won.auth.check.ConnectionTargetCheck;
import won.auth.check.TargetAtomCheckEvaluator;
import won.auth.check.TokenValidator;

public class AcceptAll implements TargetAtomCheckEvaluator, TokenValidator {
    @Override
    public boolean isRequestorAllowedTarget(ConnectionTargetCheck check) {
        return true;
    }

    @Override
    public boolean isTokenValid() {
        return true;
    }
}
