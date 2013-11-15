package won.protocol.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/**
 * User: sbyim
 * Date: 11.11.13
 */
public class PropertiesUtil {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public void updateProperty(String propertyFileName, String propertyName, String value) throws IOException, URISyntaxException {
        URL propertyFilePath = this.getClass().getResource(propertyFileName);
        File propertyFile = new File(propertyFilePath.toURI());
        FileInputStream fin = new FileInputStream(propertyFile);
        Properties props = new Properties();
        props.load(fin);
        fin.close();
       /* InputStream in = this.getClass().getResourceAsStream(propertyFileName);
        Properties props = new Properties();
        props.load(in);
        in.close();
                      */


        FileOutputStream out = new FileOutputStream(propertyFile);
        props.setProperty(propertyName, value);
        props.store(out,null);
        out.close();
    }
    public String readProperty(String propertyFileName, String propertyName) throws IOException {

        InputStream in = this.getClass().getResourceAsStream(propertyFileName);

        Properties props = new Properties();
        props.load(in);
        String result = props.getProperty(propertyName);
        in.close();
        return result;
        /*
        FileInputStream in = new FileInputStream(propertyFileName);
        Properties props = new Properties();
        String result = props.getProperty(propertyName);
        in.close();
        return result;        */
    }


}
