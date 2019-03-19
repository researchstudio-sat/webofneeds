/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.rest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class WonEtagHelperTests {
    @Test
    public void testForVersion() {
        WonEtagHelper helper = WonEtagHelper.forVersion("1");
        Assert.assertNotNull(helper);
        Assert.assertNull(helper.getMediaType());
        Assert.assertEquals("1", helper.getVersion());
        String etagValue = helper.getEtagString();
        Assert.assertEquals("\"1\"", etagValue);
    }

    @Test
    public void testFromEtagValueNoQuotes() {
        WonEtagHelper helper = WonEtagHelper.fromEtagValue("1");
        Assert.assertNull(helper);
    }

    @Test
    public void testFromEtagValueWeak() {
        WonEtagHelper helper = WonEtagHelper.fromEtagValue("W/\"1\"");
        Assert.assertNull(helper);
    }

    @Test
    public void testFromEtagValueNoMediaType() {
        WonEtagHelper helper = WonEtagHelper.fromEtagValue("\"1\"");
        Assert.assertNull(helper.getMediaType());
        Assert.assertEquals("1", helper.getVersion());
    }

    @Test
    public void testFromEtagValueInvalidMediaType() {
        WonEtagHelper helper = WonEtagHelper.fromEtagValue("\"1 /\"");
        Assert.assertNull(helper);
    }

    @Test
    public void testFromEtagValueValidMediaType() {
        WonEtagHelper helper = WonEtagHelper.fromEtagValue("\"1 application/trig\"");
        Assert.assertEquals("1", helper.getVersion());
        Assert.assertEquals(new MediaType("application", "trig"), helper.getMediaType());
    }

    @Test
    public void testSetMediatypeInHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setETag("\"1\"");
        WonEtagHelper.setMediaTypeForEtagHeaderIfPresent(new MediaType("application", "trig"), headers);
        Assert.assertEquals("\"1 application/trig\"", headers.getETag());
    }

    @Test
    public void testSetMediatypeInHeaderReplacingExistingMediaType() {
        HttpHeaders headers = new HttpHeaders();
        headers.setETag("\"1 application/n-quads\"");
        WonEtagHelper.setMediaTypeForEtagHeaderIfPresent(new MediaType("application", "trig"), headers);
        Assert.assertEquals("\"1 application/trig\"", headers.getETag());
    }

    @Test
    public void testfromHeaderIfCompatibleWithAcceptHeader_compatible() {
        HttpHeaders headers = new HttpHeaders();
        List<MediaType> mediaTypes = new ArrayList<>(3);
        mediaTypes.add(new MediaType("application", "*"));
        headers.setAccept(mediaTypes);
        headers.setIfNoneMatch("\"1 application/n-quads\"");
        WonEtagHelper helper = WonEtagHelper.fromHeaderIfCompatibleWithAcceptHeader(headers);
        Assert.assertNotNull(helper);
    }

    @Test
    public void testfromHeaderIfCompatibleWithAcceptHeader_incompatible() {
        HttpHeaders headers = new HttpHeaders();
        List<MediaType> mediaTypes = new ArrayList<>(3);
        mediaTypes.add(new MediaType("application", "*"));
        headers.setAccept(mediaTypes);
        headers.setIfNoneMatch("\"1 text/html");
        WonEtagHelper helper = WonEtagHelper.fromHeaderIfCompatibleWithAcceptHeader(headers);
        Assert.assertNull(helper);
    }

}
