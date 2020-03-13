/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/**
 * Created by LEIH-NB on 19.08.2014.
 */
"format es6" /* required to force babel to transpile this so the minifier is happy */;
import { is } from "../utils.js";
import {
  clearDisclaimerAccepted,
  clearReadUris,
  isDisclaimerAccepted,
  setDisclaimerAccepted,
} from "../won-localstorage.js";
import vocab from "./vocab.js";
import jsonld from "jsonld/dist/jsonld.js";
import * as jsonldUtils from "./jsonld-utils.js";

import N3 from "n3";

window.N34dbg = N3;

let won = {};

/**
 *  Constants
 *
 */

won.debugmode = false; //if you set this to true, the created atoms will get flagged as debug atoms in order to get matches and requests from the debugbot

won.clearReadUris = clearReadUris;
won.isDisclaimerAccepted = isDisclaimerAccepted;
won.clearDisclaimerAccepted = clearDisclaimerAccepted;
won.setDisclaimerAccepted = setDisclaimerAccepted;

//Code definitions as enum in RestStatusResponse.java
won.RESPONSECODE = Object.freeze({
  USER_CREATED: 1200,
  USER_TRANSFERRED: 1201,
  USER_SIGNED_OUT: 1202,
  USER_ANONYMOUSLINK_SENT: 1203,
  USER_NOT_FOUND: 1400,
  USER_NOT_SIGNED_IN: 1401,
  USER_BAD_CREDENTIALS: 1402,
  USER_NOT_VERIFIED: 1403,
  USERNAME_MISMATCH: 1404,
  USER_ALREADY_EXISTS: 1405,
  TRANSFERUSER_NOT_FOUND: 2400,
  TRANSFERUSER_ALREADY_EXISTS: 2401,
  PASSWORDCHANGE_USER_NOT_FOUND: 8400,
  PASSWORDCHANGE_BAD_PASSWORD: 8401,
  PASSWORDCHANGE_KEYSTORE_PROBLEM: 8402,
  PASSWORDCHANGE_WRONG_OLD_PASSWORD: 8403,
  TOKEN_VERIFICATION_SUCCESS: 3200,
  TOKEN_RESEND_SUCCESS: 3201,
  TOKEN_NOT_FOUND: 3400,
  TOKEN_CREATION_FAILED: 3401,
  TOKEN_EXPIRED: 3403,
  TOKEN_RESEND_FAILED_ALREADY_VERIFIED: 3404,
  TOKEN_RESEND_FAILED_USER_ANONYMOUS: 3405,
  TOKEN_PURPOSE_MISMATCH: 3406,
  SIGNUP_FAILED: 4400,
  SETTINGS_CREATED: 5200,
  TOS_ACCEPT_SUCCESS: 6200,
  EXPORT_SUCCESS: 7200,
  EXPORT_NOT_VERIFIED: 7403,
  RECOVERY_KEYGEN_USER_NOT_FOUND: 8100,
  RECOVERY_KEYGEN_WRONG_PASSWORD: 8101,
  SUBSCRIBE_SUCCESS: 8200,

  PRIVATEID_NOT_FOUND: 666, //this one is not defined in RestStatusResponse.java
});

//we need to define this error here because we will not retrieve it from the rest endpoint
won.PRIVATEID_NOT_FOUND_ERROR = Object.freeze({
  code: 666,
  message: "invalid privateId",
});

won.clone = function(obj) {
  if (obj === undefined) return undefined;
  else return JSON.parse(JSON.stringify(obj));
};

/**
 * Copies all arguments properties recursively into a
 * new object and returns that.
 */
won.merge = function(/*args...*/) {
  const o = {};
  for (const argument of arguments) {
    won.mergeIntoLast(argument, o);
  }
  return o;
};
/*
 * Recursively merge properties of several objects
 * Copies all properties from the passed objects into the last one starting
 * from the left (thus the further right, the higher the priority in
 * case of name-clashes)
 * You might prefer this function over won.merge for performance reasons
 * (e.g. if you're copying into a very large object). Otherwise the former
 * is recommended.
 * @param args merges all passed objects onto the first passed
 */
won.mergeIntoLast = function(/*args...*/) {
  let obj1;
  for (const argument of arguments) {
    obj1 = arguments[arguments.length - 1];
    const obj2 = argument;
    for (const p in obj2) {
      obj1[p] = obj2[p];
    }
  }
  return obj1;
};

/**
 * Method that checks if the given element is already an array, if so return it, if not
 * return the element as a single element array, if element is undefined return undefined
 * @param elements
 * @returns {*}
 */
function createArray(elements) {
  return !elements || is("Array", elements) ? elements : [elements];
}

//get the URI from a jsonld resource (expects an object with an '@id' property)
//or the value from a typed literal
won.getSafeJsonLdValue = function(dataItem) {
  if (dataItem == null) return null;
  if (typeof dataItem === "object") {
    if (dataItem["@id"]) return dataItem["@id"];
    if (dataItem["@value"]) return dataItem["@value"];
  } else {
    return dataItem;
  }
  return null;
};

won.reportError = function(message) {
  if (arguments.length == 1) {
    return function(reason) {
      console.error(message, " reason: ", reason);
    };
  } else {
    return function(reason) {
      console.error("Error! reason: ", reason);
    };
  }
};

won.replaceRegExp = function(string) {
  return string.replace(/([.*+?^=!:${}()|[\]/\\])/g, "\\$1");
};

/**
 * Visits the specified data structure. For each element, the callback is called
 * as callback(element, key, container) where the key is the key of the element
 * in its container or callback (element, null, null) if there is no such container).
 */
won.visitDepthFirst = function(data, callback, currentKey, currentContainer) {
  if (data == null) return;
  if (is("Array", data) && data.length > 0) {
    for (let key in data) {
      won.visitDepthFirst(data[key], callback, key, data);
    }
    return;
  }
  if (typeof data === "object") {
    for (let key in data) {
      won.visitDepthFirst(data[key], callback, key, data);
    }
    return;
  }
  //not a container: visit value.
  callback(data, currentKey, currentContainer);
};

