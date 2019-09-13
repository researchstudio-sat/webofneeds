package won.bot.app;

import org.springframework.boot.SpringApplication;
// import org.springframework.context.ConfigurableApplicationContext;

/**
 * Created by fsuda on 14.12.2016.
 */
public class Telegram2WonBotApp {
    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication("classpath:/spring/app/telegram2wonBotApp.xml");
        app.setWebEnvironment(false);
        app.run(args);
        // ConfigurableApplicationContext applicationContext = app.run(args);
        // Thread.sleep(5*60*1000);
        // app.exit(applicationContext);
    }
}
