/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.bot.framework.eventbot.filter.impl;

import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.cmd.CommandEvent;
import won.bot.framework.eventbot.event.impl.cmd.CommandResultEvent;
import won.bot.framework.eventbot.filter.EventFilter;

/**
 * Accepts only CommandResultEvents that are results of the specified command.
 */
public class CommandResultFilter implements EventFilter {
    private CommandEvent orginalCommand;

    public CommandResultFilter(CommandEvent orginalCommand) {
        this.orginalCommand = orginalCommand;
    }

    @Override
    public boolean accept(Event event) {
        if (!(event instanceof CommandResultEvent)) return false;
        return ((CommandResultEvent)event).getOriginalCommandEvent() == orginalCommand;
    }
}
