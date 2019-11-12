package won.bot.framework.eventbot.action.impl.wonmessage;

import java.lang.invoke.MethodHandles;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageSentEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;

public class PrintWonMessageAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public PrintWonMessageAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof MessageEvent)) {
            return;
        }
        WonMessage msg = ((MessageEvent) event).getWonMessage();
        if (msg.getMessageType().isResponseMessage()) {
            // we don't print responses
            return;
        }
        if (event instanceof WonMessageSentEvent) {
            handleSentMessage(msg);
        } else {
            handleReceivedMessage(msg);
        }
    }

    private void handleReceivedMessage(WonMessage msg) {
        System.out.println(makeMessageBox("                <---- Received Message <---- ", msg.toStringForDebug(true),
                        120));
    }

    private void handleSentMessage(WonMessage msg) {
        System.out.println(
                        makeMessageBox(" ----> Sent Message ---->                ", msg.toStringForDebug(true), 120));
    }

    private BiFunction<String, Integer, String> centerLayout = (text, width) -> {
        if (text == null) {
            return null;
        }
        if (text.length() >= width) {
            return text;
        }
        int spare = width - text.length();
        int half = spare / 2;
        int rightHalf = (half * 2) == spare ? half : half + 1;
        StringBuilder sb = new StringBuilder();
        sb
                        .append(rep(' ', half))
                        .append(text)
                        .append(rep(' ', rightHalf));
        return sb.toString();
    };
    private BiFunction<String, Integer, String> leftLayout = (text, width) -> {
        if (text == null) {
            return null;
        }
        if (text.length() >= width) {
            return text;
        }
        int spare = width - text.length();
        StringBuilder sb = new StringBuilder();
        sb
                        .append(text)
                        .append(rep(' ', spare));
        return sb.toString();
    };

    private String rep(char character, int times) {
        StringBuilder sb = new StringBuilder();
        Stream.generate(() -> character).limit(times).forEach(c -> sb.append(c));
        return sb.toString();
    }

    protected String makeMessageBox(String header, String body, int textWidth) {
        int padding = 2;
        int width = textWidth + 2 * padding;
        StringBuilder box = new StringBuilder();
        // header upper border
        box
                        .append('+')
                        .append(rep('=', width))
                        .append('+')
                        .append('\n');
        // header
        box.append(surround(lineBreaks(header, textWidth, centerLayout), ' ', '|', padding));
        // header lower border
        box
                        .append('+')
                        .append(rep('-', width))
                        .append('+')
                        .append('\n');
        // body
        box.append(surround(lineBreaks(body, textWidth, leftLayout), ' ', '|', padding));
        box
                        .append('+')
                        .append(rep('=', width))
                        .append('+')
                        .append('\n');
        return "\n" + box.toString() + "\n";
    }

    private String lineBreaks(String text, int textWidth, BiFunction<String, Integer, String> layout) {
        String[] lines = text.split("\\r?\\n");
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() <= textWidth) {
                formatted
                                .append(layout.apply(line, textWidth))
                                .append('\n');
            } else {
                formatted
                                .append(line.substring(0, textWidth))
                                .append('\n');
                lines[i] = line.substring(textWidth);
                i--; // process same array item again
            }
        }
        return formatted.toString();
    }

    private String surround(String text, char filler, char border, int padding) {
        String[] lines = text.split("\\r?\\n");
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            formatted
                            .append(border)
                            .append(rep(filler, padding))
                            .append(lines[i])
                            .append(rep(filler, padding))
                            .append(border)
                            .append('\n');
        }
        return formatted.toString();
    }
}
