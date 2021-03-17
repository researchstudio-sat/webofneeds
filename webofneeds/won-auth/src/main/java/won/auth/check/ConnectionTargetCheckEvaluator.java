package won.auth.check;

public interface ConnectionTargetCheckEvaluator {
    public boolean isRequestorAllowedTarget(ConnectionTargetCheck check);
}
