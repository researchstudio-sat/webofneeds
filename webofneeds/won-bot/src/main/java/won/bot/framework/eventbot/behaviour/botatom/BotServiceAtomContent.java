package won.bot.framework.eventbot.behaviour.botatom;

public class BotServiceAtomContent {
    // TODO: ADD MORE SERVICE BOT ATOM CONTENT
    private String name;
    private String description;

    public BotServiceAtomContent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
