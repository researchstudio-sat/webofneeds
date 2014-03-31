package won.node.facet.businessactivity.atomicoutcome;


import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 12.3.14.
 * Time: 15.12
 * To change this template use File | Settings | File Templates.
 */
public enum ATEventType {
    MESSAGE_PREPARED("MessagePrepared", new ArrayList<ATState>(Arrays.asList(ATState.ACTIVE,
            ATState.PREPARING, ATState.PREPARED, ATState.PREPARED_SUCCESS,
            ATState.COMMITTING, ATState.ABORTING)));


    private String name;
    private ArrayList<ATState> permittingPStates;


    ATEventType(String name, ArrayList<ATState> permittingPStates) {
        this.permittingPStates = permittingPStates;
        this.name = name;
    }
}
