package won.bot.framework.eventbot.action.impl.telegram.receive;

import org.telegram.telegrambots.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import won.bot.framework.bot.context.TelegramBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.telegram.WonTelegramBotHandler;
import won.bot.framework.eventbot.action.impl.telegram.util.TelegramContentExtractor;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.SendTextMessageOnConnectionEvent;
import won.bot.framework.eventbot.event.impl.mail.CloseConnectionEvent;
import won.bot.framework.eventbot.event.impl.mail.OpenConnectionEvent;
import won.bot.framework.eventbot.event.impl.telegram.TelegramMessageReceivedEvent;
import won.bot.framework.eventbot.listener.EventListener;

public class TelegramMessageReceivedAction extends BaseEventBotAction {
    private TelegramContentExtractor telegramContentExtractor;
    private WonTelegramBotHandler wonTelegramBotHandler;

    public TelegramMessageReceivedAction(EventListenerContext eventListenerContext, WonTelegramBotHandler wonTelegramBotHandler, TelegramContentExtractor telegramContentExtractor) {
        super(eventListenerContext);
        this.wonTelegramBotHandler = wonTelegramBotHandler;
        this.telegramContentExtractor = telegramContentExtractor;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventBus bus = getEventListenerContext().getEventBus();
        EventListenerContext ctx = getEventListenerContext();

        if(event instanceof TelegramMessageReceivedEvent && ctx.getBotContextWrapper() instanceof TelegramBotContextWrapper){
            TelegramBotContextWrapper botContextWrapper = (TelegramBotContextWrapper) ctx.getBotContextWrapper();

            Update update = ((TelegramMessageReceivedEvent) event).getUpdate();
            Message message = update.getMessage();
            CallbackQuery callbackQuery = update.getCallbackQuery();

            if(message != null && message.isCommand()){
                wonTelegramBotHandler.getCommandRegistry().executeCommand(wonTelegramBotHandler, message);
            }else if (callbackQuery != null && update.hasCallbackQuery()) {
                message = callbackQuery.getMessage();
                String data = callbackQuery.getData();
                WonURI correspondingURI = botContextWrapper.getWonURIForMessageId(message.getMessageId());

                AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                answerCallbackQuery.setCallbackQueryId(callbackQuery.getId());

                switch (correspondingURI.getType()) {
                    case NEED:
                        break;
                    case CONNECTION:
                        if("0".equals(data)) { //CLOSE CONNECTION
                            bus.publish(new CloseConnectionEvent(correspondingURI.getUri()));
                            answerCallbackQuery.setText("Closed Connection");
                        } else if("1".equals(data)) { //ACCEPT CONNECTION
                            bus.publish(new OpenConnectionEvent(correspondingURI.getUri()));
                            answerCallbackQuery.setText("Opened Connection");
                        }
                        break;
                }
                wonTelegramBotHandler.answerCallbackQuery(answerCallbackQuery);
                EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
                editMessageReplyMarkup.setMessageId(message.getMessageId());
                editMessageReplyMarkup.setChatId(message.getChatId());
                editMessageReplyMarkup.setReplyMarkup(null);

                wonTelegramBotHandler.editMessageReplyMarkup(editMessageReplyMarkup);
            }else if(message != null && message.isReply() && message.hasText()){
                WonURI correspondingURI = botContextWrapper.getWonURIForMessageId(message.getReplyToMessage().getMessageId());
                bus.publish(new SendTextMessageOnConnectionEvent(message.getText(), correspondingURI.getUri()));
            }
        }
    }
}