/**
 * Adds all elements of array2 to array1 that are not yet
 * contained in array1.
 * If both arrays are non-null, array1 is modified and returned.
 * If either one of the two is null/undefined, the other is returned
 *
 * @param array1
 * @param array2
 * @param comparatorFun (optional) comparator function to compare elements. A return value of 0 means the elements are equal.
 */
won.appendStrippingDuplicates = function(array1, array2, comparatorFun) {
  if (typeof array1 === "undefined") return array2;
  if (typeof array2 === "undefined") return array1;
  if (typeof comparatorFun === "undefined")
    comparatorFun = function(a, b) {
      return a === b;
    };
  array2
    .filter(function(item) {
      for (const entry of array1) {
        if (comparatorFun(item, entry) == 0) {
          return false;
        }
      }
      return true;
    })
    .map(function(item) {
      array1.push(item);
    });
  return array1;
};

function context2ttlPrefixes(jsonldContext) {
  return Object.entries(jsonldContext)
    .filter(([, uri]) => is("String", uri))
    .map(([prefix, uri]) => `@prefix ${prefix}: <${uri}>.`)
    .join("\n");
}

won.minimalContext = {
  msg: vocab.WONMSG.baseUri,
  won: vocab.WON.baseUri,
  con: vocab.WONCON.baseUri,
  match: vocab.WONMATCH.baseUri,
  demo: vocab.DEMO.baseUri,
  "wx-bot": vocab.BOT.baseUri,
  hold: vocab.HOLD.baseUri,
  chat: vocab.CHAT.baseUri,
  group: vocab.GROUP.baseUri,
  review: vocab.REVIEW.baseUri,
  buddy: vocab.BUDDY.baseUri,
  rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
  agr: vocab.AGR.baseUri,
  pay: vocab.PAYMENT.baseUri,
  gr: "http://purl.org/goodrelations/v1#",
  wf: vocab.WORKFLOW.baseUri,
  rdfg: "http://www.w3.org/2004/03/trix/rdfg-1/",
};
won.minimalTurtlePrefixes = context2ttlPrefixes(won.minimalContext);

won.defaultContext = {
  ...won.minimalContext,
  webID: "http://www.example.com/webids/",
  dc: "http://purl.org/dc/elements/1.1/",
  rdfs: vocab.RDFS.baseUri,
  geo: "http://www.w3.org/2003/01/geo/wgs84_pos#",
  xsd: "http://www.w3.org/2001/XMLSchema#",
  gr: "http://purl.org/goodrelations/v1#",
  ldp: "http://www.w3.org/ns/ldp#",
  sioc: "http://rdfs.org/sioc/ns#",
  dct: "http://purl.org/dc/terms/",
  cert: "http://www.w3.org/ns/auth/cert#",
  s: "http://schema.org/",
  sh: "http://www.w3.org/ns/shacl#",
  foaf: "http://xmlns.com/foaf/0.1/",
  "msg:messageType": {
    "@id": vocab.WONMSG.messageType,
    "@type": "@id",
  },
};
/** ttl-prefixes e.g. `@prefix msg: <https://w3id.org/won/message#>.\n @prefix...` */
won.defaultTurtlePrefixes = context2ttlPrefixes(won.defaultContext);

won.JsonLdHelper = {
  /**
   * Returns all graph URIs. If none are found, an empty array is returned.
   * @returns {Array}
   */
  getGraphNames: function(data) {
    //collect graph URIs in the specified dataset
    const graphs = data["@graph"];
    const graphURIs = [];
    if (graphs == null) {
      return graphURIs;
    }
    if (is("Array", graphs) && graphs.length > 0) {
      for (const graph of graphs) {
        const graphURI = graph["@id"];
        if (graphURI != null) {
          graphURIs.push(graphURI);
        }
      }
    } else if (typeof graphs === "object" && graphs["@graph"] != null) {
      return won.JsonLdHelper.getGraphNames(graphs["@graph"]);
    }
    return graphURIs;
  },

  getDefaultGraph: function(data) {
    if (data["@graph"] != null) {
      //graph keyword is present. It could represent the default graph
      // (in which case it contains only nodes) or a collection of
      //, which are nodes that contain th @graph keyword.
      //our naive test is: if the first node contains an '@graph' keyword
      //we assume the outermost @graph array to contain only named graphs.
      // we search for the one with '@id'='@default' or without
      // an '@id' keyword and return it (if we find it)
      //if the first node doesn't contain an @graph keyword, we assume that there
      //are no named graphs and all data is in the default graph.
      let outermostGraphContent = data["@graph"];
      for (const curNode of outermostGraphContent) {
        if (curNode["@graph"] == null) {
          //we assume there are no named graphs, the outermost graph is the default graph
          return outermostGraphContent;
        }
        if (curNode["@id"] == null || curNode["@id"] === "@default") {
          //we've found the named graph without an @id attribute - that's the default graph
          return curNode["@graph"];
        }
      }
      return null; //no default graph found
    } else {
      //there is no @graph keyword at top level:
      return data;
    }
  },
  getNamedGraph: function(data, graphName) {
    if (data["@graph"] != null) {
      if (data["@id"] != null) {
        if (data["@id"] === graphName) {
          return data["@graph"];
        }
      } else {
        //outermost node has '@graph' but no '@id'
        //--> @graph array contains named graphs. search for name.
        let outermostGraphContent = data["@graph"];
        for (const curNode of outermostGraphContent) {
          if (curNode["@id"] == null || curNode["@id"] === graphName) {
            //we've found the named graph without an @id attribute - that's the default graph
            return curNode["@graph"];
          }
        }
      }
    }
    return null;
  },
  getNodeInGraph: function(data, graphName, nodeId) {
    const graph = this.getNamedGraph(data, graphName);
    for (let key in graph["@graph"]) {
      const curNode = graph["@graph"][key];
      const curNodeId = curNode["@id"];
      if (curNodeId === nodeId) {
        return curNode;
      }
    }
    return null;
  },
  addDataToNode: function(data, graphName, nodeId, predicate, object) {
    const node = this.getNodeInGraph(data, graphName, nodeId);
    if (node != null) {
      node[predicate] = object;
    }
  },
  getContext: function(data) {
    return data["@context"];
  },
};

