/**
 * builds the 0th node, the content-node, in the create message graph
 *
 * Created by ksinger on 29.06.2015.
 */
//TODO switch to requirejs for dependency mngmt (so this lib isn't angular-bound)
//TODO replace calls to `won` object to `require('util')`
import won from "./won.js";
import { getIn } from "../utils.js";

(function() {
  // <need-builder-js> scope

  function hasPriceSpecification() {
    return false; //TODO price-specification not fully implemented yet
  }
  function hasTimeConstraint() {
    return false; //TODO time-constraint-attachment not implemented yet
  }
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
   * * args.type: the type of the need (e.g. 'won:Supply')
   * * args.title: a string with the title (e.g. 'Couch to give away')
   * * args.description: a longer string describing the need in detail
   *
   * **optional parameters:**
   *    ```
   *    //comma-separated
   *    args.tags = 'Couch, furniture';
   *
   *    // this is the default
   *    args.facet = 'won:OwnerFacet';
   *    // the options here are:
   *    // * won.WON.OwnerFacetCompacted
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
    if (!args.is && !args.seeks) {
      throw new Error(
        "Expected an object with an is- and/or a seeks-subobject. Something like `{ is: {...}, seeks: {...} }`. Got " +
          JSON.stringify(args)
      );
    }

    /*
        const putIntoBoth =
            args.type === won.WON.BasicNeedTypeDotogetherCompacted;

        const putIntoIs =
            putIntoBoth ||
            args.type === won.WON.BasicNeedTypeSupplyCompacted;

        const putIntoSeeks =
            putIntoBoth ||
            args.type === won.WON.BasicNeedTypeDemandCompacted;

        let hasFlag = [];

        if(!!won.debugmode) {
            hasFlag.push("won:UsedForTesting");
        }

        if(!!args.whatsAround){
            hasFlag.push("won:WhatsAround");
            hasFlag.push("won:NoHintForCounterpart");
        }

        if(!!args.noHints){
            hasFlag.push("won:NoHintForMe");
            hasFlag.push("won:NoHintForCounterpart");
        }
        *

        //remove possible duplicates in hasFlag
        const result = [];
        hasFlag.forEach(function(item) {
            if(result.indexOf(item) < 0) {
                result.push(item);
            }
        });
        hasFlag = result;
        */

    const buildContentNode = (id, isOrSeeksData, isSeeks) => ({
      "@id": id,
      "dc:title": isOrSeeksData.title,
      "dc:description": isOrSeeksData.description,
      "won:hasTag": isOrSeeksData.tags,
      "won:hasAttachment": hasAttachmentUrls(isOrSeeksData)
        ? isOrSeeksData.attachmentUris.map(uri => ({ "@id": uri }))
        : undefined,
      "won:hasPerson": isOrSeeksData.person,

      "foaf:title": getIn(isOrSeeksData, ["person", "title"])
        ? getIn(isOrSeeksData, ["person", "title"])
        : undefined,
      "foaf:name": getIn(isOrSeeksData, ["person", "name"])
        ? getIn(isOrSeeksData, ["person", "name"])
        : undefined,
      "s:worksFor": getIn(isOrSeeksData, ["person", "company"])
        ? {
            "@type": "s:Organization",
            "s:name": getIn(isOrSeeksData, ["person", "company"]),
          }
        : undefined,
      "s:jobTitle": getIn(isOrSeeksData, ["person", "position"])
        ? getIn(isOrSeeksData, ["person", "position"])
        : undefined,
      "s:knowsAbout": getIn(isOrSeeksData, ["person", "skills"])
        ? getIn(isOrSeeksData, ["person", "skills"]).toJS()
        : undefined,
      // "dc:description": getIn(isOrSeeksData, ["person", "bio"])
      //   ? getIn(isOrSeeksData, ["person", "bio"])
      //   : undefined,
      "won:hasLocation": !isOrSeeksData.location
        ? undefined
        : {
            "@type": "s:Place",
            "s:geo": {
              "@id": isSeeks ? "_:isLocation" : "_:seeksLocation",
              "@type": "s:GeoCoordinates",
              "s:latitude": isOrSeeksData.location.lat.toFixed(6),
              "s:longitude": isOrSeeksData.location.lng.toFixed(6),
            },
            "s:name": isOrSeeksData.location.name,
            "won:hasBoundingBox":
              !isOrSeeksData.location.nwCorner ||
              !isOrSeeksData.location.seCorner
                ? undefined
                : {
                    "won:hasNorthWestCorner": {
                      "@id": isSeeks ? "_:isBoundsNW" : "_:seeksBoundsNW",
                      "@type": "s:GeoCoordinates",
                      "s:latitude": isOrSeeksData.location.nwCorner.lat.toFixed(
                        6
                      ),
                      "s:longitude": isOrSeeksData.location.nwCorner.lng.toFixed(
                        6
                      ),
                    },
                    "won:hasSouthEastCorner": {
                      "@id": isSeeks ? "_:isBoundsSE" : "_:seeksBoundsSE",
                      "@type": "s:GeoCoordinates",
                      "s:latitude": isOrSeeksData.location.seCorner.lat.toFixed(
                        6
                      ),
                      "s:longitude": isOrSeeksData.location.seCorner.lng.toFixed(
                        6
                      ),
                    },
                  },
          },
      "won:travelAction": !isOrSeeksData.travelAction
        ? undefined
        : {
            "@type": "s:TravelAction",
            "s:fromLocation": !isOrSeeksData.travelAction.fromLocation
              ? undefined
              : {
                  "@type": "s:Place",
                  "s:geo": {
                    "@type": "s:GeoCoordinates",
                    "s:latitude": isOrSeeksData.travelAction.fromLocation.lat.toFixed(
                      6
                    ),
                    "s:longitude": isOrSeeksData.travelAction.fromLocation.lng.toFixed(
                      6
                    ),
                  },
                  "s:name": isOrSeeksData.travelAction.fromLocation.name,
                },
            "s:toLocation": !isOrSeeksData.travelAction.toLocation
              ? undefined
              : {
                  "@type": "s:Place",
                  "s:geo": {
                    "@type": "s:GeoCoordinates",
                    "s:latitude": isOrSeeksData.travelAction.toLocation.lat.toFixed(
                      6
                    ),
                    "s:longitude": isOrSeeksData.travelAction.toLocation.lng.toFixed(
                      6
                    ),
                  },
                  "s:name": isOrSeeksData.travelAction.toLocation.name,
                },
          },
      //TODO: Different id for is and seeks
      "won:hasTimeSpecification": !hasTimeConstraint(isOrSeeksData)
        ? undefined
        : {
            "@id": "_:timeSpecification",
            "@type": "won:TimeSpecification",
            "won:hasRecurInfiniteTimes": isOrSeeksData.recurInfinite,
            "won:hasRecursIn": isOrSeeksData.recursIn,
            "won:hasStartTime": isOrSeeksData.startTime,
            "won:hasEndTime": isOrSeeksData.endTime,
          },

      "won:hasPriceSpecificationhas": !hasPriceSpecification(isOrSeeksData)
        ? undefined
        : {
            "@id": "_:priceSpecification",
            "@type": "won:PriceSpecification",
            "won:hasCurrency": isOrSeeksData.currency,
            "won:hasLowerPriceLimit": isOrSeeksData.lowerPriceLimit,
            "won:hasUpperPriceLimit": isOrSeeksData.upperPriceLimit,
          },
      //TODO images, time, currency(?)
    });

    // TODO: if both is and seeks are present, the seeks content gets ignored here
    const isWhatsAround = args.is
      ? args.is.whatsAround
      : args.seeks.whatsAround;
    const isWhatsNew = args.is ? args.is.whatsNew : args.seeks.whatsNew;
    const noHints = args.is ? args.is.noHints : args.seeks.noHints;
    const matchingContext = args.matchingContext;
    const searchString = args.searchString;
    const useCase = args.useCase;
    const noHintForCounterpart = isWhatsAround
      ? true
      : isWhatsNew
        ? true
        : args.searchString
          ? true
          : false;
    let isContentUri, seeksContentUri;
    if (isWhatsAround) {
      isContentUri = won.WON.contentNodeBlankUri.whatsAround;
      seeksContentUri = won.WON.contentNodeBlankUri.whatsAround;
    } else if (isWhatsNew) {
      isContentUri = won.WON.contentNodeBlankUri.whatsNew;
      seeksContentUri = won.WON.contentNodeBlankUri.whatsNew;
    } else {
      isContentUri = args.is ? won.WON.contentNodeBlankUri.is : undefined;
      seeksContentUri = args.seeks
        ? won.WON.contentNodeBlankUri.seeks
        : undefined;
    }
    const graph = [
      {
        "@id": args.is
          ? args.is.publishedContentUri
          : args.seeks.publishedContentUri,
        "@type": "won:Need",
        "won:is": isContentUri ? { "@id": isContentUri } : undefined,
        "won:seeks": seeksContentUri ? { "@id": seeksContentUri } : undefined,
        "won:hasFacet": args.facet ? args.facet : "won:OwnerFacet",
        "won:hasFlag": new Set([
          won.debugmode ? "won:UsedForTesting" : undefined,

          isWhatsAround ? "won:WhatsAround" : undefined,
          isWhatsNew ? "won:WhatsNew" : undefined,
          noHintForCounterpart ? "won:NoHintForCounterpart" : undefined,

          noHints ? "won:NoHintForMe" : undefined,
          noHints ? "won:NoHintForCounterpart" : undefined,
        ]), ///.toArray().filter(f => f),
        "won:hasMatchingContext": matchingContext ? matchingContext : undefined,
        "won:hasSearchString": searchString ? searchString : undefined,
        "won:hasUseCase": useCase ? useCase : undefined,
      },
      //, <if _hasModalities> {... (see directly below) } </if>
      args.is ? buildContentNode(isContentUri, args.is, true) : {},
      args.seeks && !isWhatsAround
        ? buildContentNode(seeksContentUri, args.seeks, false)
        : {},
      ...(args.is && args.is.arbitraryJsonLd ? args.is.arbitraryJsonLd : []),
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

        "won:hasStartTime": "xsd:dateTime",
        "won:hasEndTime": "xsd:dateTime",

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
