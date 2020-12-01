package won.auth.check;

import java.net.URI;

public interface TargetAtomCheckEvaluator {

    public boolean isAllowedTargetAtom(URI atom, TargetAtomCheck check);
}