/**
 *  Adds a msg:content triple for each specified graphURI into the message graph
 * @param messageGraph
 * @param graphURIs
 */
won.addContentGraphReferencesToMessageGraph = function(
  messageGraph,
  graphURIs
) {
  if (graphURIs != null) {
    if (is("Array", graphURIs) && graphURIs.length > 0) {
      //if the message graph already contains content references, fetch them:
      const existingContentRefs = jsonldUtils.getProperty(
        messageGraph,
        vocab.WONMSG.contentCompacted
      );
      const contentGraphURIs =
        typeof existingContentRefs === "undefined" ||
        !is("Array", existingContentRefs)
          ? []
          : existingContentRefs;
      for (const graphURI of graphURIs) {
        contentGraphURIs.push({
          "@id": graphURI,
        });
      }
      jsonldUtils.setProperty(
        messageGraph,
        vocab.WONMSG.contentCompacted,
        contentGraphURIs
      );
    }
  }
};

/**
 * Adds the message graph to the json-ld structure 'builder.data' with
 * the specified messageType
 * and adds all specified graph URIs (which must be URIs of the
 * graphs to be added as content of the message) with triples
 * [message] wonmsg:content [graphURI]
 * @param graphURIs
 * @returns {won.CreateMessageBuilder}
 */
won.addMessageGraph = function(builder, graphURIs, messageType) {
  let graphs = builder.data["@graph"];
  let unsetMessageGraphUri = vocab.WONMSG.uriPlaceholder.event + "#envelope";
  //create the message graph, containing the message type
  const messageGraph = {
    "@graph": [
      {
        "@id": vocab.WONMSG.uriPlaceholder.event,
        "msg:messageType": {
          "@id": messageType,
        },
        "msg:envelope": {
          "@id": unsetMessageGraphUri,
        },
      },
      {
        "@id": unsetMessageGraphUri,
        "@type": vocab.WONMSG.EnvelopeGraphCompacted,
      },
    ],
    "@id": unsetMessageGraphUri,
  };
  won.addContentGraphReferencesToMessageGraph(messageGraph, graphURIs);
  //add the message graph to the graphs of the builder
  graphs.push(messageGraph);
  //point to the messagegraph so we can later access it easily for modifications
  builder.messageGraph = messageGraph;
};

/*
 * Creates a JSON-LD stucture containing a named graph with default 'unset' event URI
 * plus the specified hashFragment
 *
 */
won.newGraph = function(hashFragement) {
  hashFragement = hashFragement || "graph1";
  return {
    "@graph": [
      {
        "@id": vocab.WONMSG.uriPlaceholder.event + "#" + hashFragement,
        "@graph": [],
      },
    ],
  };
};

/**
 *  Work in progress: for generating any number/structure of domain objects
 *  (atom, connection container, connection ,event container, event (=wonMessage)
 *  from json-ld
 *
 */

won.WonDomainObjects = function() {};

won.WonDomainObjects.prototype = {
  constructor: won.WonDomainObjects,
  /**
   * Returns the atomURIs.
   */
  getAtomUris: function() {},
  /**
   * Returns the connection URIs.
   */
  getConnectionUris: function() {},
  /**
   * Returns the event URIs.
   */
  getEventUris: function() {},
  /**
   * Returns the domain object with the specified URI.
   * @param uri
   */
  getDomainObject: function(/*uri*/) {},
};

won.DomainObjectFactory = function() {};

won.DomainObjectFactory.prototype = {
  constructor: won.DomainObjectFactory,
  /**
   * Generates domain objects with the specified JSON-LD content. Returns a WonDomainObjects
   * instance containing all domain objects found in the JSON-LD content.
   */
  jsonLdToWonDomainObjects: function(/*jsonLdContent*/) {},
};

won.wonMessageFromJsonLd = function(wonMessageAsJsonLD) {
  return jsonld
    .expand(wonMessageAsJsonLD)
    .then(expandedJsonLd => new WonMessage(expandedJsonLd))
    .then(wonMessage =>
      wonMessage
        .frameInPromise()
        .then(
          () =>
            //Only generate compactedFramedMessage if it is not a response or not from Owner
            wonMessage.isResponse() ||
            (wonMessage.isFromOwner() &&
              wonMessage.generateCompactedFramedMessage())
        )
        .then(() => {
          if (wonMessage.hasParseErrors() && !wonMessage.isResponse()) {
            console.warn(
              "wonMessage<",
              wonMessage.getMessageUri(),
              "> msgType<",
              wonMessage.getMessageType(),
              "> ParseError: {" + wonMessage.parseErrors,
              "} wonMessage: ",
              wonMessage
            );
          }

          return wonMessage;
        })
        .catch(e => {
          console.error(
            "Error in wonMessageFromJsonLd: rawMessage: ",
            wonMessageAsJsonLD,
            " wonMessage: ",
            wonMessage
          );
          rethrow(e);
        })
    );
};

window.wonMessageFromJsonLd4dbg = won.wonMessageFromJsonLd;

