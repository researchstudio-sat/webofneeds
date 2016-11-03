package won.bot.framework.bot.context;

import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContextManager;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.URI;
import java.util.*;

/**
 * Tests for bot context.
 * For the mongo db implementation you have to make manually sure that the connection toi the mongo db server exists
 * and that the database in empty on test start.
 *
 * Created by hfriedrich on 24.10.2016.
 */
@RunWith(value = Parameterized.class)
public class BotContextTests
{
  private static final URI URI1 = URI.create("http://test.uri/number#1");
  private static final URI URI2 = URI.create("http://test.uri/number#2");
  private static final URI URI3 = URI.create("http://test.uri/number#3");

  private TestContextManager testContextManager;

  private BotContext botContext;
  private Class botContextClass;
  private ApplicationContext context;

  public BotContextTests(Class cl) {
    botContextClass = cl;
  }

  @Before
  public void setup() throws IllegalAccessException, InstantiationException {

    ApplicationContext ctx = new ClassPathXmlApplicationContext("/botContext.xml");
    botContext = (BotContext) ctx.getBean(botContextClass);
  }

  @Parameterized.Parameters
  public static Iterable<Class[]> getTestParameters() {
    LinkedList<Class[]> linkedList = new LinkedList<>();
    linkedList.add(new Class[] {MemoryBotContext.class});
    linkedList.add(new Class[] {MongoBotContext.class});
    return linkedList;
  }

  @Test
  public void testNamedNeedUriMethods() {

    Assert.assertNotNull(botContext.retrieveAllNeedUris());
    Assert.assertEquals(0, botContext.retrieveAllNeedUris().size());

    botContext.appendToNamedNeedUriList(URI1, "uri1");

    Assert.assertEquals(1, botContext.retrieveAllNeedUris().size());
    Assert.assertEquals(URI1, botContext.retrieveAllNeedUris().iterator().next());
    Assert.assertEquals(1, botContext.getNamedNeedUriList("uri1").size());
    Assert.assertEquals(URI1, botContext.getNamedNeedUriList("uri1").get(0));

    botContext.appendToNamedNeedUriList(URI2, "uri2");
    botContext.appendToNamedNeedUriList(URI3, "uri1");

    Assert.assertEquals(3, botContext.retrieveAllNeedUris().size());
    Assert.assertEquals(2, botContext.getNamedNeedUriList("uri1").size());
    Assert.assertTrue(botContext.getNamedNeedUriList("uri1").contains(URI1));
    Assert.assertTrue(botContext.getNamedNeedUriList("uri2").contains(URI2));
    Assert.assertTrue(botContext.getNamedNeedUriList("uri1").contains(URI3));
    Assert.assertFalse(botContext.getNamedNeedUriList("uri2").contains(URI1));
    Assert.assertFalse(botContext.getNamedNeedUriList("uri1").contains(URI2));
    Assert.assertFalse(botContext.getNamedNeedUriList("uri2").contains(URI3));
    Assert.assertTrue(botContext.isNeedKnown(URI1));
    Assert.assertTrue(botContext.isNeedKnown(URI2));
    Assert.assertTrue(botContext.isNeedKnown(URI3));

    botContext.appendToNamedNeedUriList(URI1, "uri2");

    Assert.assertEquals(3, botContext.retrieveAllNeedUris().size());
    Assert.assertEquals(2, botContext.getNamedNeedUriList("uri1").size());
    Assert.assertEquals(2, botContext.getNamedNeedUriList("uri2").size());
    Assert.assertTrue(botContext.getNamedNeedUriList("uri1").contains(URI1));

    botContext.removeNeedUriFromNamedNeedUriList(URI1, "uri2"); // removes URI1 from uri1 but not from uri2
    botContext.removeNeedUriFromNamedNeedUriList(URI2, "uri1");
    botContext.removeNeedUriFromNamedNeedUriList(URI3, "uri1");

    Assert.assertEquals(2, botContext.retrieveAllNeedUris().size()); // URI1 and URI2 should still be there in the general list
    Assert.assertEquals(1, botContext.getNamedNeedUriList("uri1").size());
    Assert.assertTrue(botContext.getNamedNeedUriList("uri1").contains(URI1));
    Assert.assertEquals(1, botContext.getNamedNeedUriList("uri2").size());
    Assert.assertTrue(botContext.getNamedNeedUriList("uri2").contains(URI2));

    botContext.removeNeedUriFromNamedNeedUriList(URI1, "uri1");
    botContext.removeNeedUriFromNamedNeedUriList(URI2, "uri2");
    Assert.assertNotNull(botContext.getNamedNeedUriList("uri1"));
    Assert.assertEquals(0, botContext.getNamedNeedUriList("uri1").size());
    Assert.assertNotNull(botContext.retrieveAllNeedUris());
    Assert.assertEquals(0, botContext.retrieveAllNeedUris().size());
  }

