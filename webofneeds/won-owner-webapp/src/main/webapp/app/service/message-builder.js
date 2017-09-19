/**
 * Created by ksinger on 29.06.2015.
 */

//TODO switch to requirejs for dependency mngmt (so this lib isn't angular-bound)
//TODO replace calls to `won` object to `require('util')`
import won from './won.js';
(function(){ // <message-builder-js> scope

    /**
     * Builds the dataset that makes up the message. The set consists of several named
     * graphs (usually `args.msgUri + '#nameOfSubgraph'`), that contain the payload-
     * and meta-data.
     * @param contentRdf
     * @param args
     * @returns {{@graph: Array, @context}}
     */
    won.buildMessageRdf = function (contentRdf, args) {
        var needGraphId = args.msgUri + '#need';
        var msgDataUri = args.msgUri + '#envelope';
        var msgGraph = [];

        var attachments = args.attachments? args.attachments : [];


        var attachmentGraphIds = attachments.map(function(a, i){ return args.msgUri + '#attachment-' + i })
        var nonEnvelopeGraphIds = Array.prototype.concat([needGraphId], attachmentGraphIds);

        msgGraph.push({
            // content
            '@id': needGraphId,
            '@graph': contentRdf['@graph']
        });

        attachments.forEach(function(attachment, i) {
            msgGraph.push({
                '@id' : attachmentGraphIds[i],
                '@graph' : [
                    {
                        // link to attachment metadata (e.g. signatures, autor-info,...)
                        // and b64-encoded attachment
                        '@id': attachment.uri,
                        // + .png to get image later (but without crypto-signature).

                        // using ContentAsBase64: http://www.w3.org/TR/Content-in-RDF10/#ContentAsBase64Class
                        'cnt:ContentAsBase64' : {
                            'cnt:bytes': attachment.data,
                            'msg:contentType': attachment.type
                            //'dct:isFormatOf : { '@id' : 'http://...png' }
                            //'dct:format' : { '@id' : 'mime:png' }
                        }
                    }
                ]
            });
        });

        var attachmentBlankNodes = attachments.map(function(a, i) {
            return {
                '@id': '_:attachment-' + i,
                'msg:hasDestinationUri': {'@id' : a.uri},
                'msg:hasAttachmentGraphUri': {'@id' : attachmentGraphIds[i]}
            }
        });

        var envelopeGraph = [
            {
                '@id': args.msgUri,
                '@type' : 'msg:FromOwner',
                'msg:hasSentTimestamp' : (new Date().getTime()),
                'msg:hasMessageType': { '@id': args.msgType },
                'msg:hasContent': nonEnvelopeGraphIds.map(function(graphId) {return {'@id': graphId} }),
                'msg:hasReceiverNode': { '@id': args.receiverNode },
                'msg:hasSenderNode': { '@id': args.senderNode },
                'msg:hasSenderNeed': { '@id': args.publishedContentUri },
                'msg:hasAttachment': attachmentBlankNodes.map(function(n) {return {'@id' : n['@id']} }),
            },
            {
                '@id': msgDataUri,
                '@type': 'msg:EnvelopeGraph',
                'rdfg:subGraphOf': { '@id': args.msgUri }
            }
        ];

        console.log('message-builder.js:buildMessageRdf:attachmentBlankNodes: ', attachmentBlankNodes);
        console.log('message-builder.js:buildMessageRdf:envelopeGraph: ', envelopeGraph.concat(attachmentBlankNodes));

        msgGraph.push({
            // msg envelope
            '@id': msgDataUri,
            '@graph': envelopeGraph.concat(attachmentBlankNodes)
        });


        /*
         //TODO in need: links to both unsigned (plain pngs) and signed (in rdf) attachments
         */

        return {
            '@graph': msgGraph,
            '@context': won.merge(
                won.defaultContext,
                contentRdf['@context'],
                getTypesForContext()
            )
        }
    }


    function getTypesForContext(){
        var o = {
            //'mime' : 'http://purl.org/NET/mediatypes/',
            //'dct' : 'http://purl.org/dc/terms/',
            'cnt' : 'http://www.w3.org/2011/content#'
        };
        o[won.WONMSG.EnvelopeGraphCompacted] = {
            '@id': 'http://purl.org/webofneeds/message#EnvelopeGraph',
            '@type': '@id'
        };
        return o;
    }
})() // </message-builder-js> scope


// local loading should be fast enough -> take handles, load & resolve promises during (build()) TODO
/*

 STOPPED HERE
 1. generate uri
 2. serialise file into desired format
 3. push into arrays of uris and files (or an object or an array of objects)
 */

/*
 //TODO it should be possible to do the creation without initialising two message builders.
 --> CreateMessageBuilder that passes through calls to it's internal needmsgbuilder
 and then builds the messsage from that?
 */
/*

 problem: file-read is asynch, but i'd like to contain *all* the details
 of the message format in this class (e.g. the data's encoding) :|
 {need} --toCreateMsg--> {msg} //(async) functional instead of OO-style msgbuilder?

 where:

 need = { title: '...', ..., images: [fileHandle1, fileHandle2]  }

 this would be very implementation specific.




 */
/*
 TODO missing parameters for equivalence with old builder:

 see won-service:548f

 forEnvelopeData(?)

 hasSender
 hasSenderNode
 hasSenderNeed (used)

 hasReceiver
 hasReceiverNeed
 hasReceiverNode (used)

 hasFacet (used, in needbuilder)
 hasRemoteFacet

 hasTextMessage
 addContentGraphData(?) //who thought this was a good idea?
 */
