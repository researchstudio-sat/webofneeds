package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * User: ypanchenko
 * Date: 02.06.2015
 */
public class WonMessageValidator
{


  private static final String resourceDir = "validation/";
  private Map<String,List<WonSparqlValidator>> dirToValidator = new LinkedHashMap<>();


  public WonMessageValidator() {
    Map validatorDirs = new HashMap<>();
    String[] dirs = {
      resourceDir + "01_basic/",
      resourceDir + "02_prop/",
      resourceDir + "03_chain/",
      resourceDir + "04_uri/",
      resourceDir + "05_sign/"
    };
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    for (String dir : dirs) {
      try {
        List validators = loadResources(resolver, dir);
        validatorDirs.put(dir, Collections.unmodifiableList(validators));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    this.dirToValidator = Collections.unmodifiableMap(validatorDirs);
  }

  private List<WonSparqlValidator> loadResources(PathMatchingResourcePatternResolver resolver, String dirString)
    throws IOException {
    List validators = new ArrayList<WonSparqlValidator>();
    Resource[] resources = resolver.getResources("classpath:" + dirString + "*.rq");
    for (Resource resource : resources) {
      String queryString = loadQueryFromResource(resource);
      Query constraint = QueryFactory.create(queryString);
      WonSparqlValidator validator = new WonSparqlValidator(constraint, resource.getFilename());
      validators.add(validator);
    }
    return validators;
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
        if (!validator.validate(input).isValid()) {
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
        WonSparqlValidator.ValidationResult result = validator.validate(input);
        if (!result.isValid()) {
          causePlaceholder.append(dir);
          causePlaceholder.append(validator.getName());
          causePlaceholder.append(": ").append(result.getErrorMessage());
          return false;
        }
      }
    }

    return true;
  }


}
