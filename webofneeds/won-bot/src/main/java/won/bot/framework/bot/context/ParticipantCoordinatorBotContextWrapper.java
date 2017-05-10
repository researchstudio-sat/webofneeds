package won.bot.framework.bot.context;

import java.net.URI;
import java.util.List;

/**
 * Created by fsuda on 14.04.2017.
 */
public class ParticipantCoordinatorBotContextWrapper extends BotContextWrapper {
    private String participantListName;
    private String coordinatorListName;

    public ParticipantCoordinatorBotContextWrapper(BotContext botContext, String participantListName, String coordinatorListName) {
        super(botContext, null);
        this.participantListName = participantListName;
        this.coordinatorListName = coordinatorListName;
    }

    public String getParticipantListName() {
        return participantListName;
    }

    public String getCoordinatorListName() {
        return coordinatorListName;
    }

    @Override
    public String getNeedCreateListName() {
        throw new UnsupportedOperationException("This List is not available for this BotContextWrapper");
    }

    public List<URI> getParticipants(){
        return getBotContext().getNamedNeedUriList(participantListName);
    }

    public List<URI> getCoordinators(){
        return getBotContext().getNamedNeedUriList(coordinatorListName);
    }
}
