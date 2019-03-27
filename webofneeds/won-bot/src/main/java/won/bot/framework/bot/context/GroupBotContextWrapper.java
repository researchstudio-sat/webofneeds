package won.bot.framework.bot.context;

import java.net.URI;
import java.util.List;

/**
 * Created by fsuda on 14.04.2017.
 */
public class GroupBotContextWrapper extends BotContextWrapper {
    private String groupListName = getBotName() + ":groupList";
    private String groupMembersListName = getBotName() + ":groupMembers";

    public GroupBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
    }

    @Override
    public String getNeedCreateListName() {
        throw new UnsupportedOperationException("This List is not available for this BotContextWrapper");
    }

    public String getGroupListName() {
        return groupListName;
    }

    public String getGroupMembersListName() {
        return groupMembersListName;
    }

    public List<URI> getGroupNeedUris() {
        return getBotContext().getNamedNeedUriList(getGroupListName());
    }

    public List<URI> getGroupMemberNeedUris() {
        return getBotContext().getNamedNeedUriList(getGroupMembersListName());
    }
}
