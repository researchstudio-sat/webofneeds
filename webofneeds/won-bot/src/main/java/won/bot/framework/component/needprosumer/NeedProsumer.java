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

package won.bot.framework.component.needprosumer;

import won.bot.framework.component.needconsumer.NeedConsumer;
import won.bot.framework.component.needproducer.NeedProducer;

/**
 * User: fkleedorfer Date: 18.12.13
 */
public class NeedProsumer {
  private NeedProducer needProducer;
  private NeedConsumer needConsumer;

  public void consumeAll() {
    while (!needProducer.isExhausted() && !needConsumer.isExhausted()) {
      needConsumer.consume(needProducer.create());
    }
  }

  public NeedProducer getNeedProducer() {
    return needProducer;
  }

  public void setNeedProducer(final NeedProducer needProducer) {
    this.needProducer = needProducer;
  }

  public NeedConsumer getNeedConsumer() {
    return needConsumer;
  }

  public void setNeedConsumer(final NeedConsumer needConsumer) {
    this.needConsumer = needConsumer;
  }
}
