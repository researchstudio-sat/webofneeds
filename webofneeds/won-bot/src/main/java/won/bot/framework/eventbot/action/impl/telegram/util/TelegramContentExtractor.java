package won.bot.framework.eventbot.action.impl.telegram.util;

import won.protocol.model.BasicNeedType;

import java.util.regex.Pattern;

public class TelegramContentExtractor {
    private Pattern demandTypePattern;
    private Pattern supplyTypePattern;
    private Pattern doTogetherTypePattern;
    private Pattern critiqueTypePattern;

    public BasicNeedType getBasicNeedType(String subject){
        if (demandTypePattern.matcher(subject).matches()) {
            return BasicNeedType.DEMAND;
        } else if (supplyTypePattern.matcher(subject).matches()) {
            return BasicNeedType.SUPPLY;
        } else if (doTogetherTypePattern.matcher(subject).matches()) {
            return BasicNeedType.DO_TOGETHER;
        } else if (critiqueTypePattern.matcher(subject).matches()) {
            return BasicNeedType.CRITIQUE;
        }

        return null;
    }

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
