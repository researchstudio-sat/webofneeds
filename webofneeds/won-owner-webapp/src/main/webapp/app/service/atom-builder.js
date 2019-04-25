/**
 * builds the 0th node, the content-node, in the create message graph
 *
 * Created by ksinger on 29.06.2015.
 */
//TODO switch to requirejs for dependency mngmt (so this lib isn't angular-bound)
//TODO replace calls to `won` object to `require('util')`
import won from "./won.js";
import * as useCaseUtils from "../usecase-utils";
import { is } from "../utils";

import { Generator } from "sparqljs";

(function() {
  // <atom-builder-js> scope

  function hasAttachmentUrls(args) {
    return (
      args.attachmentUris &&
      Array.isArray(args.attachmentUris) &&
      args.attachmentUris.length > 0
    );
  }

  /**
   * Usage:  `won.buildAtomRdf(args)`
   * Where args consists of:
   *
   * **mandatory parameters:**
   *
   * * args.title: a string with the title (e.g. 'Couch to give away')
   * * args.description: a longer string describing the atom in detail
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
   * contentId, e.g. 'http://localhost:8080/won/resource/event/1997814854983652400#content-atom';
   */
  won.buildAtomRdf = function(args) {
    if (!args.content && !args.seeks) {
      throw new Error(
        "Expected an object with an is- and/or a seeks-subobject. Something like `{ is: {...}, seeks: {...} }`. Got " +
          JSON.stringify(args)
      );
    }

    const addContent = (contentNode, contentData) => {
      //TODO: CANT HANDLE "@id" details yet (see won-message-utils.js buildChatMessage(..) additionalContent part
      const detailList = useCaseUtils.getAllDetails();

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

    let seeksContentUri = args.seeks && won.WON.contentNodeBlankUri.seeks;

    const useCase = useCaseUtils.getUseCase(args.useCase);

    let queryString = undefined;
    if (useCase && useCase.generateQuery) {
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
      "@type": ["won:Atom"],
      "won:seeks": seeksContentUri ? { "@id": seeksContentUri } : undefined,
      "won:socket": !(args.content && args.content.sockets)
        ? [
            { "@id": "#chatSocket", "@type": "won:ChatSocket" },
            { "@id": "#holdableSocket", "@type": "hold:HoldableSocket" },
          ]
        : undefined,
      "won:defaultSocket": !(args.content && args.content.defaultSocket)
        ? [{ "@id": "#chatSocket", "@type": "won:ChatSocket" }]
        : undefined,
      "won:flag": won.debugmode ? [{ "@id": "won:UsedForTesting" }] : undefined, //TODO: refactor this and use a won:flags-Detail in the content instead
      "won:doNotMatchAfter": doNotMatchAfter
        ? { "@value": doNotMatchAfter, "@type": "xsd:dateTime" }
        : undefined,
      "won:sparqlQuery": queryString,
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
        "won:currency": "xsd:string",
        "won:lowerPriceLimit": "xsd:float",
        "won:upperPriceLimit": "xsd:float",
      },
    };
  };
})(); // </atom-builder-js>
