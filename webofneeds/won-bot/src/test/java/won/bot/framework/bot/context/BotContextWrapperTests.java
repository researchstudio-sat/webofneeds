package won.bot.framework.bot.context;

import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.context.ApplicationContext;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.URI;
import java.util.*;

/**
 * Tests for bot context. For the mongo db implementation you have to make
 * manually sure that the connection toi the mongo db server exists and that the
 * database in empty on test start. Created by hfriedrich on 24.10.2016.
 */
@RunWith(value = Parameterized.class)
public class BotContextWrapperTests {
    private static final URI URI1 = URI.create("http://test.uri/number#1");
    private static final URI URI2 = URI.create("http://test.uri/number#2");
    private static final URI URI3 = URI.create("http://test.uri/number#3");
    private static final String BOT_NAME = "botTest";
    private static final String LIST_NAME_1 = BOT_NAME + ":uri1";
    private static final String LIST_NAME_2 = BOT_NAME + ":uri2";
    private static final String OBJECT_MAP_NAME_1 = BOT_NAME + ":col1";
    private static final String OBJECT_MAP_NAME_2 = BOT_NAME + ":col2";
    private static final String OBJECT_MAP_NAME_3 = BOT_NAME + ":col3";
    private static final String OBJECT_MAP_NAME_4 = BOT_NAME + ":addCol1";
    private static final String OBJECT_MAP_NAME_5 = BOT_NAME + ":addCol2";
    private static final String MAIL_NAME = BOT_NAME + ":addCol3";
    private static final String MAIL_LIST_NAME = BOT_NAME + ":addCol3";
    private static final String URI_LIST_NAME = BOT_NAME + ":uriList";
    private static final String URI_MAP_NAME = BOT_NAME + ":uriMap";
    private BotContext botContext;
    private BotContextWrapper botContextWrapper;
    private Class<Object> botContextClass;

    public BotContextWrapperTests(Class cl) {
        botContextClass = cl;
    }

    @Before
    public void setup() throws IllegalAccessException, InstantiationException {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("/botContext.xml");
        botContext = (BotContext) ctx.getBean(botContextClass);
        botContextWrapper = new BotContextWrapper(botContext, BOT_NAME, LIST_NAME_1, LIST_NAME_2);
    }

    @Parameterized.Parameters
    public static Iterable<Class[]> getTestParameters() {
        LinkedList<Class[]> linkedList = new LinkedList<>();
        linkedList.add(new Class[] { MemoryBotContext.class });
        // We exclude mongoBotContext for now TODO: find a better strategy for embedded
        // mongodb integration Testing
        // linkedList.add(new Class[] { MongoBotContext.class });
        return linkedList;
    }

    @After
    public void cleanUp() {
        botContext.dropCollection(LIST_NAME_1);
        botContext.dropCollection(LIST_NAME_2);
        botContext.dropCollection(OBJECT_MAP_NAME_1);
        botContext.dropCollection(OBJECT_MAP_NAME_2);
        botContext.dropCollection(OBJECT_MAP_NAME_3);
        botContext.dropCollection(OBJECT_MAP_NAME_4);
        botContext.dropCollection(OBJECT_MAP_NAME_5);
        botContext.dropCollection(MAIL_NAME);
        botContext.dropCollection(MAIL_LIST_NAME);
        botContext.dropCollection(URI_LIST_NAME);
        botContext.dropCollection(URI_MAP_NAME);
    }

