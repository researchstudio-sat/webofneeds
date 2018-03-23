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

package won.bot.framework.eventbot.action.impl.debugbot;

import java.net.URI;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.springframework.util.StopWatch;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.behaviour.CrawlConnectionDataBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateNeedCommandEvent;
import won.bot.framework.eventbot.event.impl.crawlconnection.CrawlConnectionCommandEvent;
import won.bot.framework.eventbot.event.impl.crawlconnection.CrawlConnectionCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.debugbot.ConnectDebugCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.HintDebugCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.MessageToElizaEvent;
import won.bot.framework.eventbot.event.impl.debugbot.SendNDebugCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.SetCacheEagernessCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.SetChattinessDebugCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.UsageDebugCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.agreement.ConversationMessage;
import won.protocol.agreement.effect.Accepts;
import won.protocol.agreement.effect.Proposes;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonConversationUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.validation.WonConnectionValidator;


/**
 * Listener that reacts to incoming messages, creating internal bot events for them
 */
public class DebugBotIncomingMessageToEventMappingAction extends BaseEventBotAction {

    Pattern PATTERN_USAGE = Pattern.compile("^usage|\\?|help|debug$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_HINT = Pattern.compile("^hint$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_CLOSE = Pattern.compile("^close$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_CONNECT = Pattern.compile("^connect$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_DEACTIVATE = Pattern.compile("^deactivate$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_CHATTY_ON = Pattern.compile("^chatty\\s+on$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_CHATTY_OFF = Pattern.compile("^chatty\\s+off$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_CACHE_EAGER = Pattern.compile("^cache\\s+eager$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_CACHE_LAZY = Pattern.compile("^cache\\s+lazy$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_SEND_N = Pattern.compile("^send ([1-9])$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_VALIDATE = Pattern.compile("^validate$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_RETRACT = Pattern.compile("^retract$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_RETRACT_MINE = Pattern.compile("^retract mine$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_PROPOSE = Pattern.compile("^propose$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_PROPOSE_MINE = Pattern.compile("^propose mine$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_ACCEPT = Pattern.compile("^accept$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_PROPOSE_TO_CANCEL = Pattern.compile("^propose cancel$", Pattern.CASE_INSENSITIVE);
    Pattern PATTERN_ACCEPT_PROPOSAL_TO_CANCEL = Pattern.compile("^accept cancel$", Pattern.CASE_INSENSITIVE);


    public static final String[] USAGE_MESSAGES = {
            "You are connected to the debug bot. You can issue commands that will cause interactions with your need.",
            "Usage:",
            "    'hint':            create a new need and send hint to it",
            "    'connect':         create a new need and send connection request to it",
            "    'close':           close the current connection",
            "    'deactivate':      deactivate remote need of the current connection",
            "    'chatty on|off':   send chat messages spontaneously every now and then? (default: on)",
            "    'send N':          send N messages, one per second. N must be an integer between 1 and 9",
            "    'validate':        download the connection data and validate it",
            "    'retract':         retract the last message sent",
            "    'retract mine':    retract the last message you sent (should not have any effect)",
            "    'propose':         propose my last message for an agreement",
            "    'propose mine':    propose your last message for an agreement",
            "    'accept':          accept the last proposal made",
            "    'propose cancel:   propose to cancel the newest agreement",
            "    'accept cancel:    accept latest proposal to cancel",
            "    'cache eager|lazy: use lazy or eager RDF cache",
            "    'usage':           display this message"
    };

    public static final String[] N_MESSAGES = {
            "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"
    };

    public static final String[] RANDOM_MESSAGES = {
            "Is there anything I can do for you?",
            "Did you read the news today?",
            "By the way, don't you just love the weather these days?",
            "Type 'usage' to see what I can do for you!",
            "I think I might see a movie tonight",
    };

    public static final String[] LAST_MESSAGES = {
            "?", "Are you still there?", "Gone?", "... cu later, I guess?", "Do you still require my services? You can use " +
            "the 'close' command, you know...", "Ping?"
    };

    public DebugBotIncomingMessageToEventMappingAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(final Event event, EventListener executingListener) throws Exception {
        if (event instanceof BaseNeedAndConnectionSpecificEvent) {
            handleTextMessageEvent((ConnectionSpecificEvent) event);
        }
    }

    private void handleTextMessageEvent(final ConnectionSpecificEvent messageEvent) {
        if (messageEvent instanceof MessageEvent) {
            EventListenerContext ctx = getEventListenerContext();
            EventBus bus = ctx.getEventBus();

            Connection con = ((BaseNeedAndConnectionSpecificEvent) messageEvent).getCon();
            WonMessage msg = ((MessageEvent) messageEvent).getWonMessage();
            String message = extractTextMessageFromWonMessage(msg);

            try {
            	if (message == null) {
            		Model messageModel = WonRdfUtils.MessageUtils.textMessage("Whatever you sent me there, it was not a normal text message. I'm expecting a <message> won:hasTextMessage \"Some text\" triple in that message.");
                    bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
            	} else if (PATTERN_USAGE.matcher(message).matches()) {
                    bus.publish(new UsageDebugCommandEvent(con));
                } else if (PATTERN_HINT.matcher(message).matches()) {
                    Model messageModel = WonRdfUtils.MessageUtils.textMessage("Ok, I'll create a new need and make it send a hint to you.");

                    bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
                    bus.publish(new HintDebugCommandEvent(con));
                } else if (PATTERN_CONNECT.matcher(message).matches()) {
                    Model messageModel = WonRdfUtils.MessageUtils.textMessage("Ok, I'll create a new need and make it send a connect to you.");

                    bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
                    bus.publish(new ConnectDebugCommandEvent(con));
                } else if (PATTERN_CLOSE.matcher(message).matches()) {
                    Model messageModel = WonRdfUtils.MessageUtils.textMessage("Ok, I'll close this connection");

                    bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
                    bus.publish(new CloseCommandEvent(con));
                } else if (PATTERN_DEACTIVATE.matcher(message).matches()) {
                    Model messageModel = WonRdfUtils.MessageUtils.textMessage("Ok, I'll deactivate this need. This will close the connection we are currently talking on.");

                    bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
                    bus.publish(new DeactivateNeedCommandEvent(con.getNeedURI()));
                } else if (PATTERN_CHATTY_ON.matcher(message).matches()) {
                    Model messageModel = WonRdfUtils.MessageUtils.textMessage("Ok, I'll send you messages spontaneously from time to time.");

                    bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
                    bus.publish(new SetChattinessDebugCommandEvent(con, true));
                } else if (PATTERN_CHATTY_OFF.matcher(message).matches()) {
                    Model messageModel = WonRdfUtils.MessageUtils.textMessage("Ok, from now on I will be quiet and only respond to your messages.");

                    bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
                    bus.publish(new SetChattinessDebugCommandEvent(con, false));
                } else if (PATTERN_CACHE_EAGER.matcher(message).matches()) {
                	Model messageModel = WonRdfUtils.MessageUtils.textMessage("Ok, I'll put any message I receive or send into the RDF cache. This slows down message processing in general, but operations that require crawling connection data will be faster.");
                	
                	bus.publish(new SetCacheEagernessCommandEvent(true));
                    bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
                } else if (PATTERN_CACHE_LAZY.matcher(message).matches()) {
                	Model messageModel = WonRdfUtils.MessageUtils.textMessage("Ok, I won't put messages I receive or send into the RDF cache. This speeds up message processing in general, but operations that require crawling connection data will be slowed down.");
                	bus.publish(new SetCacheEagernessCommandEvent(false));
                    bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
                } else if (PATTERN_SEND_N.matcher(message).matches()) {
                    Matcher m = PATTERN_SEND_N.matcher(message);
                    m.find();
                    String nStr = m.group(1);
                    int n = Integer.parseInt(nStr);

                    bus.publish(new SendNDebugCommandEvent(con, n));
                }else if (PATTERN_VALIDATE.matcher(message).matches()) {
                   validate(ctx,bus,con);
                } else if (PATTERN_RETRACT.matcher(message).matches()) {
                    retractLatestMessage(ctx, bus, con);
                } else if (PATTERN_RETRACT_MINE.matcher(message).matches()) {
                	retractLatestMessageOfCounterpart(ctx, bus, con);
                } else if (PATTERN_PROPOSE.matcher(message).matches()) {
                	proposeLatestMessage(ctx, bus, con);
                } else if (PATTERN_PROPOSE_MINE.matcher(message).matches()) {
                	proposeLatestMessageOfCounterpart(ctx, bus, con);
                } else if (PATTERN_ACCEPT.matcher(message).matches()) {
                	acceptLatestProposal(ctx, bus, con);
                } else if (PATTERN_ACCEPT_PROPOSAL_TO_CANCEL.matcher(message).matches()) {
                	acceptLatestProposalToCancel(ctx, bus, con);
                } else if (PATTERN_PROPOSE_TO_CANCEL.matcher(message).matches()) {
                	proposeToCancelLatestAccept(ctx, bus, con);

                } else {
                    //default: answer with eliza.
                    bus.publish(new MessageToElizaEvent(con, message));
                }
            } catch (Exception e) {
                //error: send an error message
                Model messageModel = WonRdfUtils.MessageUtils.textMessage("Did not understand your command '" + message + "': " + e.getClass().getSimpleName() + ":" + e.getMessage());
                bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
            }
        }
    }

    private String extractTextMessageFromWonMessage(WonMessage wonMessage) {
        if (wonMessage == null) return null;
        String message = WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
        return StringUtils.trim(message);
    }
    
    /***********************************************************************************
     * Mini framework for allowing the bot to refer to earlier messages
     * while trying to avoid code duplication
     ***********************************************************************************/
    
    private interface MessageFinder{
    	List<URI> findMessages(Dataset conversation);
    }
    
    private interface MessageReferrer {
    	Model referToMessages(Model messageModel, URI... targetUris);
    }
    
    private interface TextMessageMaker{
    	String makeTextMessage(Duration queryDuration, Dataset conversation, URI...uris);
    }
   

	private void referToEarlierMessages(EventListenerContext ctx, EventBus bus, Connection con, String crawlAnnouncement, MessageFinder messageFinder, MessageReferrer messageReferrer, TextMessageMaker textMessageMaker) {
		Model messageModel = WonRdfUtils.MessageUtils
		        .textMessage(crawlAnnouncement);
		bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
		// initiate crawl behaviour
		CrawlConnectionCommandEvent command = new CrawlConnectionCommandEvent(con.getNeedURI(), con.getConnectionURI());
		CrawlConnectionDataBehaviour crawlConnectionDataBehaviour = new CrawlConnectionDataBehaviour(ctx, command, Duration.ofSeconds(60));
		final StopWatch crawlStopWatch = new StopWatch();
		crawlStopWatch.start("crawl");
		crawlConnectionDataBehaviour.onResult(new SendMessageReportingCrawlResultAction(ctx, con, crawlStopWatch));
		crawlConnectionDataBehaviour.onResult(new SendMessageOnCrawlResultAction(ctx, con) {
			@Override
			protected Model makeSuccessMessage(CrawlConnectionCommandSuccessEvent successEvent) {
				return makeReferringMessage(successEvent.getCrawledData(), messageFinder, messageReferrer, textMessageMaker);	
			}
		});
		crawlConnectionDataBehaviour.activate();
	}
    
    private Model makeReferringMessage(Dataset conversation, MessageFinder messageFinder, MessageReferrer messageReferrer, TextMessageMaker textMessageMaker) {
		int origPrio = Thread.currentThread().getPriority();
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		StopWatch queryStopWatch = new StopWatch();
		queryStopWatch.start("query");
		List<URI> targetUris = messageFinder.findMessages(conversation); 
		URI[] targetUriArray = targetUris.toArray(new URI[targetUris.size()]);
		queryStopWatch.stop();
		Thread.currentThread().setPriority(origPrio);
		Duration queryDuration = Duration.ofMillis(queryStopWatch.getLastTaskTimeMillis());
        Model messageModel = WonRdfUtils.MessageUtils.textMessage(textMessageMaker.makeTextMessage(queryDuration, conversation, targetUriArray));
        return messageReferrer.referToMessages(messageModel, targetUriArray);
	}
    
    
	private void retractLatestMessage(EventListenerContext ctx, EventBus bus, Connection con) {
		referToEarlierMessages(ctx, bus, con, 
				"ok, I'll retract my latest message - but 'll need to crawl the connection data first, please be patient.", 
				conversationDataset -> {
					URI uri = WonConversationUtils.getNthLatestMessageOfNeed(conversationDataset, con.getNeedURI(),1);
					return uri == null ? Collections.EMPTY_LIST : Arrays.asList(uri);
				},
				(messageModel, uris) -> WonRdfUtils.MessageUtils.addRetracts(messageModel, uris),
				(Duration queryDuration, Dataset conversationDataset, URI... uris) -> {
					if (uris == null || uris.length == 0 || uris[0] == null) {
						return "Sorry, I cannot retract any of my messages - I did not find any.";
					}
					String retractedString = WonConversationUtils.getTextMessage(conversationDataset, uris[0]);
					retractedString = (retractedString == null)? ", which had no text message" : ", which read, '"+retractedString+"'";
			        return "Ok, I am hereby retracting my last message"+retractedString+" (uri: " + uris[0]+")."
			        		+ "\n The query for finding that message took " + getDurationString(queryDuration) + " seconds.";
				});
	}

	private String getDurationString(Duration queryDuration) {
		return new DecimalFormat("###.##").format( queryDuration.toMillis() / 1000d);
	}
	
	private void validate(EventListenerContext ctx, EventBus bus, Connection con) {
		Model messageModel = WonRdfUtils.MessageUtils
		        .textMessage("ok, I'll validate the connection - but I'll need to crawl the connection data first, please be patient.");
		bus.publish(new ConnectionMessageCommandEvent(con, messageModel));
		// initiate crawl behaviour
		CrawlConnectionCommandEvent command = new CrawlConnectionCommandEvent(con.getNeedURI(), con.getConnectionURI());
		CrawlConnectionDataBehaviour crawlConnectionDataBehaviour = new CrawlConnectionDataBehaviour(ctx, command, Duration.ofSeconds(60));
		final StopWatch crawlStopWatch = new StopWatch();
		crawlStopWatch.start("crawl");
		crawlConnectionDataBehaviour.onResult(new SendMessageReportingCrawlResultAction(ctx, con, crawlStopWatch));
		crawlConnectionDataBehaviour.onResult(new SendMessageOnCrawlResultAction(ctx, con) {
			@Override
			protected Model makeSuccessMessage(CrawlConnectionCommandSuccessEvent successEvent) {
				 try {
			            logger.debug("validating data of connection {}", command.getConnectionURI());
			            //TODO: use one validator for all invocations
			            WonConnectionValidator validator = new WonConnectionValidator();
			            StringBuilder message = new StringBuilder();
			            boolean valid = validator.validate(successEvent.getCrawledData(), message);
			            String successMessage =
			            "Connection " + command.getConnectionURI() + " is valid: " + valid + " " + message.toString();
			            return WonRdfUtils.MessageUtils
                                .textMessage(successMessage);
			        } catch (Exception e) {
			            return WonRdfUtils.MessageUtils.textMessage("Caught exception during validation: " + e);
			        }		
			}
		});
		crawlConnectionDataBehaviour.activate();
	}
	
	private void retractLatestMessageOfCounterpart(EventListenerContext ctx, EventBus bus, Connection con) {
		referToEarlierMessages(ctx, bus, con, 
				"ok, I'll retract your latest message (which should not have any effect) - but I'll need to crawl the connection data first, please be patient.", 
				conversationDataset -> {
					URI uri = WonConversationUtils.getNthLatestMessageOfNeed(conversationDataset, con.getRemoteNeedURI(),1);
					return uri == null ? Collections.EMPTY_LIST : Arrays.asList(uri);
				}, 
				(messageModel, uris) -> WonRdfUtils.MessageUtils.addRetracts(messageModel, uris),
				(Duration queryDuration, Dataset conversationDataset, URI... uris) -> {
					if (uris == null || uris.length == 0 || uris[0] == null) {
						return "Sorry, I cannot retract any of your messages - I did not find any.";
					}
					String retractedString = WonConversationUtils.getTextMessage(conversationDataset, uris[0]);
					retractedString = (retractedString == null)? ", which had no text message" : ", which read, '"+retractedString+"'";
			        return "Ok, I am hereby retracting your last message"+retractedString+" (uri: " + uris[0]+")."
			        		+ "\n The query for finding that message took " + getDurationString(queryDuration) + " seconds.";
				});
	}
	
	private void proposeLatestMessage(EventListenerContext ctx, EventBus bus, Connection con) {
		referToEarlierMessages(ctx, bus, con, 
				"ok, I'll propose my latest message - but I'll need to crawl the connection data first, please be patient.", 
				conversationDataset -> {
					URI uri = WonConversationUtils.getNthLatestMessageOfNeed(conversationDataset, con.getNeedURI(),1);
					return uri == null ? Collections.EMPTY_LIST : Arrays.asList(uri);
				}, 
				(messageModel, uris) -> WonRdfUtils.MessageUtils.addProposes(messageModel, uris),
				(Duration queryDuration, Dataset conversationDataset, URI... uris) -> {
					if (uris == null || uris.length == 0 || uris[0] == null) {
						return "Sorry, I cannot propose any of my messages - I did not find any.";
					}
					String proposeedString = WonConversationUtils.getTextMessage(conversationDataset, uris[0]);
					proposeedString = (proposeedString == null)? ", which had no text message" : ", which read, '"+proposeedString+"'";
			        return "Ok, I am hereby proposing my last message"+proposeedString+" (uri: " + uris[0]+")."
			        		+ "\n The query for finding that message took " + getDurationString(queryDuration) + " seconds.";
				});
	}
	
	private void proposeLatestMessageOfCounterpart(EventListenerContext ctx, EventBus bus, Connection con) {
		referToEarlierMessages(ctx, bus, con, 
				"ok, I'll propose your latest message - but I'll need to crawl the connection data first, please be patient.", 
				conversationDataset -> {
					URI uri = WonConversationUtils.getNthLatestMessageOfNeed(conversationDataset, con.getRemoteNeedURI(),1);
					return uri == null ? Collections.EMPTY_LIST : Arrays.asList(uri);
				}, 
				(messageModel, uris) -> WonRdfUtils.MessageUtils.addProposes(messageModel, uris),
				(Duration queryDuration, Dataset conversationDataset, URI... uris) -> {
					if (uris == null || uris.length == 0 || uris[0] == null) {
						return "Sorry, I cannot propose one of your messages - I did not find any";
					}
					String proposeedString = WonConversationUtils.getTextMessage(conversationDataset, uris[0]);
					proposeedString = (proposeedString == null)? ", which had no text message" : ", which read, '"+proposeedString+"'";
			        return "Ok, I am hereby proposing your last message"+proposeedString+" (uri: " + uris[0]+")."
			        		+ "\n The query for finding that message took " + getDurationString(queryDuration) + " seconds.";
				});
	}
	
	private void acceptLatestProposal(EventListenerContext ctx, EventBus bus, Connection con) {
		referToEarlierMessages(ctx, bus, con, 
				"ok, I'll accept your latest proposal - but I'll need to crawl the connection data first, please be patient.", 
				conversationDataset -> {
					URI uri = WonConversationUtils.getLatestProposesMessageOfNeed(conversationDataset, con.getRemoteNeedURI());
					return uri == null ? Collections.EMPTY_LIST : Arrays.asList(uri);
				}, 
				(messageModel, uris) -> WonRdfUtils.MessageUtils.addAccepts(messageModel, uris),
				(Duration queryDuration, Dataset conversationDataset, URI... uris) -> {
					if (uris == null || uris.length == 0 || uris[0] == null) {
						return "Sorry, I cannot accept any proposal - I did not find any 'agr:proposes' messages";
					}
					String proposeedString = WonConversationUtils.getTextMessage(conversationDataset, uris[0]);
					proposeedString = (proposeedString == null)? ", which had no text message" : ", which read, '"+proposeedString+"'";
			        return "Ok, I am hereby accepting your last proposal"+proposeedString+" (uri: " + uris[0]+")."
			        		+ "\n The query for finding that message took " + getDurationString(queryDuration) + " seconds.";
				});
	}
	
	private void acceptLatestProposalToCancel(EventListenerContext ctx, EventBus bus, Connection con) {
		referToEarlierMessages(ctx, bus, con, 
				"ok, I'll accept your latest proposal to cancel - but I'll need to crawl the connection data first, please be patient.", 
				//conversationDataset -> Lists.newArrayList(WonConversationUtils.getLatestProposesToCancelMessageOfNeed(conversationDataset, con.getRemoteNeedURI())), 
				conversationDataset -> {
					URI uri = WonConversationUtils.getNthLatestMessage(conversationDataset, 
					msg -> con.getRemoteNeedURI().equals(((ConversationMessage) msg).getSenderNeedURI()) 
						&& ((ConversationMessage) msg).isProposesToCancelMessage()
						&& ((ConversationMessage) msg).getEffects().stream().anyMatch(e -> e instanceof Proposes && ((Proposes)e).getProposesToCancel().size() > 0) 
					,1);
					return uri == null ? Collections.EMPTY_LIST : Arrays.asList(uri);
				},
				(messageModel, uris) -> WonRdfUtils.MessageUtils.addAccepts(messageModel, uris),
				(Duration queryDuration, Dataset conversationDataset, URI... uris) -> {
					if (uris == null || uris.length == 0 || uris[0] == null || conversationDataset == null) {
						return "Sorry, I cannot accept a proposal to cancel - I did not find any 'agr:proposesToCancel' messages";
					}
					String proposeedString = WonConversationUtils.getTextMessage(conversationDataset, uris[0]);
					proposeedString = (proposeedString == null)? ", which had no text message" : ", which read, '"+proposeedString+"'";
			        return "Ok, I am hereby accepting your last proposal to cancel"+proposeedString+" (uri: " + uris[0]+")."
			        		+ "\n The query for finding that message took " + getDurationString(queryDuration) + " seconds.";
				});
	}
	
	private void proposeToCancelLatestAccept(EventListenerContext ctx, EventBus bus, Connection con) {
		referToEarlierMessages(ctx, bus, con, 
				"ok, I'll propose to cancel our latest agreement (assuming the latest accept I find is a valid agreement) - but I'll need to crawl the connection data first, please be patient.", 
				conversationDataset -> {
					URI uri = WonConversationUtils.getNthLatestMessage(conversationDataset, 
					msg -> con.getRemoteNeedURI().equals(((ConversationMessage) msg).getSenderNeedURI()) 
						&& ((ConversationMessage) msg).isAcceptsMessage()
						&& ((ConversationMessage) msg).getEffects().stream().anyMatch(e -> e instanceof Accepts) 
					,1);
					return uri == null ? Collections.EMPTY_LIST : Arrays.asList(uri);
				},
				(messageModel, uris) -> WonRdfUtils.MessageUtils.addProposesToCancel(messageModel, uris),
				(Duration queryDuration, Dataset conversationDataset, URI... uris) -> {
					if (uris == null || uris.length == 0 || uris[0] == null || conversationDataset == null) {
						return "Sorry, I cannot propose to cancel - I did not find any 'agr:accept' messages";
					}
					String proposeedString = WonConversationUtils.getTextMessage(conversationDataset, uris[0]);
					proposeedString = (proposeedString == null)? ", which had no text message" : ", which read, '"+proposeedString+"'";
			        return "Ok, I am hereby proposing to cancel our latest agreement (if it is one)"+proposeedString+" (uri: " + uris[0]+")."
			        		+ "\n The query for finding that message took " + getDurationString(queryDuration) + " seconds.";
				});
	}

   
    
   
    
    
}
