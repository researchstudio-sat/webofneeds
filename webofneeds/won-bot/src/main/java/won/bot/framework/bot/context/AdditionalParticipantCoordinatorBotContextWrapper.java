package won.bot.framework.bot.context;

import java.net.URI;
import java.util.List;

/**
 * Created by fsuda on 14.04.2017.
 */
public class AdditionalParticipantCoordinatorBotContextWrapper extends ParticipantCoordinatorBotContextWrapper {
    private String participantDelayedListName = getBotName() + ":participantsDelayed";

    public AdditionalParticipantCoordinatorBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
    }

    public String getParticipantDelayedListName() {
        return participantDelayedListName;
    }

    public List<URI> getParticipantsDelayed() {
        return getBotContext().getNamedAtomUriList(participantDelayedListName);
    }
}
