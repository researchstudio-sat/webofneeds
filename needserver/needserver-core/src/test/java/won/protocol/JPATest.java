package won.protocol;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 02.11.12
 * Time: 16:17
 * To change this template use File | Settings | File Templates.
 */
public class JPATest {
    public static void main(final String[] args) {

        final AbstractApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        context.registerShutdownHook();
        final Executor executor = context.getBean(Executor.class);
        executor.execute();

    }
}
