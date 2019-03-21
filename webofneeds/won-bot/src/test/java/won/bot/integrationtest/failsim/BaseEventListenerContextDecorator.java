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

package won.bot.integrationtest.failsim;

import org.springframework.scheduling.TaskScheduler;
import won.bot.framework.bot.context.BotContext;
import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.component.needproducer.NeedProducer;
import won.bot.framework.component.nodeurisource.NodeURISource;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.bus.EventBus;
import won.matcher.component.MatcherNodeURISource;
import won.matcher.protocol.impl.MatcherProtocolMatcherServiceImplJMSBased;
import won.protocol.matcher.MatcherProtocolNeedServiceClientSide;
import won.protocol.message.sender.WonMessageSender;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.net.URI;
import java.util.concurrent.Executor;

/**
 * Delegates calls to another EventListenerContext, allowing to set proxies for
 * some of the services offered by the ELC.
 */
public abstract class BaseEventListenerContextDecorator implements EventListenerContext {
  protected EventListenerContext delegate;

  public BaseEventListenerContextDecorator(EventListenerContext delegate) {
    this.delegate = delegate;
  }

  @Override
  public TaskScheduler getTaskScheduler() {
    return delegate.getTaskScheduler();
  }

  @Override
  public URI getSolrServerURI() {
    return delegate.getSolrServerURI();
  }

  @Override
  public NodeURISource getNodeURISource() {
    return delegate.getNodeURISource();
  }

  @Override
  public MatcherNodeURISource getMatcherNodeURISource() {
    return delegate.getMatcherNodeURISource();
  }

  @Override
  public WonMessageSender getWonMessageSender() {
    return delegate.getWonMessageSender();
  }

  @Override
  public MatcherProtocolNeedServiceClientSide getMatcherProtocolNeedServiceClient() {
    return delegate.getMatcherProtocolNeedServiceClient();
  }

  @Override
  public MatcherProtocolMatcherServiceImplJMSBased getMatcherProtocolMatcherService() {
    return delegate.getMatcherProtocolMatcherService();
  }

  @Override
  public NeedProducer getNeedProducer() {
    return delegate.getNeedProducer();
  }

  @Override
  public void cancelTrigger() {
    delegate.cancelTrigger();
  }

  @Override
  public void workIsDone() {
    delegate.workIsDone();
  }

  @Override
  public EventBus getEventBus() {
    return delegate.getEventBus();
  }

  @Override
  public BotContext getBotContext() {
    return delegate.getBotContext();
  }

  @Override
  public BotContextWrapper getBotContextWrapper() {
    return delegate.getBotContextWrapper();
  }

  @Override
  public Executor getExecutor() {
    return delegate.getExecutor();
  }

  @Override
  public LinkedDataSource getLinkedDataSource() {
    return delegate.getLinkedDataSource();
  }

  @Override
  public WonNodeInformationService getWonNodeInformationService() {
    return delegate.getWonNodeInformationService();
  }

}
