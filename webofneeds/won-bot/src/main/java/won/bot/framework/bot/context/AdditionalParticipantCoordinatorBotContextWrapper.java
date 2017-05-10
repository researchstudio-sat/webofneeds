package won.bot.framework.bot.context;

import java.net.URI;
import java.util.List;

/**
 * Created by fsuda on 14.04.2017.
 */
public class AdditionalParticipantCoordinatorBotContextWrapper extends ParticipantCoordinatorBotContextWrapper {
    private String participantDelayedListName;

    public AdditionalParticipantCoordinatorBotContextWrapper(BotContext botContext, String participantListName, String participantDelayedListName, String coordinatorListName) {
        super(botContext, participantListName, coordinatorListName);
        this.participantDelayedListName = participantDelayedListName;
    }

    public String getParticipantDelayedListName() {
        return participantDelayedListName;
    }

    public List<URI> getParticipantsDelayed() {
        return getBotContext().getNamedNeedUriList(participantDelayedListName);
    }
}
