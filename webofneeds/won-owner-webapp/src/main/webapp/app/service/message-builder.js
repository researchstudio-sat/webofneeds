/**
 * Created by ksinger on 29.06.2015.
 */

//TODO switch to requirejs for dependency mngmt (so this lib isn't angular-bound)
//TODO replace calls to `won` object to `require('util')`
import won from "./won.js";
import vocab from "./vocab.js";
(function() {
  // <message-builder-js> scope

  /**
   * Builds the dataset that makes up the message. The set consists of several named
   * graphs (usually `args.msgUri + '#nameOfSubgraph'`), that contain the payload-
   * and meta-data.
   * @param contentRdf
   * @param args
   * @returns {{@graph: Array, @context}}
   */
  won.buildMessageRdf = function(contentRdf, args) {
    const atomGraphId = args.msgUri + "#atom";
    const msgDataUri = args.msgUri + "#envelope";
    const aclGraphId = args.msgUri + "#acl";
    const msgGraph = [];

    const attachments = args.attachments ? args.attachments : [];

    const attachmentGraphIds = attachments.map(function(a, i) {
      return args.msgUri + "#attachment-" + i;
    });

    const nonEnvelopeGraphIds = Array.prototype.concat(
      [atomGraphId],
      attachmentGraphIds
    );

    msgGraph.push({
      // content
      "@id": atomGraphId,
      "@graph": contentRdf["@graph"],
    });

    /**
     * ACL
     */
    const aclGraph = [];
    const acl = args.acl ? args.acl : [];
    const authBlankNodeIds = acl.map(function(a, i) {
      return "_:auth-" + i;
    });

    acl.forEach(function(authorization, i) {
      aclGraph.push({
        "@id": authBlankNodeIds[i],
        "@type": [vocab.AUTH.AuthorizationCompacted],
        ...authorization,
      });
    });

    msgGraph.push({
      "@id": aclGraphId,
      "@graph": aclGraph,
    });
    nonEnvelopeGraphIds.push(aclGraphId);

    attachments.forEach(function(attachment, i) {
      msgGraph.push({
        "@id": attachmentGraphIds[i],
        "@graph": [
          {
            // link to attachment metadata (e.g. signatures, autor-info,...)
            // and b64-encoded attachment
            "@id": attachment.uri,
            // + .png to get image later (but without crypto-signature).

            // using ContentAsBase64: http://www.w3.org/TR/Content-in-RDF10/#ContentAsBase64Class
            "cnt:ContentAsBase64": {
              "cnt:bytes": attachment.encoding,
              "msg:contentType": attachment.encodingFormat,
              //'dct:isFormatOf : { '@id' : 'http://...png' }
              //'dct:format' : { '@id' : 'mime:png' }
            },
          },
        ],
      });
    });

    const attachmentBlankNodes = attachments.map(function(a, i) {
      return {
        "@id": "_:attachment-" + i,
        "msg:hasDestinationUri": { "@id": a.uri },
        "msg:hasAttachmentGraphUri": { "@id": attachmentGraphIds[i] },
      };
    });

    const envelopeGraph = [
      {
        "@id": args.msgUri,
        "@type": vocab.WONMSG.FromOwnerCompacted,
        "msg:protocolVersion": "1.0",
        "msg:timestamp": new Date().getTime(),
        "msg:messageType": { "@id": args.msgType },
        "msg:content": nonEnvelopeGraphIds.map(function(graphId) {
          return { "@id": graphId };
        }),
        "msg:atom": { "@id": args.publishedContentUri },
        "msg:hasAttachment": attachmentBlankNodes.map(function(n) {
          return { "@id": n["@id"] };
        }),
        "msg:envelope": { "@id": msgDataUri },
      },
      {
        "@id": msgDataUri,
        "@type": vocab.WONMSG.EnvelopeGraphCompacted,
      },
    ];

    msgGraph.push({
      // msg envelope
      "@id": msgDataUri,
      "@graph": envelopeGraph.concat(attachmentBlankNodes),
    });

    //TODO in atom: links to both unsigned (plain pngs) and signed (in rdf)  attachments

    /**
     * Copies all arguments properties recursively into a
     * new object and returns that.
     */
    function merge(/*args...*/) {
      const o = {};
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
      function mergeIntoLast(/*args...*/) {
        let obj1;
        for (const argument of arguments) {
          obj1 = arguments[arguments.length - 1];
          const obj2 = argument;
          for (const p in obj2) {
            obj1[p] = obj2[p];
          }
        }
        return obj1;
      }

      for (const argument of arguments) {
        mergeIntoLast(argument, o);
      }
      return o;
    }

    return {
      "@graph": msgGraph,
      "@context": merge(
        vocab.defaultContext,
        contentRdf["@context"],
        getTypesForContext()
      ),
    };
  };

  function getTypesForContext() {
    const o = {
      //'mime' : 'http://purl.org/NET/mediatypes/',
      //'dct' : 'http://purl.org/dc/terms/',
      cnt: "http://www.w3.org/2011/content#",
    };
    o[vocab.WONMSG.EnvelopeGraphCompacted] = {
      "@id": vocab.WONMSG.EnvelopeGraph,
      "@type": "@id",
    };
    return o;
  }
})(); // </message-builder-js> scope

// local loading should be fast enough -> take handles, load & resolve promises during (build()) TODO
/*

 STOPPED HERE
 1. generate uri
 2. serialise file into desired format
 3. push into arrays of uris and files (or an object or an array of objects)
 */

/*
 //TODO it should be possible to do the creation without initialising two message builders.
 --> CreateMessageBuilder that passes through calls to it's internal atommsgbuilder
 and then builds the messsage from that?
 */
/*

 problem: file-read is asynch, but i'd like to contain *all* the details
 of the message format in this class (e.g. the data's encoding) :|
 {atom} --toCreateMsg--> {msg} //(async) functional instead of OO-style msgbuilder?

 where:

 atom = { title: '...', ..., images: [fileHandle1, fileHandle2]  }

 this would be very implementation specific.




 */
/*
 TODO missing parameters for equivalence with old builder:

 see won-service:548f

 sender
 senderAtom (used)

 recipient
 recipientAtom

 socket (used, in atombuilder)
 targetSocket

 textMessage
 addContentGraphData(?) //who thought this was a good idea?
 */
