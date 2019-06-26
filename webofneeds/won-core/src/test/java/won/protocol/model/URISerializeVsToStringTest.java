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
package won.protocol.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.StopWatch;

/**
 * Created by fkleedorfer on 24.08.2016.
 */
public class URISerializeVsToStringTest {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        StopWatch readWatch = new StopWatch();
        StopWatch writeWatch = new StopWatch();
        StopWatch stopWatch = new StopWatch();
        String myuri = "https://192.168.124.49:8443/won/resource/atom/561548487155823600";
        readWatch.start();
        readWatch.suspend();
        writeWatch.start();
        writeWatch.suspend();
        stopWatch.start();
        stopWatch.suspend();
        for (int i = 0; i < 1000 * 1000; i++) {
            String toParse = myuri + RandomStringUtils.randomAlphanumeric(10);
            stopWatch.resume();
            readWatch.resume();
            URI theURI = URI.create(toParse);
            readWatch.suspend();
            writeWatch.resume();
            String anotherString = theURI.toString();
            writeWatch.suspend();
            stopWatch.suspend();
        }
        // System.out.println("test1 took " + stopWatch.getTime() + " millis");
        // System.out.println("write test2 took " + writeWatch.getTime() + " millis");
        // System.out.println("read test2 took " + readWatch.getTime() + " millis");
        readWatch.reset();
        readWatch.start();
        readWatch.suspend();
        writeWatch.reset();
        writeWatch.start();
        writeWatch.suspend();
        stopWatch.reset();
        stopWatch.start();
        stopWatch.suspend();
        for (int i = 0; i < 1000 * 1000; i++) {
            URI theURI = URI.create(myuri + RandomStringUtils.randomAlphanumeric(10));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            stopWatch.resume();
            writeWatch.resume();
            oos.writeObject(theURI);
            writeWatch.suspend();
            byte[] data = baos.toByteArray();
            stopWatch.suspend();
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            stopWatch.resume();
            readWatch.resume();
            URI theSameURI = (URI) ois.readObject();
            readWatch.suspend();
            stopWatch.suspend();
        }
        // System.out.println("test2 took " + stopWatch.getTime() + " millis");
        // System.out.println("write test2 took " + writeWatch.getTime() + " millis");
        // System.out.println("read test2 took " + readWatch.getTime() + " millis");
    }
}
