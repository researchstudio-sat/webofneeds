package won.utils.im.port;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * User: ypanchenko
 * Date: 04.09.2014
 */
public class NeedDataImporterTest
{

  public static String MAILS_FOLDER = "/test-mails";
  //@Rule
  //public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void importMailsTest() throws Exception {

    File input = new File(this.getClass().getResource(MAILS_FOLDER).getFile());

    //File output = testFolder.newFolder("mails-tests-out");
    File output = File.createTempFile("mails", "out");
    output.delete();
    output.mkdir();
    System.out.println("Check output in: " + output.getAbsolutePath());
    try (
      NeedDataReader reader = new NeedDataMailsReader(input.getAbsolutePath());
      NeedDataWriter writer = new ModelsRdfFolderWriter(output.getAbsolutePath())
    ) {
      NeedDataImporter.process(reader, writer);
    }
  }

  @Test
  public void importDummyNeedsTest() throws Exception {

    int numberOfDummyNeeds = 4;
    //File output = testFolder.newFolder("mails-tests-out");
    File output = File.createTempFile("dummy-needs", "out");
    output.delete();
    output.mkdir();
    System.out.println("Check output in: " + output.getAbsolutePath());
    try (
      NeedDataReader reader = new DummyNeedsReader(numberOfDummyNeeds);
      NeedDataWriter writer = new ModelsRdfFolderWriter(output.getAbsolutePath())
    ) {
      NeedDataImporter.process(reader, writer);
    }
    Assert.assertTrue(output.listFiles().length == 4);
  }

}