    @Test
    public void testNamedAtomUriMethods() {
        botContext.dropCollection(LIST_NAME_1);
        botContext.dropCollection(LIST_NAME_2);
        botContext.dropCollection(botContextWrapper.getAtomCreateListName());
        botContext.dropCollection(botContextWrapper.getNodeListName());
        Assert.assertNotNull(botContextWrapper.retrieveAllAtomUris());
        Assert.assertEquals(0, botContextWrapper.retrieveAllAtomUris().size());
        botContext.appendToUriList(URI1, LIST_NAME_1);
        Assert.assertEquals(1, botContextWrapper.retrieveAllAtomUris().size());
        Assert.assertEquals(URI1, botContextWrapper.retrieveAllAtomUris().iterator().next());
        Assert.assertEquals(1, botContext.getUriList(LIST_NAME_1).size());
        Assert.assertEquals(URI1, botContext.getUriList(LIST_NAME_1).get(0));
        botContext.appendToUriList(URI2, LIST_NAME_2);
        botContext.appendToUriList(URI3, LIST_NAME_1);
        Assert.assertEquals(3, botContextWrapper.retrieveAllAtomUris().size());
        Assert.assertEquals(2, botContext.getUriList(LIST_NAME_1).size());
        Assert.assertTrue(botContext.getUriList(LIST_NAME_1).contains(URI1));
        Assert.assertTrue(botContext.getUriList(LIST_NAME_2).contains(URI2));
        Assert.assertTrue(botContext.getUriList(LIST_NAME_1).contains(URI3));
        Assert.assertFalse(botContext.getUriList(LIST_NAME_2).contains(URI1));
        Assert.assertFalse(botContext.getUriList(LIST_NAME_1).contains(URI2));
        Assert.assertFalse(botContext.getUriList(LIST_NAME_2).contains(URI3));
        Assert.assertTrue(botContextWrapper.isAtomKnown(URI1));
        Assert.assertTrue(botContextWrapper.isAtomKnown(URI2));
        Assert.assertTrue(botContextWrapper.isAtomKnown(URI3));
        botContext.appendToUriList(URI1, LIST_NAME_2);
        Assert.assertEquals(3, botContextWrapper.retrieveAllAtomUris().size());
        Assert.assertEquals(2, botContext.getUriList(LIST_NAME_1).size());
        Assert.assertEquals(2, botContext.getUriList(LIST_NAME_2).size());
        Assert.assertTrue(botContext.getUriList(LIST_NAME_1).contains(URI1));
        botContext.removeFromUriList(URI1, LIST_NAME_2); // removes URI1 from uri1 but not from uri2
        botContext.removeFromUriList(URI2, LIST_NAME_1);
        botContext.removeFromUriList(URI3, LIST_NAME_1);
        Assert.assertEquals(2, botContextWrapper.retrieveAllAtomUris().size()); // URI1 and URI2 should still be there
                                                                                // in the
        // general list
        Assert.assertEquals(1, botContext.getUriList(LIST_NAME_1).size());
        Assert.assertTrue(botContext.getUriList(LIST_NAME_1).contains(URI1));
        Assert.assertEquals(1, botContext.getUriList(LIST_NAME_2).size());
        Assert.assertTrue(botContext.getUriList(LIST_NAME_2).contains(URI2));
        botContext.removeFromUriList(URI1, LIST_NAME_1);
        botContext.removeFromUriList(URI2, LIST_NAME_2);
        Assert.assertNotNull(botContext.getUriList(LIST_NAME_1));
        Assert.assertEquals(0, botContext.getUriList(LIST_NAME_1).size());
        Assert.assertNotNull(botContextWrapper.retrieveAllAtomUris());
        Assert.assertEquals(0, botContextWrapper.retrieveAllAtomUris().size());
        botContext.dropCollection(LIST_NAME_1);
        botContext.dropCollection(LIST_NAME_2);
        botContext.dropCollection(botContextWrapper.getAtomCreateListName());
        botContext.dropCollection(botContextWrapper.getNodeListName());
    }

    @Test
    public void testNodeUriMethods() {
        botContext.dropCollection(botContextWrapper.getNodeListName());
        Assert.assertFalse(botContextWrapper.isNodeKnown(URI1));
        botContextWrapper.rememberNodeUri(URI1);
        Assert.assertTrue(botContextWrapper.isNodeKnown(URI1));
        botContextWrapper.rememberNodeUri(URI1);
        botContextWrapper.rememberNodeUri(URI2);
        Assert.assertTrue(botContextWrapper.isNodeKnown(URI1));
        Assert.assertTrue(botContextWrapper.isNodeKnown(URI2));
        botContextWrapper.removeNodeUri(URI1);
        Assert.assertFalse(botContextWrapper.isNodeKnown(URI1));
        Assert.assertTrue(botContextWrapper.isNodeKnown(URI2));
        botContextWrapper.rememberNodeUri(URI1);
        Assert.assertTrue(botContextWrapper.isNodeKnown(URI1));
        Assert.assertTrue(botContextWrapper.isNodeKnown(URI2));
        botContextWrapper.removeNodeUri(URI1);
        botContextWrapper.removeNodeUri(URI2);
        Assert.assertFalse(botContextWrapper.isNodeKnown(URI1));
        Assert.assertFalse(botContextWrapper.isNodeKnown(URI2));
        botContext.dropCollection(botContextWrapper.getNodeListName());
    }

