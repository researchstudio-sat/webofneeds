package won.bot.app;

import org.springframework.boot.SpringApplication;
// import org.springframework.context.ConfigurableApplicationContext;

/**
 * Created by MS on 17.09.2018.
 */
public class HokifyJobBotApp {
    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication("classpath:/spring/app/hokifyJobBotApp.xml");
        app.setWebEnvironment(false);
        app.run(args);
        // ConfigurableApplicationContext applicationContext = app.run(args);
        // Thread.sleep(5*60*1000);
        // app.exit(applicationContext);
    }
}
