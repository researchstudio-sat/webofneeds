/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.bot.app;

import org.springframework.boot.SpringApplication;
// import org.springframework.context.ConfigurableApplicationContext;

/**
 * App for EchoBot.
 * The echo bot echoes all sent messages back to the user.
 */
public class EchoBotApp {
    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(new Object[] { "classpath:/spring/app/echoBotApp.xml" });
        app.setWebEnvironment(false);
        app.run(args);
        // ConfigurableApplicationContext applicationContext = app.run(args);
        // Thread.sleep(5*60*1000);
        // app.exit(applicationContext);
    }
}
