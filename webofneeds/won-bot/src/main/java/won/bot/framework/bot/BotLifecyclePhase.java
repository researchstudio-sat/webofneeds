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

package won.bot.framework.bot;

/**
 * Lifecycle phases of a bot.
 */
public enum BotLifecyclePhase
{
  //not initialized, changes to STARTING_UP through a call to bot.initialize(). May have been reached through a call to bot.shutdown().
  DOWN,
  //bot.initialize() is being executed
  STARTING_UP,
  //bot.initialize() is done
  ACTIVE,
  //bot.shutdown() is being executed. Next state is DOWN.
  SHUTTING_DOWN;

  public boolean isDown(){ return this == DOWN;}
  public boolean isStartingUp(){ return this == STARTING_UP;}
  public boolean isActive(){ return this == ACTIVE;}
  public boolean isShuttingDown(){ return this == SHUTTING_DOWN;}

}
