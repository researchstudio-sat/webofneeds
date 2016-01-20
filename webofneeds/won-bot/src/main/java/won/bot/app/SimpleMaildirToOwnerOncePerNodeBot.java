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

package won.bot.app;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;
import won.bot.framework.component.needprosumer.NeedProsumer;

@Component
public class SimpleMaildirToOwnerOncePerNodeBot implements CommandLineRunner
{
  @Autowired
  private NeedProsumer prosumer;
  @Autowired
  CamelContext camelContext;

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(
        new Object[]{"classpath:/spring/app/readMailsFromFolder-emulateOwner-oncePerNode-testBot.xml"}
    );
    app.setWebEnvironment(false);
    app.run(args);
  }

  @Override
  public void run(final String... strings) throws Exception
  {
    prosumer.consumeAll();
    camelContext.stop();
  }


}