  @Test
  public void testNodeUriMethods() {

    Assert.assertFalse(botContext.isNodeKnown(URI1));

    botContext.rememberNodeUri(URI1);

    Assert.assertTrue(botContext.isNodeKnown(URI1));

    botContext.rememberNodeUri(URI1);
    botContext.rememberNodeUri(URI2);

    Assert.assertTrue(botContext.isNodeKnown(URI1));
    Assert.assertTrue(botContext.isNodeKnown(URI2));

    botContext.removeNodeUri(URI1);

    Assert.assertFalse(botContext.isNodeKnown(URI1));
    Assert.assertTrue(botContext.isNodeKnown(URI2));

    botContext.rememberNodeUri(URI1);

    Assert.assertTrue(botContext.isNodeKnown(URI1));
    Assert.assertTrue(botContext.isNodeKnown(URI2));

    botContext.removeNodeUri(URI1);
    botContext.removeNodeUri(URI2);

    Assert.assertFalse(botContext.isNodeKnown(URI1));
    Assert.assertFalse(botContext.isNodeKnown(URI2));
  }

  @Test
  public void testGenericMethods() {

    Assert.assertNotNull(botContext.genericValues("col1"));
    Assert.assertEquals(0, botContext.genericValues("col1").size());

    botContext.putGeneric("col1", "uri1", URI1);
    botContext.putGeneric("col1", "uri1", URI1);
    botContext.putGeneric("col1", "uri2", URI1);
    botContext.putGeneric("col1", "uri2", URI2);  // overwrite
    botContext.putGeneric("col2", "uri1", URI2);
    botContext.putGeneric("col2", "uri2", URI2);
    botContext.putGeneric("col2", "uri3", URI3);

    Assert.assertEquals(2, botContext.genericValues("col1").size());
    Assert.assertEquals(URI1, botContext.getGeneric("col1", "uri1"));
    Assert.assertEquals(URI2, botContext.getGeneric("col1", "uri2"));
    Assert.assertEquals(2, botContext.genericValues("col2").size());
    Assert.assertTrue(botContext.genericValues("col2").contains(URI2));
    Assert.assertTrue(botContext.genericValues("col2").contains(URI3));

    botContext.removeGeneric("col3", "uri1");
    botContext.removeGeneric("col2", "uri3");
    botContext.removeGeneric("col1", "uri1");
    botContext.removeGeneric("col1", "uri2");

    Assert.assertNull(botContext.getGeneric("col1", "uri1"));
    Assert.assertEquals(0, botContext.genericValues("col1").size());
    Assert.assertEquals(1, botContext.genericValues("col2").size());
  }

  @Test
  public void testPutAndRetrieveGenericObjects() throws MessagingException, IOException, ClassNotFoundException {

    // List
    LinkedList<URI> uriList = new LinkedList<>();
    uriList.add(URI1);
    uriList.add(URI1);
    uriList.add(URI2);
    uriList.add(URI3);
    botContext.putGeneric("uriList", "list1", uriList);
    List<URI> uriListCopy = (List<URI>) botContext.getGeneric("uriList", "list1");
    Assert.assertEquals(uriList, uriListCopy);

    // HashMap needs to be serialized (here only non-complex keys are allowed in maps)
    HashMap<String, URI> uriHashMap = new HashMap<>();
    uriHashMap.put(URI1.toString(), URI1);
    uriHashMap.put(URI2.toString(), URI1);
    uriHashMap.put(URI3.toString(), URI3);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(os);
    oos.writeObject(uriHashMap);
    botContext.putGeneric("uriMap", "map1", os.toByteArray());
    byte[] byteMsg  = (byte[]) botContext.getGeneric("uriMap", "map1");
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
    botContext.putGeneric("uriMap", "map1", uriTreeMap);  // overwrite the HashMap entry from the previous step
    Map<String, URI> uriTreeMapCopy  = (Map<String, URI>) botContext.getGeneric("uriMap", "map1");
    Assert.assertEquals(uriTreeMap, uriTreeMapCopy);

    // MimeMessage cannot be serialized directly => has to be serialized manually first
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
    botContext.putGeneric("mime", "mime1", os.toByteArray());
    byteMsg = (byte[]) botContext.getGeneric("mime", "mime1");
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
