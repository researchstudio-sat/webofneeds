package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 02.06.2015
 */
public class WonMessageValidator
{


  private static final String resourceDir = "validation/";
  private Map<String,List<WonSparqlValidator>> dirToValidator = new LinkedHashMap<>();


  public WonMessageValidator() {

    dirToValidator.put(resourceDir + "01_basic/", new ArrayList<WonSparqlValidator>());
    dirToValidator.put(resourceDir + "02_prop/", new ArrayList<WonSparqlValidator>());
    dirToValidator.put(resourceDir + "03_chain/", new ArrayList<WonSparqlValidator>());
    dirToValidator.put(resourceDir + "04_uri/", new ArrayList<WonSparqlValidator>());
    dirToValidator.put(resourceDir + "05_sign/", new ArrayList<WonSparqlValidator>());
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    for (String dir : dirToValidator.keySet()) {
      try {
        loadResources(resolver, dir, dirToValidator.get(dir));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

  }

  private void loadResources(PathMatchingResourcePatternResolver resolver, String dirString,
                             List<WonSparqlValidator> loadTo) throws IOException {

    Resource[] resources = resolver.getResources("classpath:" + dirString + "*.rq");
    for (Resource resource : resources) {
      String queryString = loadQueryFromResource(resource);
      Query constraint = QueryFactory.create(queryString);
      WonSparqlValidator validator = new WonSparqlValidator(constraint, resource.getFilename());
      loadTo.add(validator);
    }
  }

  private String loadQueryFromResource(final Resource resource) {
    BufferedReader reader = null;
    StringBuilder sb = new StringBuilder();
    String line;
    try {
      reader = new BufferedReader(new InputStreamReader(resource.getInputStream(),
                                                        "UTF-8"));
      while ((line = reader.readLine()) != null) {
        sb.append(line);
        sb.append("\n");
      }

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return sb.toString();
  }


  public boolean validate(Dataset input) {

    for (String dir : dirToValidator.keySet()) {
      List<WonSparqlValidator> validators = dirToValidator.get(dir);
      for (WonSparqlValidator validator : validators) {
        if (!validator.validate(input)) {
          return false;
        }
      }
    }

    return true;
  }

  public boolean validate(Dataset input, StringBuilder causePlaceholder) {

    for (String dir : dirToValidator.keySet()) {
      List<WonSparqlValidator> validators = dirToValidator.get(dir);
      for (WonSparqlValidator validator : validators) {
        if (!validator.validate(input)) {
          causePlaceholder.append(dir);
          causePlaceholder.append(validator.getName());
          return false;
        }
      }
    }

    return true;
  }


}
