/**
 * builds the 0th node, the content-node, in the create message graph
 *
 * Created by ksinger on 29.06.2015.
 */
//TODO switch to requirejs for dependency mngmt (so this lib isn't angular-bound)
//TODO replace calls to `won` object to `require('util')`
import won from "./won.js";
// import { useCases } from "useCaseDefinitions";
import { getAllDetails } from "../won-utils";
import { is } from "../utils";
import { useCases } from "../../config/usecase-definitions.js";
import {
  generateWhatsAroundQuery,
  generateWhatsNewQuery,
} from "../sparql-builder-utils.js";

import { Generator } from "sparqljs";

(function() {
  // <need-builder-js> scope

  function hasAttachmentUrls(args) {
    return (
      args.attachmentUris &&
      Array.isArray(args.attachmentUris) &&
      args.attachmentUris.length > 0
    );
  }

  /**
   * Usage:  `won.buildNeedRdf(args)`
   * Where args consists of:
   *
   * **mandatory parameters:**
   *
   * * args.title: a string with the title (e.g. 'Couch to give away')
   * * args.description: a longer string describing the need in detail
   *
   * **optional parameters:**
   *    ```
   *    //comma-separated
   *    args.tags = 'Couch, furniture';
   *
   *    args.latitude = 12.345678;
   *    args.longitude = 12.345678;
   *
   *    //no format assumed. this will just be attached as a string
   *    args.address = 'Yellowbrick Rd. 7, 12345 Oz';
   *
   *    // the URIs where attachments can be found / will be published
   *    args.attachmentUris = ['http://example.org/.../1234.png', 'http://example.org/.../1234.pdf']
   *
   *    // the triples will be put in the graph that's build here anyway, so there's no need to pass graph-uri
   *    args.arbitraryJsonLdGraph = [{ '@id': 'http://example.org/.../1234', 'dc:title': 'hi}, {...}, {...}]
   *
   *    args.recurInfinite =
   *    args.recursIn =
   *    args.startTime =
   *    args.endTime =
   *
   *    args.currency =
   *    args.lowerLimit =
   *    args.upperLimit =
   *    ```
   * @returns {{@id: string, @graph: {...}*}}
   *
   * NOTE: the function below makes heavy use of the fact that `undefined`
   * properties won't be included for stringification. This allows to
   * keep it readable (the alternative is a myriad of ternary statements or
   * splitting the graph-building in as many sub-functions)
   *
   *
   * //TODO mandatory but specify later:
   * contentId, e.g. 'http://localhost:8080/won/resource/event/1997814854983652400#content-need';
   */
  won.buildNeedRdf = function(args) {
    if (!args.content && !args.seeks) {
      throw new Error(
        "Expected an object with an is- and/or a seeks-subobject. Something like `{ is: {...}, seeks: {...} }`. Got " +
          JSON.stringify(args)
      );
    }

    const addContent = (contentNode, contentData) => {
      //TODO: CANT HANDLE "@id" details yet (see won-message-utils.js buildChatMessage(..) additionalContent part
      const detailList = getAllDetails();

      for (const detail of Object.values(detailList)) {
        // const detail = detailList[detailName];
        const detailRDF = {
          ...detail.parseToRDF({
            value: contentData[detail.identifier],
            identifier: detail.identifier,
            contentUri: contentData["publishedContentUri"],
          }),
        };

        // add to content node
        for (const key of Object.keys(detailRDF)) {
          //if contentNode[key] and detailRDF[key] both have values we ommit adding new content (until we implement a merge function)
          if (contentNode[key]) {
            if (!Array.isArray(contentNode[key]))
              contentNode[key] = Array.of(contentNode[key]);

            contentNode[key] = contentNode[key].concat(detailRDF[key]);
          } else {
            contentNode[key] = detailRDF[key];
          }
        }
      }
      if (contentNode["@type"]) {
        if (contentData["@type"]) {
          contentNode["@type"] = contentNode["@type"].concat(
            contentData["@type"]
          );
        } else if (contentData.type) {
          contentNode["@type"] = contentNode["@type"].concat(contentData.type);
        }
      } else {
        if (contentData["@type"]) {
          contentNode["@type"] = contentData["@type"];
        } else if (contentData.type) {
          contentNode["@type"] = contentData.type;
        }
      }

      return contentNode;
    };

    const buildSeeksContentNode = (id, seeksData) => {
      let contentNode = {
        "@id": id,
        "won:hasAttachment": hasAttachmentUrls(seeksData)
          ? seeksData.attachmentUris.map(uri => ({ "@id": uri }))
          : undefined,
      };

      return addContent(contentNode, seeksData);
    };

    const matchingContext = args.matchingContext;

    let seeksContentUri = args.seeks && won.WON.contentNodeBlankUri.seeks;

    const useCase = useCases[args.useCase];

    /*TODO: instead of the detection if whatsX to generate the query we could just make useCases out of the whatsX instead
      or define the query as a detail
    */
    const flags = args.content && args.content.flags;
    const isWhatsAroundDraft =
      flags &&
      ((is("Array", flags) && flags.indexOf("won:WhatsAround") != -1) ||
        flags === "won:WhatsAround");
    const isWhatsNewDraft =
      flags &&
      ((is("Array", flags) && flags.indexOf("won:WhatsNew") != -1) ||
        flags === "won:WhatsNew");

    let queryString = undefined;
    if (isWhatsAroundDraft) {
      const location = args.seeks.location;

      if (location && location.lat && location.lng) {
        queryString = generateWhatsAroundQuery(location.lat, location.lng);
      }
    } else if (isWhatsNewDraft) {
      queryString = generateWhatsNewQuery();
    } else if (useCase && useCase.generateQuery) {
      const queryMask = {
        type: "query",
        queryType: "SELECT",
        variables: ["?result", "?score"],
      };

      let useCaseQuery = {
        ...useCase.generateQuery(args, "?result"),
        ...queryMask,
      };

      if (useCaseQuery) {
        const sparqlGenerator = new Generator();
        queryString = useCaseQuery && sparqlGenerator.stringify(useCaseQuery);
      }
    }

    const seeksContentNode = seeksContentUri
      ? buildSeeksContentNode(seeksContentUri, args.seeks)
      : {};

    const doNotMatchAfterFnOrLit = useCase && useCase.doNotMatchAfter;
    const doNotMatchAfter = is("Function", doNotMatchAfterFnOrLit)
      ? doNotMatchAfterFnOrLit(
          args,
          {
            "@graph": [seeksContentNode],
          },
          useCase && useCase.timeToLiveMillisDefault
            ? useCase.timeToLiveMillisDefault
            : 30 * 60 * 1000,
          useCase && useCase.timeToLiveMillisAfterDate
            ? useCase.timeToLiveMillisAfterDate
            : 30 * 60 * 1000
        ) // TODO pass draft and jsonld
      : doNotMatchAfterFnOrLit;

    let contentGraph = {
      "@id": args.content
        ? args.content.publishedContentUri
        : args.seeks
          ? args.seeks.publishedContentUri
          : undefined,
      "@type": ["won:Need"],
      "won:seeks": seeksContentUri ? { "@id": seeksContentUri } : undefined,
      "won:hasFacet": !(args.content && args.content.facets)
        ? [
            { "@id": "#chatFacet", "@type": "won:ChatFacet" },
            { "@id": "#holdableFacet", "@type": "won:HoldableFacet" },
          ]
        : undefined,
      "won:hasDefaultFacet": !(args.content && args.content.defaultFacet)
        ? [{ "@id": "#chatFacet", "@type": "won:ChatFacet" }]
        : undefined,
      "won:hasFlag": won.debugmode
        ? [{ "@id": "won:UsedForTesting" }]
        : undefined, //TODO: refactor this and use a won:hasFlags-Detail in the content instead
      "won:doNotMatchAfter": doNotMatchAfter
        ? { "@value": doNotMatchAfter, "@type": "xsd:dateTime" }
        : undefined,
      "won:hasMatchingContext": matchingContext ? matchingContext : undefined,
      "won:hasQuery": queryString,
    };

    if (args.content) {
      contentGraph = addContent(contentGraph, args.content);
    }
    const graph = [
      contentGraph,
      //, <if _hasModalities> {... (see directly below) } </if>
      seeksContentNode,
      ...(args.content && args.content.arbitraryJsonLd
        ? args.content.arbitraryJsonLd
        : []),
      ...(args.seeks && args.seeks.arbitraryJsonLd
        ? args.seeks.arbitraryJsonLd
        : []),
    ];

    return {
      "@graph": graph,
      "@context": {
        ...won.defaultContext, // needed for the arbitrary rdf
        //TODO probably an alias instead of an type declaration as it's intended here
        "won:hasCurrency": "xsd:string",
        "won:hasLowerPriceLimit": "xsd:float",
        "won:hasUpperPriceLimit": "xsd:float",
      },
    };
  };
})(); // </need-builder-js>