/**
 * An wrapper for N3's writer that returns a promise
 * @param {*} quads list of quads following the rdfjs interface (http://rdf.js.org/)
 *   e.g.:
 * ```
 *  [ Quad {
 *      graph: DefaultGraph {id: ""}
 *      object: NamedNode {id: "http://example.org/cartoons#Cat"}
 *      predicate: NamedNode {id: "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}
 *      subject: NamedNode {id: "http://example.org/cartoons#Tom"}
 * }, Quad {...}, ...]
 * ```
 * See here for ways to create them using N3: https://www.npmjs.com/package/n3#creating-triplesquads
 * @param {*} writerArgs the arguments for intializing the writer.
 *   e.g. `{format: 'application/trig'}`. See the writer-documentation
 *   (https://github.com/RubenVerborgh/N3.js#writing) for more details.
 */
won.n3Write = async function(quads, writerArgs) {
  //const { namedNode, literal, defaultGraph, quad } = N3.DataFactory;
  const writer = new N3.Writer(writerArgs);
  return new Promise((resolve, reject) => {
    //quads.forEach(t => writer.addQuad(t))
    writer.addQuads(quads);
    writer.end((error, result) => {
      if (error) reject(error);
      else resolve(result);
    });
  });
};

/**
 * A wrapper for N3's parse that returns a promise
 * @param {*} rdf a rdf-string to be parsed
 * @param {*} parserArgs arguments for initializing the parser,
 *   e.g. `{format: 'application/n-quads'}` if you want to make
 *   parser stricter about what it accepts. See the parser-documentation
 *   (https://github.com/RubenVerborgh/N3.js#parsing) for more details.
 */
won.n3Parse = async function(rdf, parserArgs) {
  const parser = parserArgs ? new N3.Parser(parserArgs) : new N3.Parser();
  return new Promise((resolve, reject) => {
    let quads = [];
    parser.parse(rdf, (error, quad, prefixes) => {
      if (error) {
        reject(error);
      } else if (quad) {
        quads.push(quad);
      } else {
        // all quads collected
        resolve({
          quads,
          prefixes,
        });
      }
    });
  });
};

/**
 *
 * @param {string} ttl
 * @param {boolean} prependWonPrefixes
 */
won.ttlToJsonLd = function(ttl) {
  return won
    .n3Parse(ttl)
    .then(({ quads }) => won.n3Write(quads, { format: "application/n-quads" }))
    .then(quadString =>
      jsonld.fromRDF(quadString, { format: "application/n-quads" })
    )
    .catch(e => {
      e.message =
        "error while parsing the following turtle:\n\n" +
        ttl +
        "\n\n----\n\n" +
        e.message;
      throw e;
    });
};

window.ttlToJsonLd4dbg = won.ttlToJsonLd;

/**
 * Like the JSONLD-Helper, an object that wraps a won message and
 * offers convenience functions on it.
 * @param jsonLdContent
 * @constructor
 */
function WonMessage(jsonLdContent) {
  if (!(this instanceof WonMessage)) {
    return new WonMessage(jsonLdContent);
  }
  this.rawMessage = jsonLdContent;
  this.parseErrors = [];
  this.__init();
}

