package won.bot.framework.bot.context;

import java.net.URI;
import java.util.List;

public class CommentBotContextWrapper extends BotContextWrapper {
    private String commentListName = getBotName() + ":commentList";

    public CommentBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
    }

    public String getCommentListName() {
        return null;
    }

    public List<URI> getCommentList() {
        return getBotContext().getNamedNeedUriList(commentListName);
    }
}
