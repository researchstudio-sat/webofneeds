package won.bot.framework.bot.context;


public class FactoryBotContextWrapper extends BotContextWrapper {
    private String factoryListName;

    public FactoryBotContextWrapper(BotContext botContext, String needCreateListName, String factoryListName) {
        super(botContext, needCreateListName);
        this.factoryListName = factoryListName;
    }

    public String getFactoryListName() {
        return null;
    }
}
