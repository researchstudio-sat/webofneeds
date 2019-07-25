package won.matcher.service.common.service.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;

/**
 * Service to use HTTP to request resources
 * <p/>
 * User: hfriedrich Date: 04.05.2015
 */
@Component
@Scope("prototype")
public class HttpService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private RestTemplate restTemplate;
    private HttpHeaders jsonHeaders;

    public HttpService() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        init(factory);
    }

    public HttpService(int readTimeout, int connectionTimeout) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(readTimeout);
        factory.setConnectTimeout(connectionTimeout);
        init(factory);
    }

    private void init(ClientHttpRequestFactory factory) {
        restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter(Charset.forName("UTF-8")));
        jsonHeaders = new HttpHeaders();
        jsonHeaders.add("Content-Type", "application/json");
        jsonHeaders.add("Accept", "*/*");
    }

    public void postJsonRequest(String uri, String body) {
        ResponseEntity<String> response = null;
        logger.debug("POST URI: {}", uri);
        HttpEntity<String> jsonEntity = new HttpEntity(body, jsonHeaders);
        response = restTemplate.exchange(uri, HttpMethod.POST, jsonEntity, String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            logger.warn("HTTP POST request returned status code: {}", response.getStatusCode());
            throw new HttpClientErrorException(response.getStatusCode());
        }
    }
}
