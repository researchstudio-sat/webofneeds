package won.bot.framework.bot.context;

public class BotContextWrapper {
    private String needCreateListName;
    private BotContext botContext;

    public BotContextWrapper(BotContext botContext, String needCreateListName) {
        this.botContext = botContext;
        this.needCreateListName = needCreateListName;
    }

    public String getNeedCreateListName() {
        return needCreateListName;
    }

    public BotContext getBotContext() {
        return botContext;
    }
}
