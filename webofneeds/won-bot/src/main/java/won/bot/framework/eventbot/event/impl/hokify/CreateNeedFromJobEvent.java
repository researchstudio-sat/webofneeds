package won.bot.framework.eventbot.event.impl.hokify;

import java.util.ArrayList;

import won.bot.framework.eventbot.action.impl.hokify.HokifyJob;
import won.bot.framework.eventbot.action.impl.hokify.util.HokifyBotsApi;
import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Created by MS on 18.09.2018.
 */
public class CreateNeedFromJobEvent extends BaseEvent {
    private final HokifyBotsApi hokifyBotsApi;

    public CreateNeedFromJobEvent(HokifyBotsApi HokifyBotsApi) {
        this.hokifyBotsApi = HokifyBotsApi;
    }

    public HokifyBotsApi getHokifyBotsApi() {
        return hokifyBotsApi;
    }
}
