package won.bot.framework.eventbot.action.impl.telegram.util;

import java.util.regex.Pattern;

public class TelegramContentExtractor {
    private Pattern demandTypePattern;
    private Pattern supplyTypePattern;
    private Pattern doTogetherTypePattern;
    private Pattern critiqueTypePattern;

    //Spring setter

    public void setDemandTypePattern(final Pattern demandTypePattern) {
        this.demandTypePattern = demandTypePattern;
    }

    public void setSupplyTypePattern(final Pattern supplyTypePattern) {
        this.supplyTypePattern = supplyTypePattern;
    }

    public void setDoTogetherTypePattern(final Pattern doTogetherTypePattern) {
        this.doTogetherTypePattern = doTogetherTypePattern;
    }

    public void setCritiqueTypePattern(final Pattern critiqueTypePattern) {
        this.critiqueTypePattern = critiqueTypePattern;
    }
}
