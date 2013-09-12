package won.matcher.webapp;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: moru
 * Date: 11/09/13
 * Time: 20:13
 * To change this template use File | Settings | File Templates.
 */
@Configuration
@EnableWebMvc
@ComponentScan("won.matcher.webapp")
public class WebConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> httpMessageConverters) {

        //httpMessageConverters.add(new RdfModelConverter(new MediaType("text", "csv")));
        httpMessageConverters.add(new RdfModelConverter(new MediaType("application", "ld+json")));
        //TODO stopped here (verify the media type is possible), test the RIOT json-ld capabilities
    }
}