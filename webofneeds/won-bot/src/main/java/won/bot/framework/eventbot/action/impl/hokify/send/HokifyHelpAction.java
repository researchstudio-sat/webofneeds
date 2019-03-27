package won.bot.framework.eventbot.action.impl.hokify.send;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.hokify.WonHokifyJobBotHandler;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.telegram.SendHelpEvent;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Created by MS on 24.09.2018.
 */
public class HokifyHelpAction extends BaseEventBotAction {
    private WonHokifyJobBotHandler wonHokifyJobBotHandler;

    public HokifyHelpAction(EventListenerContext eventListenerContext, WonHokifyJobBotHandler wonHokifyJobBotHandler) {
        super(eventListenerContext);
        this.wonHokifyJobBotHandler = wonHokifyJobBotHandler;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (event instanceof SendHelpEvent) {
            logger.info("HelpEvent received");
            /*
             * Update update = ((SendHelpEvent) event).getUpdate(); Message message =
             * update.getMessage(); Long chatId = message.getChatId(); ReplyKeyboardMarkup
             * replyKeyboardMarkup = new ReplyKeyboardMarkup(); List<KeyboardRow> commands =
             * new ArrayList<>(); KeyboardRow kb = new KeyboardRow(); kb.add("create need");
             * KeyboardRow kb1 = new KeyboardRow(); kb1.add("list your needs");
             * commands.add(kb); commands.add(kb1);
             * replyKeyboardMarkup.setResizeKeyboard(true);
             * replyKeyboardMarkup.setKeyboard(commands); SendMessage sendMessage = new
             * SendMessage(); sendMessage.setChatId(chatId);
             * sendMessage.setText("how can i help you?"+message.getText());
             * sendMessage.setReplyMarkup(replyKeyboardMarkup); try{
             * wonTelegramBotHandler.sendMessage(sendMessage); }catch (TelegramApiException
             * e){ logger.error(e.getMessage()); }
             */
        }
    }
}
