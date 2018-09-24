package won.bot.framework.eventbot.event.impl.hokify;

import java.util.ArrayList;

import won.bot.framework.eventbot.action.impl.hokify.HokifyJob;
import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Created by MS on 18.09.2018.
 */
public class CreateNeedFromJobEvent extends BaseEvent {
    private final ArrayList<HokifyJob> hokifyJobs;

    public CreateNeedFromJobEvent(ArrayList<HokifyJob> hokifyJobs) {
        this.hokifyJobs = hokifyJobs;
    }

    public ArrayList<HokifyJob> getHokifyJobs() {
        return hokifyJobs;
    }
}
