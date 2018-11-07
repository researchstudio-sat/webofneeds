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
   *    // this is the default
   *    args.facet = 'won:ChatFacet';
   *    // the options here are:
   *    // * won.WON.ChatFacetCompacted
   *    // * won.WON.GroupFacetCompacted
   *    // * won.WON.CoordinatorFacetCompacted
   *    // * won.WON.ParticipantFacetCompacted
   *    // * won.WON.CommentFacetCompacted
   *    // * 'won:CommentModeratedFacet'
   *    // * 'won:CommentUnrestrictedFacet'
   *    // * 'won:ControlFacet'
   *    // * 'won:BAPCCordinatorFacet'
   *    // * 'won:BAPCParticipantFacet'
   *    // * 'won:BACCCoordinatorFacet'
   *    // * 'won:BACCParticipantFacet'
   *    // * 'won:BAAtomicPCCoordinatorFacet'
   *    // * 'won:BAAtomicCCCoordinatorFacet'
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
    if (!args.content && !args.seeks && !args.searchString) {
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

    const buildContentNode = (id, seeksData) => {
      //TODO: CANT HANDLE "@id" details yet (see won-message-utils.js buildChatMessage(..) additionalContent part
      let contentNode = {
        "@id": id,
        "@type": seeksData.type || seeksData["@type"],
        "won:hasAttachment": hasAttachmentUrls(seeksData)
          ? seeksData.attachmentUris.map(uri => ({ "@id": uri }))
          : undefined,
      };

      const detailList = getAllDetails();

      for (const detail of Object.values(detailList)) {
        // const detail = detailList[detailName];
        const detailRDF = {
          ...detail.parseToRDF({
            value: seeksData[detail.identifier],
            identifier: detail.identifier,
            contentUri: seeksData["publishedContentUri"],
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
      return contentNode;
    };

    const matchingContext = args.matchingContext;
    const searchString = args.searchString;

    // TODO: if both is and seeks are present, the seeks content gets ignored here
    const isWhatsAround = args.content
      ? args.content.whatsAround
      : args.seeks
        ? args.seeks.whatsAround
        : undefined;
    const isWhatsNew = args.content
      ? args.content.whatsNew
      : args.seeks
        ? args.seeks.whatsNew
        : undefined;
    const noHints = args.content
      ? args.content.noHints
      : args.seeks
        ? args.seeks.noHints
        : undefined;
    const noHintForCounterpart = isWhatsAround
      ? true
      : isWhatsNew
        ? true
        : args.useCase === "search" //hack: no hint for counterpart if it's a pure search
          ? true
          : false;
    let seeksContentUri;
    if (isWhatsAround) {
      seeksContentUri = won.WON.contentNodeBlankUri.whatsAround;
    } else if (isWhatsNew) {
      seeksContentUri = won.WON.contentNodeBlankUri.whatsNew;
    } else {
      seeksContentUri = args.seeks
        ? won.WON.contentNodeBlankUri.seeks
        : undefined;
    }

    const useCase = useCases[args.useCase];

    const queryMask = {
      type: "query",
      queryType: "SELECT",
      variables: ["?result"],
    };

    let query = useCase &&
      useCase.generateQuery && {
        ...useCase.generateQuery(args, "?result"),
        ...queryMask,
      };

    if (isWhatsAround) {
      const location = args.seeks.location;

      if (location && location.lat && location.lng) {
        query = generateWhatsAroundQuery(location.lat, location.lng);
      }
    }

    if (isWhatsNew) {
      query = generateWhatsNewQuery();
    }

    const sparqlGenerator = new Generator();

    const seeksContentNode = args.seeks
      ? buildContentNode(seeksContentUri, args.seeks)
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
      "won:hasFacet": [
        args.facet
          ? args.facet
          : { "@id": "#chatFacet", "@type": "won:ChatFacet" },
        { "@id": "#holdableFacet", "@type": "won:HoldableFacet" },
      ],
      "won:hasDefaultFacet": args.facet
        ? args.facet
        : { "@id": "#chatFacet", "@type": "won:ChatFacet" },
      "won:hasFlag": new Set([
        won.debugmode ? "won:UsedForTesting" : undefined,

        isWhatsAround ? "won:WhatsAround" : undefined,
        isWhatsNew ? "won:WhatsNew" : undefined,
        noHintForCounterpart ? "won:NoHintForCounterpart" : undefined,

        noHints ? "won:NoHintForMe" : undefined,
        noHints ? "won:NoHintForCounterpart" : undefined,
      ]), ///.toArray().filter(f => f),
      "won:doNotMatchAfter": doNotMatchAfter
        ? { "@value": doNotMatchAfter, "@type": "xsd:dateTime" }
        : undefined,
      "won:hasMatchingContext": matchingContext ? matchingContext : undefined,
      "won:hasSearchString": searchString ? searchString : undefined,
      "won:hasQuery":
        isWhatsAround || isWhatsNew
          ? query
          : query
            ? sparqlGenerator.stringify(query)
            : undefined,
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

        //'geo:latitude': 'xsd:float',
        //'geo:longitude':'xsd:float',
        //'won:hasAddress': 'xsd:string',

        "won:hasFacet": {
          "@id": won.WON.hasFacet,
          "@type": "@id",
        },
        "won:hasFlag": {
          "@id": won.WON.hasFlag,
          "@type": "@id",
        },
      },
    };
  };
})(); // </need-builder-js>
