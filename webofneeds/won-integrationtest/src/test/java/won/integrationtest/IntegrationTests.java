package won.integrationtest;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import won.test.category.RequiresDockerServer;
import won.utils.dns.DnsMappingAdder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Base class for all tests that require the won services to run. The services
 * are only started once for all tests. If the environment variable
 * 'START_CONTAINERS' (default:true) is set to false, the services are not
 * started - in that case the tests assume the services (wonnode, owner,
 * matcher_service) to be reachable at the configured ports. (see
 * src/test/resources/docker-compose.yml)
 */
@Category(RequiresDockerServer.class)
public abstract class IntegrationTests {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static boolean startContainers = Optional.ofNullable(System.getenv("START_CONTAINERS"))
                    .map(Boolean::valueOf).orElse(true);
    @ClassRule
    public static DockerComposeContainer environment = new Supplier<DockerComposeContainer>() {
        public DockerComposeContainer get() {
            if (startContainers) {
                DockerComposeContainer container = null;
                try {
                    container = new DockerComposeContainer(
                                    new File("target/test-classes/docker-compose.yml"))
                                                    .withExposedService("owner", 8082,
                                                                    Wait.forLogMessage(
                                                                                    // "^.+connected with WoN
                                                                                    // node:.+https://wonnode:8443/won/resource.*$",
                                                                                    "^.+connected with WoN node:.+$",
                                                                                    1)
                                                                                    .withStartupTimeout(Duration
                                                                                                    .ofSeconds(120)))
                                                    .waitingFor("matcher_service",
                                                                    Wait.forLogMessage(
                                                                                    "^.+\\[akka://ClusterSystem/user/WonNodeControllerActor\\] registered won node .+ and start crawling it.+$",
                                                                                    1))
                                                    .withLocalCompose(true);
                } catch (Exception e) {
                    return null;
                }
                return container;
            } else {
                return null;
            }
        }
    }.get();
    static {
        // the containers in our docker-compose file must be reachable from outside the
        // docker environment
        // by the names they have in the compose config (actually only needed for
        // wonnode, but for completeness,
        // we map all)
        DnsMappingAdder.addDnsMapping("owner", "127.0.0.1");
        DnsMappingAdder.addDnsMapping("wonnode", "127.0.0.1");
        DnsMappingAdder.addDnsMapping("postgres", "127.0.0.1");
        DnsMappingAdder.addDnsMapping("bigdata", "127.0.0.1");
    }

    @BeforeClass
    public static void checkDockerAvailable() {
        Assume.assumeTrue(isDockerAvailable());
    }

    private static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    @BeforeClass
    public static void logContainerStartupMessage() {
        if (startContainers) {
            logger.info("Running tests against containers managed by testcontainers framework");
            logger.info("To run against externally managed containers, set environment variable 'START_CONTAINERS' to 'false'");
        } else {
            logger.info("Running against externally managed containers as environment variable 'START_CONTAINERS' is 'false'");
        }
    }

    @Test(timeout = 10 * 1000)
    public void testOwnerReachable() throws Exception {
        CloseableHttpClient httpclient = getHttpClientThatTrustsAnyCert();
        HttpGet httpget = new HttpGet("https://owner:8082/owner");
        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertTrue(200 <= statusCode && 300 > statusCode);
        } finally {
            httpclient.close();
        }
    }

    @Test(timeout = 10 * 1000)
    public void testWonnodeReachable() throws Exception {
        CloseableHttpClient httpclient = getHttpClientThatTrustsAnyCert();
        HttpGet httpget = new HttpGet("https://owner:8443/won/resource");
        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertTrue(400 > statusCode);
        } finally {
            httpclient.close();
        }
    }

    protected CloseableHttpClient getHttpClientThatTrustsAnyCert() throws Exception {
        SSLContext sslcontext = SSLContexts.custom()
                        .loadTrustMaterial(new TrustStrategy() {
                            @Override
                            public boolean isTrusted(X509Certificate[] x509Certificates, String s)
                                            throws CertificateException {
                                return true;
                            }
                        })
                        .build();
        sslcontext.init(null, new TrustManager[] {
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType)
                                            throws CertificateException {
                                // accept any
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType)
                                            throws CertificateException {
                                // accept any
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                        }
        }, null);
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                        sslcontext,
                        SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        CloseableHttpClient httpclient = HttpClients.custom()
                        .setSSLSocketFactory(sslsf)
                        .build();
        return httpclient;
    }
}