    @Test
    public void testObjectMapMethods() {
        botContext.dropCollection(OBJECT_MAP_NAME_1);
        botContext.dropCollection(OBJECT_MAP_NAME_2);
        botContext.dropCollection(OBJECT_MAP_NAME_3);
        Assert.assertNotNull(botContext.loadObjectMap(OBJECT_MAP_NAME_1));
        Assert.assertEquals(0, botContext.loadObjectMap(OBJECT_MAP_NAME_1).size());
        botContext.saveToObjectMap(OBJECT_MAP_NAME_1, "uri1", URI1);
        botContext.saveToObjectMap(OBJECT_MAP_NAME_1, "uri1", URI1);
        botContext.saveToObjectMap(OBJECT_MAP_NAME_1, "uri2", URI1);
        botContext.saveToObjectMap(OBJECT_MAP_NAME_1, "uri2", URI2); // overwrite
        botContext.saveToObjectMap(OBJECT_MAP_NAME_2, "uri1", URI2);
        botContext.saveToObjectMap(OBJECT_MAP_NAME_2, "uri2", URI2);
        botContext.saveToObjectMap(OBJECT_MAP_NAME_2, "uri3", URI3);
        Assert.assertEquals(2, botContext.loadObjectMap(OBJECT_MAP_NAME_1).size());
        Assert.assertEquals(URI1, botContext.loadFromObjectMap(OBJECT_MAP_NAME_1, "uri1"));
        Assert.assertEquals(URI2, botContext.loadFromObjectMap(OBJECT_MAP_NAME_1, "uri2"));
        Assert.assertEquals(3, botContext.loadObjectMap(OBJECT_MAP_NAME_2).size());
        Assert.assertTrue(botContext.loadObjectMap(OBJECT_MAP_NAME_2).containsValue(URI2));
        Assert.assertTrue(botContext.loadObjectMap(OBJECT_MAP_NAME_2).containsValue(URI3));
        botContext.removeFromObjectMap(OBJECT_MAP_NAME_3, "uri1");
        botContext.removeFromObjectMap(OBJECT_MAP_NAME_2, "uri3");
        botContext.removeFromObjectMap(OBJECT_MAP_NAME_1, "uri1");
        botContext.removeFromObjectMap(OBJECT_MAP_NAME_1, "uri2");
        Assert.assertNull(botContext.loadFromObjectMap(OBJECT_MAP_NAME_1, "uri1"));
        Assert.assertEquals(0, botContext.loadObjectMap(OBJECT_MAP_NAME_1).size());
        Assert.assertEquals(2, botContext.loadObjectMap(OBJECT_MAP_NAME_2).size());
        botContext.dropCollection(OBJECT_MAP_NAME_1);
        Assert.assertEquals(2, botContext.loadObjectMap(OBJECT_MAP_NAME_2).size());
        botContext.dropCollection(OBJECT_MAP_NAME_2);
        Assert.assertEquals(0, botContext.loadObjectMap(OBJECT_MAP_NAME_2).size());
        botContext.dropCollection(OBJECT_MAP_NAME_1);
        botContext.dropCollection(OBJECT_MAP_NAME_2);
        botContext.dropCollection(OBJECT_MAP_NAME_3);
    }

