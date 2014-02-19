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

package won.bot.framework.component.needproducer.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.component.needproducer.NeedProducer;
import won.protocol.util.NeedModelBuilder;

/**
 * NeedProducer that is configured to read needs from a directory.
 */
public class CommentNeedProducer implements NeedProducer
{

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private boolean created = false;

  @Override
  public Model create()
  {
      NeedModelBuilder needModelBuilder = new NeedModelBuilder();
      needModelBuilder.setUri("no:uri");
      created = true;
      return needModelBuilder.build();
  }

    @Override
    public Model create(Class clazz) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
  public boolean isExhausted()
  {
      return created;
  }





}
