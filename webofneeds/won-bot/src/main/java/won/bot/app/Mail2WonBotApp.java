package won.bot.app;

import org.springframework.boot.SpringApplication;
// import org.springframework.context.ConfigurableApplicationContext;

/**
 * Created by fsuda on 27.09.2016.
 */
public class Mail2WonBotApp {
    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(new Object[] { "classpath:/spring/app/mail2wonBotApp.xml" });
        app.setWebEnvironment(false);
        app.run(args);
        // ConfigurableApplicationContext applicationContext = app.run(args);
        // Thread.sleep(5*60*1000);
        // app.exit(applicationContext);
    }
}