    @Test
    public void testListMapMethods() {
        botContext.dropCollection(OBJECT_MAP_NAME_4);
        botContext.dropCollection(OBJECT_MAP_NAME_5);
        Assert.assertNotNull(botContext.loadListMap(OBJECT_MAP_NAME_4));
        Assert.assertEquals(0, botContext.loadListMap(OBJECT_MAP_NAME_4).size());
        Assert.assertNotNull(botContext.loadFromListMap(OBJECT_MAP_NAME_4, "list1"));
        Assert.assertEquals(0, botContext.loadFromListMap(OBJECT_MAP_NAME_4, "list1").size());
        botContext.addToListMap(OBJECT_MAP_NAME_4, "list1", URI1);
        botContext.addToListMap(OBJECT_MAP_NAME_4, "list1", URI2);
        botContext.addToListMap(OBJECT_MAP_NAME_4, "list2", URI1);
        botContext.addToListMap(OBJECT_MAP_NAME_4, "list2", URI1);
        botContext.addToListMap(OBJECT_MAP_NAME_4, "list2", URI2);
        botContext.addToListMap(OBJECT_MAP_NAME_5, "list1", URI2);
        botContext.addToListMap(OBJECT_MAP_NAME_5, "list2", URI1, URI2, URI3, URI1);
        Assert.assertEquals(2, botContext.loadFromListMap(OBJECT_MAP_NAME_4, "list1").size());
        Assert.assertTrue(botContext.loadFromListMap(OBJECT_MAP_NAME_4, "list1").contains(URI1));
        Assert.assertTrue(botContext.loadFromListMap(OBJECT_MAP_NAME_4, "list1").contains(URI2));
        Assert.assertEquals(2, botContext.loadListMap(OBJECT_MAP_NAME_4).size());
        Assert.assertEquals(2, botContext.loadListMap(OBJECT_MAP_NAME_4).get("list1").size());
        Assert.assertEquals(3, botContext.loadListMap(OBJECT_MAP_NAME_4).get("list2").size());
        Assert.assertEquals(4, botContext.loadFromListMap(OBJECT_MAP_NAME_5, "list2").size());
        botContext.removeFromListMap(OBJECT_MAP_NAME_4, "list2");
        botContext.removeFromListMap(OBJECT_MAP_NAME_5, "list2");
        Assert.assertEquals(2, botContext.loadFromListMap(OBJECT_MAP_NAME_4, "list1").size());
        Assert.assertEquals(0, botContext.loadFromListMap(OBJECT_MAP_NAME_4, "list2").size());
        Assert.assertEquals(1, botContext.loadFromListMap(OBJECT_MAP_NAME_5, "list1").size());
        Assert.assertEquals(URI2, botContext.loadFromListMap(OBJECT_MAP_NAME_5, "list1").get(0));
        botContext.dropCollection(OBJECT_MAP_NAME_4);
        botContext.dropCollection(OBJECT_MAP_NAME_5);
    }

    @Test
    public void testIsAtomKnown() throws Exception {
        URI rememberUri = new URI("http://rememberedUri");
        botContext.dropCollection(botContextWrapper.getAtomCreateListName());
        botContext.dropCollection(LIST_NAME_1);
        botContextWrapper.rememberAtomUri(rememberUri);
        Assert.assertTrue(botContextWrapper.isAtomKnown(rememberUri));
        botContext.appendToUriList(rememberUri, LIST_NAME_1);
        botContextWrapper.removeAtomUri(rememberUri);
        Assert.assertTrue(botContextWrapper.isAtomKnown(rememberUri));
        botContext.removeFromUriList(rememberUri, LIST_NAME_1);
        Assert.assertFalse(botContextWrapper.isAtomKnown(rememberUri));
    }

    @Test
    public void useEmailAddressAsKey() {
        botContext.dropCollection(MAIL_NAME);
        botContext.dropCollection(MAIL_LIST_NAME);
        String mailAddress = "first.last!#$%&'*+-/=?^_`{|}~@TEST123.com";
        botContext.saveToObjectMap(MAIL_NAME, mailAddress, mailAddress);
        Assert.assertEquals(mailAddress, botContext.loadFromObjectMap(MAIL_NAME, mailAddress));
        botContext.addToListMap(MAIL_LIST_NAME, mailAddress, 1, 2, 3);
        Assert.assertEquals(3, botContext.loadFromListMap(MAIL_LIST_NAME, mailAddress).size());
        botContext.dropCollection(MAIL_NAME);
        botContext.dropCollection(MAIL_LIST_NAME);
    }

