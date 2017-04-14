package won.bot.framework.bot.context;

/**
 * Created by fsuda on 14.04.2017.
 */
public class GroupBotContextWrapper extends BotContextWrapper {
    private String groupListName;
    private String groupMembersListName;

    public GroupBotContextWrapper(BotContext botContext, String groupMembersListName, String groupListName) {
        super(botContext, null);
        this.groupListName = groupListName;
        this.groupMembersListName = groupMembersListName;
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
}