WonMessage.prototype = {
  constructor: WonMessage,

  getMessageUri: function() {
    return this.__getMessageUri(this.messageStructure);
  },

  __getMessageUri: function(messageStructure) {
    if (messageStructure.messageUri) {
      return messageStructure.messageUri;
    }
    if (messageStructure.containedEnvelopes) {
      let uris = messageStructure.containedEnvelopes.map(envelope =>
        this.__getMessageUri(envelope)
      );
      if (uris.length > 1) {
        throw new Error(
          "Found more than one contained envelope in message with message uris: " +
            uris
        );
      }
      if (uris.length == 0) {
        throw new Error("Did not find any contained envelopes in message");
      }
      return uris[0];
    }
  },

  getMessageDirection: function() {
    const messageDirection = this.__getMessageDirection(this.messageStructure);
    if (is("Array", messageDirection) && messageDirection.length == 1) {
      return messageDirection[0];
    }
    return messageDirection;
  },
  generateCompactedFramedMessage: async function() {
    //TODO: change it so it returns all the contentgraphscontent
    if (this.compactFramedMessage) {
      return this.compactFramedMessage;
    }
    if (this.framedMessage && this.rawMessage) {
      try {
        this.compactFramedMessage = await jsonld.compact(
          this.framedMessage,
          won.defaultContext
        );

        this.compactRawMessage = await jsonld.compact(
          this.rawMessage,
          won.defaultContext
        );

        return this.compactFramedMessage;
      } catch (e) {
        console.error(
          "Failed to generate jsonld for message ",
          this.getMessageUri(),
          "\n\n",
          e
        );
        const msg =
          "Failed to generate jsonld for message " +
          this.getMessageUri() +
          "\n\n" +
          e.message +
          "\n\n" +
          e.stack;
        this.compactFramedMessageError = msg;
      }
    }
  },
  frameInPromise: function() {
    if (this.framedMessage) {
      return Promise.resolve(this.framedMessage);
    }
    const type = this.getMessageDirection();
    const that = this;
    return jsonld
      .frame(this.rawMessage, {
        "@type": type,
      })
      .then(result => {
        const graphs = result["@graph"];
        if (graphs && graphs.length > 1) {
          const msgUri = that.getMessageUri();
          const msgGraphIndex = graphs.findIndex(
            elem => elem["@id"] === msgUri
          );
          if (msgGraphIndex != 0 && msgGraphIndex != -1) {
            let newGraphs = [];
            newGraphs.push(graphs[msgGraphIndex]);
            result["@graph"] = newGraphs.concat(
              graphs.filter(elem => elem["@id"] === msgUri)
            );

            that.framedMessage = result;
            return result;
          }
        }
        that.framedMessage = result;
        return result;
      });
  },

  getProperty: function(property) {
    let val = jsonldUtils.getProperty(this.framedMessage, property);
    if (val) {
      return this.__singleValueOrArray(val);
    }
    return null;
  },

  __singleValueOrArray: function(val) {
    if (!val) return null;
    if (is("Array", val)) {
      if (val.length == 1) {
        return won.getSafeJsonLdValue(val);
      }
      return val.map(x => won.getSafeJsonLdValue(x));
    }
    return won.getSafeJsonLdValue(val);
  },
  getContentGraphs: function() {
    // walk over graphs, copy all graphs to result that are content graphs
    // we identify content graphs by finding their URI in messageStructure.containedContent
    return this.graphs.filter(graph =>
      this.contentGraphUris.includes(graph["@id"])
    );
  },
  getContentGraphsAsJsonLD: function() {
    return JSON.stringify(this.getContentGraphs());
  },
  getCompactFramedMessageContent: function() {
    return this.compactFramedMessage;
  },
  getCompactRawMessage: function() {
    return this.compactRawMessage;
  },
  getMessageType: function() {
    return this.getProperty(vocab.WONMSG.messageType);
  },
  getInjectIntoConnectionUris: function() {
    return createArray(this.getProperty(vocab.WONMSG.injectIntoConnection));
  },
  getForwardedMessageUris: function() {
    return createArray(this.getProperty(vocab.WONMSG.forwardedMessage));
  },
  getTimestamp: function() {
    return this.getProperty(vocab.WONMSG.timestamp);
  },
  getTextMessage: function() {
    return this.getProperty(vocab.WONCON.text);
  },
  getHintScore: function() {
    return this.getProperty(vocab.WONMSG.hintScore);
  },
  getHintTargetAtom: function() {
    return this.getProperty(vocab.WONMSG.hintTargetAtom);
  },
  getHintTargetSocket: function() {
    return this.getProperty(vocab.WONMSG.hintTargetSocket);
  },
  getIsResponseTo: function() {
    return this.getProperty(vocab.WONMSG.respondingTo);
  },
  getIsRemoteResponseTo: function() {
    return this.getProperty(vocab.WONMSG.isRemoteResponseTo);
  },
  getRespondingToMessageType: function() {
    return this.getProperty(vocab.WONMSG.respondingToMessageType);
  },

  getSenderNode: function() {
    return this.getProperty(vocab.WONMSG.senderNode);
  },
  getSenderSocket: function() {
    return this.getProperty(vocab.WONMSG.senderSocket);
  },
  getSenderAtom: function() {
    return this.getProperty(vocab.WONMSG.senderAtom);
  },
  getConnection: function() {
    return this.getProperty(vocab.WONMSG.connection);
  },
  getTargetSocket: function() {
    return this.getProperty(vocab.WONMSG.recipientSocket);
  },
  getAtom: function() {
    return this.getProperty(vocab.WONMSG.atom);
  },
  getProposedMessageUris: function() {
    return createArray(this.getProperty(vocab.AGR.proposes));
  },

  getClaimsMessageUris: function() {
    return createArray(this.getProperty(vocab.AGR.claims));
  },

  getAcceptsMessageUris: function() {
    return createArray(this.getProperty(vocab.AGR.accepts));
  },
  getProposedToCancelMessageUris: function() {
    return createArray(this.getProperty(vocab.AGR.proposesToCancel));
  },
  getRejectsMessageUris: function() {
    return createArray(this.getProperty(vocab.AGR.rejects));
  },
  getRetractsMessageUris: function() {
    return createArray(this.getProperty(vocab.MOD.retracts));
  },

  isProposeMessage: function() {
    return !!this.getProperty(vocab.AGR.proposes);
  },
  isAcceptMessage: function() {
    return !!this.getProperty(vocab.AGR.accepts);
  },
  isProposeToCancel: function() {
    return !!this.getProperty(vocab.AGR.proposesToCancel);
  },
  isProposal: function() {
    return !!this.getProperty(vocab.AGR.Proposal);
  },
  isAgreement: function() {
    return !!this.getProperty(vocab.AGR.Agreement);
  },

  isRejectMessage: function() {
    return !!this.getProperty(vocab.AGR.rejects);
  },
  isRetractMessage: function() {
    return !!this.getProperty(vocab.MOD.retracts);
  },
  hasParseErrors: function() {
    return this.parseErrors && this.parseErrors.length > 0;
  },
  isFromSystem: function() {
    let direction = this.getMessageDirection();
    return direction === vocab.WONMSG.FromSystem;
  },
  isFromOwner: function() {
    let direction = this.getMessageDirection();
    return direction === vocab.WONMSG.FromOwner;
  },
  isFromExternal: function() {
    let direction = this.getMessageDirection();
    return direction === vocab.WONMSG.FromExternal;
  },

  isAtomHintMessage: function() {
    return this.getMessageType() === vocab.WONMSG.atomHintMessage;
  },
  isSocketHintMessage: function() {
    return this.getMessageType() === vocab.WONMSG.socketHintMessage;
  },
  isCreateMessage: function() {
    return this.getMessageType() === vocab.WONMSG.createMessage;
  },
  isConnectMessage: function() {
    return this.getMessageType() === vocab.WONMSG.connectMessage;
  },
  isConnectionMessage: function() {
    return this.getMessageType() === vocab.WONMSG.connectionMessage;
  },
  isCloseMessage: function() {
    return this.getMessageType() === vocab.WONMSG.closeMessage;
  },
  isHintFeedbackMessage: function() {
    return this.getMessageType() === vocab.WONMSG.hintFeedbackMessage;
  },
  isActivateMessage: function() {
    return this.getMessageType() === vocab.WONMSG.activateAtomMessage;
  },
  isDeactivateMessage: function() {
    return this.getMessageType() === vocab.WONMSG.deactivateAtomMessage;
  },
  isDeleteMessage: function() {
    return this.getMessageType() === vocab.WONMSG.deleteAtomMessage;
  },
  isAtomMessage: function() {
    return this.getMessageType() === vocab.WONMSG.AtomMessage;
  },
  isResponse: function() {
    return this.isSuccessResponse() || this.isFailureResponse();
  },
  isSuccessResponse: function() {
    return this.getMessageType() === vocab.WONMSG.successResponse;
  },
  isFailureResponse: function() {
    return this.getMessageType() === vocab.WONMSG.failureResponse;
  },
  isResponseToReplaceMessage: function() {
    return this.getRespondingToMessageType() === vocab.WONMSG.replaceMessage;
  },
  isResponseToAtomHintMessage: function() {
    return this.getRespondingToMessageType() === vocab.WONMSG.atomHintMessage;
  },
  isResponseToCreateMessage: function() {
    return this.getRespondingToMessageType() === vocab.WONMSG.createMessage;
  },
  isResponseToConnectMessage: function() {
    return this.getRespondingToMessageType() === vocab.WONMSG.connectMessage;
  },
  isResponseToConnectionMessage: function() {
    return this.getRespondingToMessageType() === vocab.WONMSG.connectionMessage;
  },
  isResponseToCloseMessage: function() {
    return this.getRespondingToMessageType() === vocab.WONMSG.closeMessage;
  },
  isResponseToHintFeedbackMessage: function() {
    return (
      this.getRespondingToMessageType() === vocab.WONMSG.hintFeedbackMessage
    );
  },
  isResponseToActivateMessage: function() {
    return (
      this.getRespondingToMessageType() === vocab.WONMSG.activateAtomMessage
    );
  },
  isResponseToDeactivateMessage: function() {
    return (
      this.getRespondingToMessageType() === vocab.WONMSG.deactivateAtomMessage
    );
  },
  isResponseToDeleteMessage: function() {
    return this.getRespondingToMessageType() === vocab.WONMSG.deleteAtomMessage;
  },
  isChangeNotificationMessage: function() {
    return this.getMessageType() === vocab.WONMSG.changeNotificationMessage;
  },

  __getMessageDirection: function(messageStructure) {
    if (messageStructure.messageDirection) {
      return messageStructure.messageDirection;
    }
    if (messageStructure.containedEnvelopes) {
      let uris = messageStructure.containedEnvelopes.map(envelope =>
        this.__getMessageDirection(envelope)
      );
      if (uris.length > 1) {
        throw new Error(
          "Found more than one contained envelope in message with message uris: " +
            uris
        );
      }
      if (uris.length == 0) {
        throw new Error("Did not find any contained envelopes in message");
      }
      return uris[0];
    }
  },

  __init: function() {
    this.graphs = this.rawMessage;

    if (!is("Array", this.graphs)) {
      this.parseErrors.push("@graph not found or not an array");
    }
    this.graphUris = this.graphs.map(g => g["@id"]);
    if (!is("Array", this.graphUris)) {
      this.parseErrors.push("GraphUris not found or not an array");
    }
    const nodes = {};
    let unreferencedEnvelopes = [];
    const innermostEnvelopes = [];
    const contentGraphUris = [];

    //first pass: create one node per envelope/content graph
    this.graphs.forEach(graph => {
      let graphUri = graph["@id"];
      if (this.__isEnvelopeGraph(graph)) {
        let node = {
          uri: graphUri,
        };
        unreferencedEnvelopes.push(graphUri);
        let msgUriAndDirection = this.__getMessageUriAndDirection(graph);
        if (msgUriAndDirection) {
          // it's possible that we don't find a triple <messageUri> a <type>
          // in the envelope, and we can't add it to the node here.
          node.messageUri = msgUriAndDirection.messageUri;
          node.messageDirection = msgUriAndDirection.messageDirection;
        }
        let messageUriAndForwardedMessageUri = this.__getMessageUriAndForwardedMessageUri(
          graph
        );
        if (messageUriAndForwardedMessageUri) {
          node.forwardedMessageUri =
            messageUriAndForwardedMessageUri.forwardedMessageUri;
        }
        nodes[graphUri] = node;
      } else if (this.__isSignatureGraph(graph)) {
        //do nothing - we don't want to handle signatures in the client for now
      } else {
        //content graph
        nodes[graphUri] = {
          uri: graphUri,
        };
      }
    });
    //second pass: connect the nodes so we get a tree
    this.graphs.forEach(graph => {
      let graphUri = graph["@id"];
      let node = nodes[graphUri];
      if (this.__isEnvelopeGraph(graph)) {
        let containedEnvelopes = this.__getContainedEnvelopeUris(graph);
        let referencesOtherGraphs = false;
        if (containedEnvelopes.length > 0) {
          referencesOtherGraphs = true;
          node.containsEnvelopes = containedEnvelopes.map(uri => nodes[uri]);
          //remember that these envelopes are now referenced
          unreferencedEnvelopes = unreferencedEnvelopes.filter(
            uri => !containedEnvelopes.includes(uri)
          );
        }
        if (node && node.forwardedMessageUri) {
          referencesOtherGraphs = true;
        }
        if (!referencesOtherGraphs) {
          //remember that this envelope contains no envelopes (and points to no remote messages)
          innermostEnvelopes.push(graphUri);
        }
        if (node.messageUri) {
          //if we know the message uri, we can look for content in this envelope
          let containedContent = this.__getContainedContentGraphUris(
            graph,
            node.messageUri
          );
          if (containedContent.length > 0) {
            node.containedContent = containedContent.map(uri => nodes[uri]);
            //remember the content graphs
            containedContent.forEach(uri => contentGraphUris.push(uri));
          }
        }
      }
    });

    // if we still have more than 1 unreferenced envelope, it must be because there is
    // a forwarded message.
    if (unreferencedEnvelopes.length > 1) {
      // one more pass: we did not connect the message and the forwardedMessage so their
      // respective local and remote messages could be connected. now we connect
      // them and remove the forwarded message from the unreferenced list
      this.graphs.forEach(graph => {
        let graphUri = graph["@id"];
        let node = nodes[graphUri];
        if (node && node.forwardedMessageUri) {
          node.forwardedMessage = nodes[node.forwardedMessageUri];
          unreferencedEnvelopes = unreferencedEnvelopes.filter(
            uri => uri != node.forwardedMessageUri
          );
        }
      });
    }

    if (innermostEnvelopes.length == 0) {
      this.parseErrors.push("no innermost envelope found");
    }
    if (innermostEnvelopes.length > 1) {
      this.parseErrors.push("more than one innermost envelope found");
    }
    if (unreferencedEnvelopes.length == 0) {
      this.parseErrors.push("no unreferenced (i.e. outermost) envelope found");
    }
    if (unreferencedEnvelopes.length > 1) {
      this.parseErrors.push(
        "more than one unreferenced (i.e. outermost) envelope found"
      );
    }
    this.messageStructure = nodes[unreferencedEnvelopes[0]]; //set the pointer to the outermost envelope

    if (!this.messageStructure) {
      this.messageStructure = {};

      const contentGraphs = this.graphs.filter(
        graph =>
          !(
            graph["@type"].includes(vocab.WONMSG.EnvelopeGraph) ||
            graph["@type"].includes(vocab.WONMSG.Signature)
          )
      );

      if (contentGraphs.length == 1) {
        this.messageStructure.messageUri = contentGraphs[0]["@id"];
        this.messageStructure.messageDirection = contentGraphs[0]["@type"];
        contentGraphUris.push(this.messageStructure.messageUri);
      }
    }

    this.contentGraphUris = contentGraphUris;
  },

  __isEnvelopeGraph: graph => {
    let graphUri = graph["@id"];
    let graphData = graph["@graph"];
    // graphType and check where removed, because otherwise the hints wont work properly
    // let graphType = graph["@type"];
    return (
      // (graphType && graphType.includes(vocab.WONMSG.EnvelopeGraph) || //
      graphData &&
      graphData.some(
        resource =>
          resource["@id"] === graphUri &&
          resource["@type"].includes(vocab.WONMSG.EnvelopeGraph)
      )
    );
  },
  __isSignatureGraph: graph => {
    let graphUri = graph["@id"];
    let graphData = graph["@graph"];
    let graphType = graph["@type"];
    return (
      (graphType && graphType.includes(vocab.WONMSG.Signature)) ||
      (graphData &&
        graphData.some(
          resource =>
            resource["@id"] === graphUri &&
            resource["@type"].includes(vocab.WONMSG.Signature)
        ))
    );
  },
  __getContainedEnvelopeUris: graph => {
    let graphUri = graph["@id"];
    let graphData = graph["@graph"];
    let data = graphData
      .filter(resource => resource["@id"] == graphUri)
      .map(resource => resource[vocab.WONMSG.containsEnvelope])
      .filter(x => x);
    if (data.length > 0) {
      return data[0].map(x => x["@id"]);
    } else {
      return [];
    }
  },
  __getContainedContentGraphUris: (graph, messageUri) => {
    let graphData = graph["@graph"];
    const contentUrisArray = graphData
      .filter(resource => resource["@id"] === messageUri)
      .map(resource => resource[vocab.WONMSG.content])
      .filter(x => x);
    if (contentUrisArray && contentUrisArray.length > 0) {
      return contentUrisArray[0].map(x => x["@id"] || x["@value"]);
    } else {
      return [];
    }
  },
  __getMessageUriAndDirection: graph => {
    let graphData = graph["@graph"];
    let data = graphData
      .filter(
        resource =>
          resource["@type"].includes(vocab.WONMSG.FromExternal) ||
          resource["@type"].includes(vocab.WONMSG.FromOwner) ||
          resource["@type"].includes(vocab.WONMSG.FromSystem)
      )
      .map(resource => ({
        messageUri: resource["@id"],
        messageDirection: resource["@type"][0], //@type is an array in expanded jsonld
      }))
      .filter(x => !!x); //if that property was not present, filter out undefineds
    if (is("Array", data)) {
      if (data.length == 0) {
        return null;
      }
      return data[0];
    }
    return data;
  },
  __getMessageUriAndForwardedMessageUri: graph => {
    let graphData = graph["@graph"];
    let data = graphData
      .filter(resource => resource[vocab.WONMSG.forwardedMessage])
      .map(resource => ({
        messageUri: resource["@id"],
        forwardedMessageUri: resource[vocab.WONMSG.forwardedMessage][0]["@id"],
      }))
      .filter(x => !!x); //if that property was not present, filter out undefineds
    if (is("Array", data)) {
      if (data.length == 0) {
        return null;
      }
      return data[0];
    }
    return data;
  },
};