    @Test
    public void testPutAndRetrieveGenericObjects() throws MessagingException, IOException, ClassNotFoundException {
        // List
        botContext.dropCollection(URI_LIST_NAME);
        LinkedList<URI> uriList = new LinkedList<>();
        uriList.add(URI1);
        uriList.add(URI1);
        uriList.add(URI2);
        uriList.add(URI3);
        botContext.saveToObjectMap(URI_LIST_NAME, "list1", uriList);
        List<URI> uriListCopy = (List<URI>) botContext.loadFromObjectMap(URI_LIST_NAME, "list1");
        Assert.assertEquals(uriList, uriListCopy);
        // HashMap needs to be serialized (here only non-complex keys are allowed in
        // maps)
        botContext.dropCollection(URI_MAP_NAME);
        HashMap<String, URI> uriHashMap = new HashMap<>();
        uriHashMap.put(URI1.toString(), URI1);
        uriHashMap.put(URI2.toString(), URI1);
        uriHashMap.put(URI3.toString(), URI3);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(uriHashMap);
        botContext.saveToObjectMap(URI_MAP_NAME, "map1", os.toByteArray());
        byte[] byteMsg = (byte[]) botContext.loadFromObjectMap(URI_MAP_NAME, "map1");
        ByteArrayInputStream is = new ByteArrayInputStream(byteMsg);
        ObjectInputStream ois = new ObjectInputStream(is);
        HashMap<String, URI> uriHashMapCopy = (HashMap<String, URI>) ois.readObject();
        Assert.assertEquals(uriHashMap, uriHashMapCopy);
        oos.close();
        os.close();
        ois.close();
        is.close();
        // TreeMap is serializable
        TreeMap<String, URI> uriTreeMap = new TreeMap<>();
        uriTreeMap.put(URI1.toString(), URI2);
        uriTreeMap.put(URI2.toString(), URI3);
        uriTreeMap.put(URI3.toString(), URI1);
        botContext.saveToObjectMap(URI_MAP_NAME, "map1", uriTreeMap); // overwrite the HashMap entry from the previous
                                                                      // step
        Map<String, URI> uriTreeMapCopy = (Map<String, URI>) botContext.loadFromObjectMap(URI_MAP_NAME, "map1");
        Assert.assertEquals(uriTreeMap, uriTreeMapCopy);
        // MimeMessage cannot be serialized directly => has to be serialized manually
        // first
        botContext.dropCollection("mime");
        Properties props = new Properties();
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(props, null));
        message.addHeader("test1", "test2");
        message.addHeaderLine("test3");
        message.addRecipients(Message.RecipientType.TO, "mail@test.test");
        message.setText("text");
        message.setSubject("text1");
        message.setDescription("text2");
        message.setSentDate(new Date());
        os = new ByteArrayOutputStream();
        message.writeTo(os);
        botContext.saveToObjectMap("mime", "mime1", os.toByteArray());
        byteMsg = (byte[]) botContext.loadFromObjectMap("mime", "mime1");
        is = new ByteArrayInputStream(byteMsg);
        MimeMessage messageCopy = new MimeMessage(Session.getDefaultInstance(props, null), is);
        Assert.assertEquals(message.getHeader("test1")[0], messageCopy.getHeader("test1")[0]);
        Assert.assertEquals(message.getAllHeaderLines().nextElement(), messageCopy.getAllHeaderLines().nextElement());
        Assert.assertEquals(message.getRecipients(Message.RecipientType.TO)[0],
                        messageCopy.getRecipients(Message.RecipientType.TO)[0]);
        Assert.assertEquals(message.getSubject(), messageCopy.getSubject());
        Assert.assertEquals(message.getDescription(), messageCopy.getDescription());
        Assert.assertEquals(message.getSentDate(), messageCopy.getSentDate());
        oos.close();
        os.close();
        ois.close();
        is.close();
    }
}
