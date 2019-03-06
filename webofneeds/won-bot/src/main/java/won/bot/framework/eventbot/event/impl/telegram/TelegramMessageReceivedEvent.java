package won.bot.framework.eventbot.event.impl.telegram;

import org.telegram.telegrambots.api.objects.Update;

import won.bot.framework.eventbot.event.BaseEvent;

public class TelegramMessageReceivedEvent extends BaseEvent {
    private final Update update;

    public TelegramMessageReceivedEvent(Update update) {
        this.update = update;
    }

    public Update getUpdate() {
        return update;
    }
}
