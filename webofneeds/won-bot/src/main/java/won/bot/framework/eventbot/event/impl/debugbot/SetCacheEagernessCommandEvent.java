package won.bot.framework.eventbot.event.impl.debugbot;

import won.bot.framework.eventbot.event.BaseEvent;

public class SetCacheEagernessCommandEvent extends BaseEvent {

	private boolean eager;
	
	public SetCacheEagernessCommandEvent(boolean eager) {
		this.eager = eager;
	}

	public boolean isEager() {
		return eager;
	}
}
