package won.node.test;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WonMessageRoutesTestHelper {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void doInSeparateTransaction(Runnable task) {
        task.run();
    }
}
