package won.bot.framework.bot.context;


public class CommentBotContextWrapper extends BotContextWrapper {
    private String commentListName;

    public CommentBotContextWrapper(BotContext botContext, String needCreateListName, String commentListName) {
        super(botContext, needCreateListName);
        this.commentListName = commentListName;
    }

    public String getCommentListName() {
        return null;
    }
}
