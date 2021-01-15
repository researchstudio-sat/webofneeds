package won.integrationtest;

import com.alibaba.dcm.DnsCacheManipulator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import won.test.category.RequiresDockerServer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Category(RequiresDockerServer.class)
public class IntegrationTests {
    @ClassRule
    public static DockerComposeContainer environment = new DockerComposeContainer(
                    new File("target/test-classes/docker-compose.yml"))
                                    .withExposedService("owner", 8082,
                                                    Wait.forLogMessage(
                                                                    "^.+connected with WoN node:.+https://wonnode:8443/won/resource.*$",
                                                                    1)
                                                                    .withStartupTimeout(Duration.ofSeconds(60)));

    @BeforeClass
    public static void addWonnodeToLocalhostDnsEntry() {
        DnsCacheManipulator.setDnsCache("wonnode", "127.0.0.1");
    }

    @Test
    public void testOwnerReachable() throws Exception {
        CloseableHttpClient httpclient = getHttpClientThatTrustsAnyCert();
        HttpGet httpget = new HttpGet("https://localhost:8082/owner");
        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertTrue(200 <= statusCode && 300 > statusCode);
        } finally {
            httpclient.close();
        }
    }

    @Test
    public void testWonnodeReachable() throws Exception {
        CloseableHttpClient httpclient = getHttpClientThatTrustsAnyCert();
        HttpGet httpget = new HttpGet("https://localhost:8443/won/resource");
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