/**
 * Builds a JSON-LD WoN Message or adds the relevant data to the specified
 * JSON-LD data structure.
 * @param messageType a fully qualified URI
 * @param content a JSON-LD structure or null
 * @constructor
 */
won.MessageBuilder = function MessageBuilder(messageType, content) {
  if (messageType == null) {
    throw {
      message: "messageType must not be null!",
    };
  }
  let graphNames = null;
  if (content != null) {
    this.data = won.clone(content);
    graphNames = won.JsonLdHelper.getGraphNames(this.data);
  } else {
    this.data = {
      "@graph": [],
      "@context": won.clone(won.defaultContext),
    };
  }
  this.messageGraph = null;
  this.eventUriValue = vocab.WONMSG.uriPlaceholder.event;
  won.addMessageGraph(this, graphNames, messageType);
};

won.MessageBuilder.prototype = {
  constructor: won.MessageBuilder,

  eventURI: function(eventUri) {
    this.getContext()[vocab.WONMSG.EnvelopeGraphCompacted] = {
      "@id": vocab.WONMSG.EnvelopeGraph,
      "@type": "@id",
    };
    const regex = new RegExp(won.replaceRegExp(this.eventUriValue));
    won.visitDepthFirst(this.data, function(element, key, collection) {
      if (collection != null && key === "@id") {
        if (element) collection[key] = element.replace(regex, eventUri);
      }
    });
    this.eventUriValue = eventUri;
    return this;
  },
  getContext: function() {
    return this.data["@context"];
  },
  atom: function(atomURI) {
    this.getMessageEventNode()[vocab.WONMSG.atomCompacted] = {
      "@id": atomURI,
    };
    return this;
  },
  senderAtom: function(senderAtomURI) {
    this.getMessageEventNode()[vocab.WONMSG.senderAtomCompacted] = {
      "@id": senderAtomURI,
    };
    return this;
  },
  senderSocket: function(senderSocketURI) {
    this.getMessageEventNode()[vocab.WONMSG.senderSocketCompacted] = {
      "@id": senderSocketURI,
    };
    return this;
  },
  targetSocket: function(recipientSocketURI) {
    this.getMessageEventNode()[vocab.WONMSG.recipientSocketCompacted] = {
      "@id": recipientSocketURI,
    };
    return this;
  },
  senderNode: function(senderNodeURI) {
    this.getMessageEventNode()[vocab.WONMSG.senderNodeCompacted] = {
      "@id": senderNodeURI,
    };
    return this;
  },
  sender: function(senderURI) {
    this.getMessageEventNode()[vocab.WONMSG.senderCompacted] = {
      "@id": senderURI,
    };
    return this;
  },
  recipient: function(recipientURI) {
    this.getMessageEventNode()[vocab.WONMSG.recipientCompacted] = {
      "@id": recipientURI,
    };
    return this;
  },
  recipientAtom: function(recipientAtomURI) {
    this.getMessageEventNode()[vocab.WONMSG.recipientAtomCompacted] = {
      "@id": recipientAtomURI,
    };
    return this;
  },
  recipientNode: function(recipientURI) {
    this.getMessageEventNode()[vocab.WONMSG.recipientNodeCompacted] = {
      "@id": recipientURI,
    };
    return this;
  },
  ownerDirection: function() {
    this.getMessageEventNode()["@type"] = vocab.WONMSG.FromOwnerCompacted;
    return this;
  },
  timestamp: function(timestamp) {
    this.getMessageEventNode()["msg:timestamp"] = timestamp;
    return this;
  },
  protocolVersion: function(version) {
    this.getMessageEventNode()["msg:protocolVersion"] = version;
    return this;
  },
  /**
   * Adds the specified text as text message inside the content. Can be
   * used with connectMessage, openMessage and connectionMessage.
   * @param text - text of the message
   * @returns {won.MessageBuilder}
   */
  textMessage: function(text) {
    if (text == null || text === "") {
      // text is either null, undefined, or empty
      // do nothing
    } else {
      this.getContentGraphNode()[vocab.WONCON.textCompacted] = text;
    }
    return this;
  },

  getMessageEventGraph: function() {
    return this.messageGraph;
  },
  getMessageEventNode: function() {
    return (
      (this.getMessageEventGraph() &&
        this.getMessageEventGraph()["@graph"] &&
        this.getMessageEventGraph()["@graph"][0]) ||
      this.getMessageEventGraph()
    );
  },
  /**
   * Fetches the content graph, creating it if it doesn't exist.
   */
  getContentGraph: function() {
    const graphs = this.data["@graph"];
    const contentGraphUri = this.eventUriValue + "#content";
    for (let key in graphs) {
      const graph = graphs[key];
      if (graph["@id"] === contentGraphUri) {
        return graph;
      }
    }
    //none found: create it
    const contentGraph = {
      "@id": this.eventUriValue + "#content",
      "@graph": [
        {
          "@id": this.eventUriValue,
        },
      ],
    };
    graphs.push(contentGraph);
    //add a reference to it to the envelope
    won.addContentGraphReferencesToMessageGraph(this.messageGraph, [
      contentGraphUri,
    ]);
    return contentGraph;
  },
  getContentGraphNode: function() {
    return (
      (this.getContentGraph() &&
        this.getContentGraph()["@graph"] &&
        this.getContentGraph()["@graph"][0]) ||
      this.getContentGraph()
    );
  },
  getContentGraphNodes: function() {
    if (this.getContentGraph() && this.getContentGraph()["@graph"]) {
      return this.getContentGraph()["@graph"];
    } else {
      return [this.getContentGraph()];
    }
  },
  /**
   * takes a lists of json-ld-objects and merges them into the content-graph
   */
  mergeIntoContentGraph: function(jsonldPayload) {
    const contentGraph = this.getContentGraph();
    contentGraph["@graph"] = contentGraph["@graph"].concat(jsonldPayload);
  },
  addContentGraphData: function(predicate, object) {
    this.getContentGraphNode()[predicate] = object;
    return this;
  },

  addRating: function(rating, connectionUri) {
    this.getContentGraphNode()[vocab.WONCON.feedback] = {
      "@id": "_:b0",
      "https://w3id.org/won/content#feedbackTarget": {
        "@id": connectionUri,
      },
      "https://w3id.org/won/content#binaryRating": {
        "@id": rating,
      },
    };
    return this;
  },

  build: function() {
    return this.data;
  },
};

/**
 * Optionally prepends a string, and then throws
 * whatever it gets as proper javascript error.
 * Note, that throwing an error will also
 * reject in a `Promise`-constructor-callback.
 * @param {*} e
 * @param {*} prependedMsg
 */
function rethrow(e, prependedMsg = "") {
  prependedMsg = prependedMsg ? prependedMsg + "\n" : "";

  if (is("String", e)) {
    throw new Error(prependedMsg + e);
  } else if (e.stack && e.message) {
    // a class defined
    const g = new Error(prependedMsg + e.message);
    g.stack = e.stack;
    g.response = e.response; //we add the response so we can look up why a request threw an error

    throw g;
  } else {
    throw new Error(prependedMsg + JSON.stringify(e));
  }
}

//TODO replace with `export default` after switching everything to ES6-module-syntax
export default won;
