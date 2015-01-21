package won.matcher.webapp;

import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import pojo.SearchResultPojo;
import won.matcher.service.SearchResult;
import won.matcher.service.SearchResultModelMapper;
import won.matcher.service.SearchService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: moru
 * Date: 12/09/13
 * Time: 12:50
 * To change this template use File | Settings | File Templates.
 */
@Controller
@RequestMapping("/")
public class SearchController
{
  @Autowired
  private SearchService searchService;

  private SearchResultModelMapper searchResultModelMapper = new SearchResultModelMapper();
  private static final String DEFAULT_NUM_RESULTS = "10";

  /**
   * takes keywords and number of results to be returned and performs the search.
   * @param keywords keywords to search
   * @param numResults number of results to be returned
   * @return RDF model of search results
   */
  @RequestMapping(value="search",
      method = RequestMethod.GET,
      produces={"text/html","application/xhtml+xml","application/xml","application/rdf+xml","application/x-turtle",
                "text/turtle","text/rdf+n3", "application/ld+json"})
  @ResponseBody
  public Model search(
      @RequestParam(value="q", required = true) final String keywords,
      @RequestParam(value="n", required = false, defaultValue = DEFAULT_NUM_RESULTS) final int numResults)
  {

    return searchResultModelMapper.toModel(searchService.search(keywords, numResults));
  }

  /**
   * takes keywords and number of results to be returned and performs the search.
   * @param keywords keywords to search
   * @param numResults number of results to be returned
   * @return RDF model of search results
   */
  @RequestMapping(value="searchJson",
    method = RequestMethod.GET,
    produces={"application/ld+json"})
  @ResponseBody
  public List<SearchResultPojo> searchJson(
    @RequestParam(value="q", required = true) final String keywords,
    @RequestParam(value="n", required = false, defaultValue = DEFAULT_NUM_RESULTS) final int numResults)
  {
    List<SearchResultPojo> searchResultPojos = new ArrayList<>();

    SearchResult searchResult = searchService.search(keywords,numResults);

    for (int i = 0; i<searchResult.getItems().size();i++){
      SearchResultPojo searchResultPojo = new SearchResultPojo(searchResult.getItems().get(i).getUri().toString(),searchResult
        .getItems().get(i).getScore());
      searchResultPojos.add(searchResultPojo);
    }
    return searchResultPojos;
  }

  /**
   *
   * @param keywords keywords to search
   * @param needModel
   * @param numResults number of results to be returned
   * @return
   */
  @RequestMapping(
      value="search",
      method = RequestMethod.POST,
      produces={"application/rdf+xml","application/x-turtle","text/turtle","text/rdf+n3","application/ld+json"})
  @ResponseBody
  public Model search(
      @RequestParam(value="q", required=false) final String keywords,
      @RequestParam(value="model", required = false) final Model needModel,
      @RequestParam(value="n", required = false, defaultValue = DEFAULT_NUM_RESULTS) final int numResults)
  {
    return searchResultModelMapper.toModel(searchService.search(keywords, needModel, numResults));
  }


  public void notifyOfNewNeed()
  {
    //TODO implement
  }

  public void setSearchService(final SearchService searchService)
  {
    this.searchService = searchService;
  }
}
