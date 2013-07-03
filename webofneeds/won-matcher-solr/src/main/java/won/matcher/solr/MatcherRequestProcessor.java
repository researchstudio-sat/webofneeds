package won.matcher.solr;

import org.apache.solr.update.*;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * User: atus
 * Date: 03.07.13
 */
public class MatcherRequestProcessor extends UpdateRequestProcessor
{

  private Logger logger;

  public MatcherRequestProcessor(UpdateRequestProcessor next)
  {
    super(next);

    logger = LoggerFactory.getLogger(getClass().getName());
    logger.info("MATCHER INITIALIZED!");
  }

  @Override
  public void processAdd(final AddUpdateCommand cmd) throws IOException
  {
    super.processAdd(cmd);
    logger.info("MATCHER ADD");

  }

  @Override
  public void processDelete(final DeleteUpdateCommand cmd) throws IOException
  {
    super.processDelete(cmd);
    logger.info("MATCHER DELETE");
  }

  @Override
  public void processMergeIndexes(final MergeIndexesCommand cmd) throws IOException
  {
    super.processMergeIndexes(cmd);
    logger.info("MATCHER MERGE");
  }

  @Override
  public void processCommit(final CommitUpdateCommand cmd) throws IOException
  {
    super.processCommit(cmd);
    logger.info("MATCHER COMMIT");
  }

  @Override
  public void processRollback(final RollbackUpdateCommand cmd) throws IOException
  {
    super.processRollback(cmd);
    logger.info("MATCHER ROLLBACK");
  }

  @Override
  public void finish() throws IOException
  {
    super.finish();
    logger.info("MATCHER FINISH");
  }
}
