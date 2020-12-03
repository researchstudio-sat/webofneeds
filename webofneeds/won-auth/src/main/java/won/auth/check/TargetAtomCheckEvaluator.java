package won.auth.check;

public interface TargetAtomCheckEvaluator {
    public boolean isRequestorAllowedTarget(TargetAtomCheck check);
}
