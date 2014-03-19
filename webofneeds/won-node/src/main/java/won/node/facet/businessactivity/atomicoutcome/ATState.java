package won.node.facet.businessactivity.atomicoutcome;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 12.3.14.
 * Time: 15.11
 * To change this template use File | Settings | File Templates.
 */
public enum ATState {
    ACTIVE("Active"),
    PREPARING("Preparing"),
    PREPARED("Prepared"),
    PREPARED_SUCCESS("PreparedSucess"),
    COMMITTING("Commiting"),
    ABORTING("Aborting");



    private static final Logger logger = LoggerFactory.getLogger(ATState.class);

    private String name;
    private static ATState resendEvent = null;

    private ATState(String name)
    {
        this.name = name;
    }

}
