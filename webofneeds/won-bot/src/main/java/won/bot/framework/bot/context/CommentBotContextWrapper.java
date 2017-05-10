package won.bot.framework.bot.context;


import java.net.URI;
import java.util.List;

public class CommentBotContextWrapper extends BotContextWrapper {
    private String commentListName;

    public CommentBotContextWrapper(BotContext botContext, String needCreateListName, String commentListName) {
        super(botContext, needCreateListName);
        this.commentListName = commentListName;
    }

    public String getCommentListName() {
        return null;
    }

    public List<URI> getCommentList(){
        return getBotContext().getNamedNeedUriList(commentListName);
    }
}
