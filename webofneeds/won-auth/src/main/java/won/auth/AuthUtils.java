package won.auth;

import won.auth.model.TokenSpecification;

import java.util.Optional;

public class AuthUtils {
    public static Optional<Long> getExpiresAfterSecondsLong(TokenSpecification spec) {
        if (spec.getExpiresAfterInteger() != null) {
            return Optional.of(spec.getExpiresAfterInteger().longValue());
        } else if (spec.getExpiresAfterLong() != null) {
            return Optional.of(spec.getExpiresAfterLong().longValue());
        } else if (spec.getExpiresAfterBigInteger() != null) {
            return Optional.of(spec.getExpiresAfterBigInteger().longValueExact());
        }
        return Optional.empty();
    }

    public static Optional<Integer> getExpiresAfterSecondsInteger(TokenSpecification spec) {
        if (spec.getExpiresAfterInteger() != null) {
            return Optional.of(spec.getExpiresAfterInteger().intValue());
        } else if (spec.getExpiresAfterLong() != null) {
            return Optional.of(spec.getExpiresAfterLong().intValue());
        } else if (spec.getExpiresAfterBigInteger() != null) {
            return Optional.of(spec.getExpiresAfterBigInteger().intValueExact());
        }
        return Optional.empty();
    }
}
