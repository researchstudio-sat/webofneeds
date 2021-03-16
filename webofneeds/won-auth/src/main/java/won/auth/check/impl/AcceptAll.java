package won.auth.check.impl;

import won.auth.check.ConnectionTargetCheck;
import won.auth.check.ConnectionTargetCheckEvaluator;
import won.auth.check.TokenValidator;

public class AcceptAll implements ConnectionTargetCheckEvaluator, TokenValidator {
    @Override
    public boolean isRequestorAllowedTarget(ConnectionTargetCheck check) {
        return true;
    }

    @Override
    public boolean isTokenValid() {
        return true;
    }
}
