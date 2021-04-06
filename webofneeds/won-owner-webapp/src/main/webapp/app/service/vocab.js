// Warning: this file is imported into serviceWorkers, do not make any other imports here, otherwise the serviceWorkers might fail

let vocab = {};

vocab.RDFS = {};
vocab.RDFS.prefix = "rdfs";
vocab.RDFS.baseUri = "http://www.w3.org/2000/01/rdf-schema#";
vocab.RDFS.memberCompacted = vocab.RDFS.prefix + ":member";
vocab.RDFS.member = vocab.RDFS.baseUri + "member";

vocab.WON = {};
vocab.WON.baseUri = "https://w3id.org/won/core#";
vocab.WON.matcherURI = "https://localhost:8443/matcher/search/";
vocab.WON.prefix = "won";

vocab.WON.wonNode = vocab.WON.baseUri + "wonNode";
vocab.WON.wonNodeCompacted = vocab.WON.prefix + ":wonNode";
vocab.WON.Active = vocab.WON.baseUri + "Active";
vocab.WON.ActiveCompacted = vocab.WON.prefix + ":Active";
vocab.WON.Closed = vocab.WON.baseUri + "Closed";
vocab.WON.ClosedCompacted = vocab.WON.prefix + ":Closed";

vocab.WON.Inactive = vocab.WON.baseUri + "Inactive";
vocab.WON.InactiveCompacted = vocab.WON.prefix + ":Inactive";

vocab.WON.Deleted = vocab.WON.baseUri + "Deleted";
vocab.WON.DeletedCompacted = vocab.WON.prefix + ":Deleted";

vocab.WON.atomState = vocab.WON.baseUri + "atomState";
vocab.WON.atomStateCompacted = vocab.WON.prefix + ":atomState";
vocab.WON.socket = vocab.WON.baseUri + "socket";
vocab.WON.socketCompacted = vocab.WON.prefix + ":socket";
vocab.WON.socketDefinition = vocab.WON.baseUri + "socketDefinition";
vocab.WON.socketDefinitionCompacted = vocab.WON.prefix + ":socketDefinition";
vocab.WON.targetSocket = vocab.WON.baseUri + "targetSocket";
vocab.WON.targetSocketCompacted = vocab.WON.prefix + ":targetSocket";

vocab.WON.targetAtom = vocab.WON.baseUri + "targetAtom";
vocab.WON.targetAtomCompacted = vocab.WON.prefix + ":targetAtom";
vocab.WON.targetConnection = vocab.WON.baseUri + "targetConnection";
vocab.WON.targetConnectionCompacted = vocab.WON.prefix + ":targetConnection";

vocab.WON.Suggested = vocab.WON.baseUri + "Suggested";
vocab.WON.SuggestedCompacted = vocab.WON.baseUri + ":Suggested";
vocab.WON.RequestReceived = vocab.WON.baseUri + "RequestReceived";
vocab.WON.RequestReceivedCompacted = vocab.WON.baseUri + ":RequestReceived";
vocab.WON.RequestSent = vocab.WON.baseUri + "RequestSent";
vocab.WON.RequestSentCompacted = vocab.WON.baseUri + ":RequestSent";

vocab.WON.Connected = vocab.WON.baseUri + "Connected";
vocab.WON.ConnectedCompacted = vocab.WON.prefix + ":Connected";

vocab.WON.graph = vocab.WON.baseUri + "graph";
vocab.WON.graphCompacted = vocab.WON.prefix + ":graph";

vocab.WON.Connection = vocab.WON.baseUri + "Connection";
vocab.WON.ConnectionCompacted = vocab.WON.prefix + ":Connection";

vocab.WON.Atom = vocab.WON.baseUri + "Atom";
vocab.WON.AtomCompacted = vocab.WON.prefix + ":Atom";
vocab.WON.sourceAtom = vocab.WON.baseUri + "sourceAtom";
vocab.WON.sourceAtomCompacted = vocab.WON.prefix + ":sourceAtom";
vocab.WON.connections = vocab.WON.baseUri + "connections";
vocab.WON.connectionsCompacted = vocab.WON.prefix + ":connections";
vocab.WON.connectionState = vocab.WON.baseUri + "connectionState";
vocab.WON.connectionStateCompacted = vocab.WON.prefix + ":connectionState";
vocab.WON.previousConnectionState =
  vocab.WON.baseUri + "previousConnectionState";
vocab.WON.previousConnectionStateCompacted =
  vocab.WON.prefix + ":previousConnectionState";
vocab.WON.hasContent = vocab.WON.baseUri + "hasContent";
vocab.WON.hasContentCompacted = vocab.WON.prefix + ":content";
vocab.WON.messageContainer = vocab.WON.baseUri + "messageContainer";
vocab.WON.messageContainerCompacted = vocab.WON.prefix + ":messageContainer";

vocab.WON.Persona = vocab.WON.baseUri + "Persona";
vocab.WON.PersonaCompacted = vocab.WON.prefix + ":Persona";
vocab.WON.GenericGroupChat = vocab.WON.baseUri + "GenericGroupChat";
vocab.WON.GenericGroupChatCompacted = vocab.WON.prefix + ":GenericGroupChat";

vocab.WON.matchScore = vocab.WON.baseUri + "matchScore";
vocab.WON.matchScoreCompacted = vocab.WON.prefix + ":matchScore";
vocab.WON.matchCounterpart = vocab.WON.baseUri + "matchCounterpart";
vocab.WON.matchCounterpart = vocab.WON.prefix + ":matchCounterpart";

vocab.WON.searchResultURI = vocab.WON.baseUri + "uri";
vocab.WON.searchResultPreview = vocab.WON.baseUri + "preview";
//todo: change to SearchResult
vocab.WON.searchResult = vocab.WON.baseUri + "Match";

vocab.WON.usedForTesting = vocab.WON.prefix + "UserForTesting";

vocab.WONCON = {};
vocab.WONCON.baseUri = "https://w3id.org/won/content#";
vocab.WONCON.prefix = "con";
vocab.WONCON.text = vocab.WONCON.baseUri + "text";
vocab.WONCON.textCompacted = vocab.WONCON.prefix + ":text";
vocab.WONCON.tag = vocab.WONCON.baseUri + "tag";
vocab.WONCON.tagCompacted = vocab.WONCON.prefix + ":tag";
vocab.WONCON.feedback = vocab.WONCON.baseUri + "feedback";
vocab.WONCON.feedbackTarget = vocab.WON.baseUri + "feedbackTarget";

vocab.WONMATCH = {};
vocab.WONMATCH.baseUri = "https://w3id.org/won/matching#";
vocab.WONMATCH.prefix = "match";
vocab.WONMATCH.NoHintForCounterpartCompacted =
  vocab.WONMATCH.prefix + ":NoHintForCounterpart";
vocab.WONMATCH.UsedForTestingCompacted =
  vocab.WONMATCH.prefix + ":UsedForTesting";
vocab.WONMATCH.NoHintForMeCompacted = vocab.WONMATCH.prefix + ":NoHintForMe";
vocab.WONMATCH.flag = vocab.WONMATCH.baseUri + "flag";
vocab.WONMATCH.flagCompacted = vocab.WONMATCH.prefix + ":flag";
vocab.WONMATCH.seeks = vocab.WONMATCH.baseUri + "seeks";
vocab.WONMATCH.seeksCompacted = vocab.WONMATCH.prefix + ":seeks";
vocab.WONMATCH.doNotMatchAfter = vocab.WONMATCH.baseUri + "doNotMatchAfter";
vocab.WONMATCH.doNotMatchAfterCompacted =
  vocab.WONMATCH.prefix + ":doNotMatchAfter";
vocab.WONMATCH.sparqlQuery = vocab.WONMATCH.baseUri + "sparqlQuery";
vocab.WONMATCH.sparqlQueryCompacted = vocab.WONMATCH.prefix + ":sparqlQuery";

vocab.AGR = {};
vocab.AGR.baseUri = "https://w3id.org/won/agreement#";
vocab.AGR.prefix = "agr";
vocab.AGR.rejects = vocab.AGR.baseUri + "rejects";
vocab.AGR.proposes = vocab.AGR.baseUri + "proposes";
vocab.AGR.accepts = vocab.AGR.baseUri + "accepts";
vocab.AGR.proposesToCancel = vocab.AGR.baseUri + "proposesToCancel";
vocab.AGR.claims = vocab.AGR.baseUri + "claims";
vocab.AGR.Proposal = vocab.AGR.baseUri + "Proposal";
vocab.AGR.Agreement = vocab.AGR.baseUri + "Agreement";

vocab.MOD = {};
vocab.MOD.baseUri = "https://w3id.org/won/modification#";
vocab.MOD.retracts = vocab.MOD.baseUri + "retracts";

vocab.PAYMENT = {};
vocab.PAYMENT.baseUri = "https://w3id.org/won/payment#";
vocab.PAYMENT.prefix = "pay";

vocab.WORKFLOW = {};
vocab.WORKFLOW.baseUri = "https://w3id.org/won/workflow#";
vocab.WORKFLOW.prefix = "wf";

vocab.WONMSG = {};
vocab.WONMSG.baseUri = "https://w3id.org/won/message#";
vocab.WONMSG.prefix = "msg";
vocab.WONMSG.recipientAtom = vocab.WONMSG.baseUri + "recipientAtom";
vocab.WONMSG.recipientAtomCompacted = vocab.WONMSG.prefix + ":recipientAtom";
vocab.WONMSG.recipient = vocab.WONMSG.baseUri + "recipient";
vocab.WONMSG.recipientCompacted = vocab.WONMSG.prefix + ":recipient";
vocab.WONMSG.recipientNode = vocab.WONMSG.baseUri + "recipientNode";
vocab.WONMSG.recipientNodeCompacted = vocab.WONMSG.prefix + ":recipientNode";
vocab.WONMSG.recipientSocket = vocab.WONMSG.baseUri + "recipientSocket";
vocab.WONMSG.recipientSocketCompacted =
  vocab.WONMSG.prefix + ":recipientSocket";
vocab.WONMSG.atom = vocab.WONMSG.baseUri + "atom";
vocab.WONMSG.atomCompacted = vocab.WONMSG.prefix + ":atom";
vocab.WONMSG.senderAtom = vocab.WONMSG.baseUri + "senderAtom";
vocab.WONMSG.senderAtomCompacted = vocab.WONMSG.prefix + ":senderAtom";
vocab.WONMSG.sender = vocab.WONMSG.baseUri + "sender";
vocab.WONMSG.senderCompacted = vocab.WONMSG.prefix + ":sender";
vocab.WONMSG.senderNode = vocab.WONMSG.baseUri + "senderNode";
vocab.WONMSG.senderNodeCompacted = vocab.WONMSG.prefix + ":senderNode";
vocab.WONMSG.senderSocket = vocab.WONMSG.baseUri + "senderSocket";
vocab.WONMSG.senderSocketCompacted = vocab.WONMSG.prefix + ":senderSocket";
vocab.WONMSG.injectIntoConnection =
  vocab.WONMSG.baseUri + "injectIntoConnection";
vocab.WONMSG.forwardedMessage = vocab.WONMSG.baseUri + "forwardedMessage";
vocab.WONMSG.hintScore = vocab.WONMSG.baseUri + "hintScore";
vocab.WONMSG.hintTargetAtom = vocab.WONMSG.baseUri + "hintTargetAtom";
vocab.WONMSG.hintTargetSocket = vocab.WONMSG.baseUri + "hintTargetSocket";
vocab.WONMSG.respondingTo = vocab.WONMSG.baseUri + "respondingTo";
vocab.WONMSG.respondingToMessageType =
  vocab.WONMSG.baseUri + "respondingToMessageType";
vocab.WONMSG.messageType = vocab.WONMSG.baseUri + "messageType";
vocab.WONMSG.messageTypeCompacted = vocab.WONMSG.prefix + ":messageType";
vocab.WONMSG.timestamp = vocab.WONMSG.baseUri + "timestamp";
vocab.WONMSG.timestampCompacted = vocab.WONMSG.prefix + ":timestamp";
vocab.WONMSG.isResponseTo = vocab.WONMSG.baseUri + "isResponseTo";
vocab.WONMSG.isResponseToCompacted = vocab.WONMSG.prefix + ":isResponseTo";
vocab.WONMSG.isRemoteResponseTo = vocab.WONMSG.baseUri + "isRemoteResponseTo";
vocab.WONMSG.isRemoteResponseToCompacted =
  vocab.WONMSG.prefix + ":isRemoteResponseTo";
vocab.WONMSG.EnvelopeGraph = vocab.WONMSG.baseUri + "EnvelopeGraph";
vocab.WONMSG.EnvelopeGraphCompacted = vocab.WONMSG.prefix + ":EnvelopeGraph";

vocab.WONMSG.FromOwner = vocab.WONMSG.baseUri + "FromOwner";
vocab.WONMSG.FromOwnerCompacted = vocab.WONMSG.prefix + ":FromOwner";
vocab.WONMSG.FromExternal = vocab.WONMSG.baseUri + "FromExternal";
vocab.WONMSG.FromExternalCompacted = vocab.WONMSG.prefix + ":FromExternal";
vocab.WONMSG.FromSystem = vocab.WONMSG.baseUri + "FromSystem";
vocab.WONMSG.FromSystemCompacted = vocab.WONMSG.prefix + ":FromSystem";
vocab.WONMSG.Signature = vocab.WONMSG.baseUri + "Signature";

//message types
vocab.WONMSG.createMessage = vocab.WONMSG.baseUri + "CreateMessage";
vocab.WONMSG.createMessageCompacted = vocab.WONMSG.prefix + ":CreateMessage";
vocab.WONMSG.replaceMessage = vocab.WONMSG.baseUri + "ReplaceMessage";
vocab.WONMSG.replaceMessageCompacted = vocab.WONMSG.prefix + ":ReplaceMessage";
vocab.WONMSG.AtomMessage = vocab.WONMSG.baseUri + "AtomMessage";
vocab.WONMSG.activateAtomMessage = vocab.WONMSG.baseUri + "ActivateMessage";
vocab.WONMSG.activateAtomMessageCompacted =
  vocab.WONMSG.prefix + ":ActivateMessage";
vocab.WONMSG.deactivateAtomMessage = vocab.WONMSG.baseUri + "DeactivateMessage";
vocab.WONMSG.deactivateAtomMessageCompacted =
  vocab.WONMSG.prefix + ":DeactivateMessage";
vocab.WONMSG.deleteAtomMessage = vocab.WONMSG.baseUri + "DeleteMessage";
vocab.WONMSG.deleteAtomMessageCompacted =
  vocab.WONMSG.prefix + ":DeleteMessage";
vocab.WONMSG.deleteAtomSentMessage = vocab.WONMSG.baseUri + "DeleteSentMessage";
vocab.WONMSG.deleteAtomSentMessageCompacted =
  vocab.WONMSG.prefix + ":DeleteSentMessage";
vocab.WONMSG.closeAtomMessage = vocab.WONMSG.baseUri + "DeactivateMessage";
vocab.WONMSG.closeAtomMessageCompacted =
  vocab.WONMSG.prefix + ":DeactivateMessage";
vocab.WONMSG.closeAtomSentMessage =
  vocab.WONMSG.baseUri + "DeactivateSentMessage";
vocab.WONMSG.closeAtomSentMessageCompacted =
  vocab.WONMSG.prefix + ":DeactivateSentMessage";
vocab.WONMSG.atomHintMessage = vocab.WONMSG.baseUri + "AtomHintMessage";
vocab.WONMSG.atomHintMessageCompacted =
  vocab.WONMSG.prefix + ":AtomHintMessage";
vocab.WONMSG.socketHintMessage = vocab.WONMSG.baseUri + "SocketHintMessage";
vocab.WONMSG.socketHintMessageCompacted =
  vocab.WONMSG.prefix + ":SocketHintMessage";
vocab.WONMSG.hintFeedbackMessage = vocab.WONMSG.baseUri + "HintFeedbackMessage";
vocab.WONMSG.hintFeedbackMessageCompacted =
  vocab.WONMSG.prefix + ":HintFeedbackMessage";
vocab.WONMSG.connectMessage = vocab.WONMSG.baseUri + "ConnectMessage";
vocab.WONMSG.connectMessageCompacted = vocab.WONMSG.prefix + ":ConnectMessage";
vocab.WONMSG.connectSentMessage = vocab.WONMSG.baseUri + "ConnectSentMessage";
vocab.WONMSG.connectSentMessageCompacted =
  vocab.WONMSG.prefix + ":ConnectSentMessage";
vocab.WONMSG.atomStateMessage = vocab.WONMSG.baseUri + "AtomStateMessage";
vocab.WONMSG.atomStateMessageCompacted =
  vocab.WONMSG.prefix + ":AtomStateMessage";
vocab.WONMSG.closeMessage = vocab.WONMSG.baseUri + "CloseMessage";
vocab.WONMSG.closeMessageCompacted = vocab.WONMSG.prefix + ":CloseMessage";
vocab.WONMSG.feedbackMessage = vocab.WONMSG.baseUri + "HintFeedbackMessage";
vocab.WONMSG.openSentMessage = vocab.WONMSG.baseUri + "OpenSentMessage";
vocab.WONMSG.openSentMessageCompacted =
  vocab.WONMSG.prefix + ":OpenSentMessage";
vocab.WONMSG.changeNotificationMessage =
  vocab.WONMSG.baseUri + "ChangeNotificationMessage";
vocab.WONMSG.changeNotificationMessageCompacted =
  vocab.WONMSG.prefix + ":ChangeNotificationMessage";
vocab.WONMSG.connectionMessage = vocab.WONMSG.baseUri + "ConnectionMessage";
vocab.WONMSG.connectionMessageCompacted =
  vocab.WONMSG.prefix + ":ConnectionMessage";
vocab.WONMSG.connectionMessageSentMessage =
  vocab.WONMSG.baseUri + "ConnectionMessageSentMessage";
vocab.WONMSG.connectionMessageSentMessageCompacted =
  vocab.WONMSG.prefix + ":ConnectionMessageSentMessage";
vocab.WONMSG.connectionMessageReceivedMessage =
  vocab.WONMSG.baseUri + "ConnectionMessageReceivedMessage";
vocab.WONMSG.connectionMessageReceivedMessageCompacted =
  vocab.WONMSG.prefix + ":ConnectionMessageReceivedMessage";

vocab.WONMSG.content = vocab.WONMSG.baseUri + "content";
vocab.WONMSG.contentCompacted = vocab.WONMSG.prefix + ":content";
vocab.WONMSG.connection = vocab.WONMSG.prefix + ":connection";
vocab.WONMSG.containsEnvelope = vocab.WONMSG.prefix + ":containsEnvelope";

//response types
vocab.WONMSG.successResponse = vocab.WONMSG.baseUri + "SuccessResponse";
vocab.WONMSG.successResponseCompacted =
  vocab.WONMSG.prefix + ":SuccessResponse";
vocab.WONMSG.failureResponse = vocab.WONMSG.baseUri + "FailureResponse";
vocab.WONMSG.failureResponseCompacted =
  vocab.WONMSG.prefix + ":FailureResponse";

vocab.HOLD = {};
vocab.HOLD.baseUri = "https://w3id.org/won/ext/hold#";
vocab.HOLD.prefix = "hold";
vocab.HOLD.HoldableSocket = vocab.HOLD.baseUri + "HoldableSocket";
vocab.HOLD.HoldableSocketCompacted = vocab.HOLD.prefix + ":HoldableSocket";
vocab.HOLD.HolderSocket = vocab.HOLD.baseUri + "HolderSocket";
vocab.HOLD.HolderSocketCompacted = vocab.HOLD.prefix + ":HolderSocket";
vocab.HOLD.heldByCompacted = vocab.HOLD.prefix + ":heldBy";
vocab.HOLD.holdsCompacted = vocab.HOLD.prefix + ":holds";
vocab.HOLD.ScopeReadHeldAtoms = vocab.HOLD.baseUri + "ScopeReadHeldAtoms";
vocab.HOLD.ScopeReadHeldAtomsCompacted =
  vocab.HOLD.prefix + ":ScopeReadHeldAtoms";

vocab.BOT = {};
vocab.BOT.baseUri = "https://w3id.org/won/ext/bot#";
vocab.BOT.prefix = "wx-bot";
vocab.BOT.ServiceAtom = vocab.BOT.baseUri + "ServiceAtom";
vocab.BOT.ServiceAtomCompacted = vocab.BOT.prefix + ":ServiceAtom";

vocab.DEMO = {};
vocab.DEMO.baseUri = "https://w3id.org/won/ext/demo#";
vocab.DEMO.prefix = "demo";

vocab.WXPERSONA = {};
vocab.WXPERSONA.baseUri = "https://w3id.org/won/ext/persona#";
vocab.WXPERSONA.prefix = "wx-persona";
vocab.WXPERSONA.Interest = vocab.WXPERSONA.baseUri + "Interest";
vocab.WXPERSONA.InterestCompacted = vocab.WXPERSONA.prefix + ":Interest";
vocab.WXPERSONA.Expertise = vocab.WXPERSONA.baseUri + "Expertise";
vocab.WXPERSONA.ExpertiseCompacted = vocab.WXPERSONA.prefix + ":Expertise";
vocab.WXPERSONA.InterestSocket = vocab.WXPERSONA.baseUri + "InterestSocket";
vocab.WXPERSONA.InterestSocketCompacted =
  vocab.WXPERSONA.prefix + ":InterestSocket";
vocab.WXPERSONA.ExpertiseSocket = vocab.WXPERSONA.baseUri + "ExpertiseSocket";
vocab.WXPERSONA.ExpertiseSocketCompacted =
  vocab.WXPERSONA.prefix + ":ExpertiseSocket";
vocab.WXPERSONA.InterestOfSocket = vocab.WXPERSONA.baseUri + "InterestOfSocket";
vocab.WXPERSONA.InterestOfSocketCompacted =
  vocab.WXPERSONA.prefix + ":InterestOfSocket";
vocab.WXPERSONA.ExpertiseOfSocket =
  vocab.WXPERSONA.baseUri + "ExpertiseOfSocket";
vocab.WXPERSONA.ExpertiseOfSocketCompacted =
  vocab.WXPERSONA.prefix + ":ExpertiseOfSocket";

vocab.CHAT = {};
vocab.CHAT.baseUri = "https://w3id.org/won/ext/chat#";
vocab.CHAT.prefix = "chat";
vocab.CHAT.ChatSocket = vocab.CHAT.baseUri + "ChatSocket";
vocab.CHAT.ChatSocketCompacted = vocab.CHAT.prefix + ":ChatSocket";

vocab.GROUP = {};
vocab.GROUP.baseUri = "https://w3id.org/won/ext/group#";
vocab.GROUP.prefix = "group";
vocab.GROUP.GroupSocket = vocab.GROUP.baseUri + "GroupSocket";
vocab.GROUP.GroupSocketCompacted = vocab.GROUP.prefix + ":GroupSocket";

vocab.PROJECT = {};
vocab.PROJECT.baseUri = "https://w3id.org/won/ext/project#";
vocab.PROJECT.prefix = "wx-project";
vocab.PROJECT.ProjectSocket = vocab.PROJECT.baseUri + "ProjectSocket";
vocab.PROJECT.ProjectSocketCompacted = vocab.PROJECT.prefix + ":ProjectSocket";
vocab.PROJECT.ProjectOfSocket = vocab.PROJECT.baseUri + "ProjectOfSocket";
vocab.PROJECT.ProjectOfSocketCompacted =
  vocab.PROJECT.prefix + ":ProjectOfSocket";
vocab.PROJECT.RelatedProjectSocket =
  vocab.PROJECT.baseUri + "RelatedProjectSocket";
vocab.PROJECT.RelatedProjectSocketCompacted =
  vocab.PROJECT.prefix + ":RelatedProjectSocket";

vocab.BUDDY = {};
vocab.BUDDY.baseUri = "https://w3id.org/won/ext/buddy#";
vocab.BUDDY.prefix = "buddy";
vocab.BUDDY.BuddySocket = vocab.BUDDY.baseUri + "BuddySocket";
vocab.BUDDY.BuddySocketCompacted = vocab.BUDDY.prefix + ":BuddySocket";
vocab.BUDDY.buddyCompacted = vocab.BUDDY.prefix + ":buddy";

vocab.WXSCHEMA = {};
vocab.WXSCHEMA.baseUri = "https://w3id.org/won/ext/schema#";
vocab.WXSCHEMA.prefix = "wx-schema";

vocab.WXSCHEMA.MemberOfSocket = vocab.WXSCHEMA.baseUri + "MemberOfSocket";
vocab.WXSCHEMA.MemberOfSocketCompacted =
  vocab.WXSCHEMA.prefix + ":MemberOfSocket";

vocab.WXSCHEMA.OrganizationRoleOfSocket =
  vocab.WXSCHEMA.baseUri + "OrganizationRoleOfSocket";
vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted =
  vocab.WXSCHEMA.prefix + ":OrganizationRoleOfSocket";

vocab.WXSCHEMA.MemberSocket = vocab.WXSCHEMA.baseUri + "MemberSocket";
vocab.WXSCHEMA.MemberSocketCompacted = vocab.WXSCHEMA.prefix + ":MemberSocket";

vocab.WXSCHEMA.AttendeeSocket = vocab.WXSCHEMA.baseUri + "AttendeeSocket";
vocab.WXSCHEMA.AttendeeSocketCompacted =
  vocab.WXSCHEMA.prefix + ":AttendeeSocket";

vocab.WXSCHEMA.AttendeeInverseSocket =
  vocab.WXSCHEMA.baseUri + "AttendeeInverseSocket";
vocab.WXSCHEMA.AttendeeInverseSocketCompacted =
  vocab.WXSCHEMA.prefix + ":AttendeeInverseSocket";

vocab.WXSCHEMA.WorksForSocket = vocab.WXSCHEMA.baseUri + "WorksForSocket";
vocab.WXSCHEMA.WorksForSocketCompacted =
  vocab.WXSCHEMA.prefix + ":WorksForSocket";

vocab.WXSCHEMA.WorksForInverseSocket =
  vocab.WXSCHEMA.baseUri + "WorksForInverseSocket";
vocab.WXSCHEMA.WorksForInverseSocketCompacted =
  vocab.WXSCHEMA.prefix + ":WorksForInverseSocket";

vocab.WXSCHEMA.AssociatedArticleSocket =
  vocab.WXSCHEMA.baseUri + "AssociatedArticleSocket";
vocab.WXSCHEMA.AssociatedArticleSocketCompacted =
  vocab.WXSCHEMA.prefix + ":AssociatedArticleSocket";

vocab.WXSCHEMA.AssociatedArticleInverseSocket =
  vocab.WXSCHEMA.baseUri + "AssociatedArticleInverseSocket";
vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted =
  vocab.WXSCHEMA.prefix + ":AssociatedArticleInverseSocket";

vocab.WXSCHEMA.SubOrganizationSocket =
  vocab.WXSCHEMA.baseUri + "SubOrganizationSocket";
vocab.WXSCHEMA.SubOrganizationSocketCompacted =
  vocab.WXSCHEMA.prefix + ":SubOrganizationSocket";

vocab.WXSCHEMA.ParentOrganizationSocket =
  vocab.WXSCHEMA.baseUri + "ParentOrganizationSocket";
vocab.WXSCHEMA.ParentOrganizationSocketCompacted =
  vocab.WXSCHEMA.prefix + ":ParentOrganizationSocket";

vocab.WXSCHEMA.ReviewSocket = vocab.WXSCHEMA.baseUri + "ReviewSocket";
vocab.WXSCHEMA.ReviewSocketCompacted = vocab.WXSCHEMA.prefix + ":ReviewSocket";

vocab.WXSCHEMA.EventSocket = vocab.WXSCHEMA.baseUri + "EventSocket";
vocab.WXSCHEMA.EventSocketCompacted = vocab.WXSCHEMA.prefix + ":EventSocket";

vocab.WXSCHEMA.EventInverseSocket =
  vocab.WXSCHEMA.baseUri + "EventInverseSocket";
vocab.WXSCHEMA.EventInverseSocketCompacted =
  vocab.WXSCHEMA.prefix + ":EventInverseSocket";

vocab.WXSCHEMA.EventInverseSocket =
  vocab.WXSCHEMA.baseUri + "EventInverseSocket";
vocab.WXSCHEMA.EventInverseSocketCompacted =
  vocab.WXSCHEMA.prefix + ":EventInverseSocket";

vocab.WXSCHEMA.ReviewInverseSocket =
  vocab.WXSCHEMA.baseUri + "ReviewInverseSocket";
vocab.WXSCHEMA.ReviewInverseSocketCompacted =
  vocab.WXSCHEMA.prefix + ":ReviewInverseSocket";

vocab.AUTH = {};
vocab.AUTH.baseUri = "https://w3id.org/won/auth#";
vocab.AUTH.prefix = "auth";
vocab.AUTH.acl = vocab.AUTH.baseUri + "acl";
vocab.AUTH.aclCompacted = vocab.AUTH.prefix + ":acl";
vocab.AUTH.Authorization = vocab.AUTH.baseUri + "Authorization";
vocab.AUTH.AuthorizationCompacted = vocab.AUTH.prefix + ":Authorization";
vocab.AUTH.bearer = vocab.AUTH.baseUri + "bearer";
vocab.AUTH.bearerCompacted = vocab.AUTH.prefix + ":bearer";
vocab.AUTH.grantee = vocab.AUTH.baseUri + "grantee";
vocab.AUTH.granteeCompacted = vocab.AUTH.prefix + ":grantee";
vocab.AUTH.grant = vocab.AUTH.baseUri + "grant";
vocab.AUTH.grantCompacted = vocab.AUTH.prefix + ":grant";
vocab.AUTH.bearer = vocab.AUTH.baseUri + "bearer";
vocab.AUTH.bearerCompacted = vocab.AUTH.prefix + ":bearer";
vocab.AUTH.provideAuthInfo = vocab.AUTH.baseUri + "provideAuthInfo";
vocab.AUTH.provideAuthInfoCompacted = vocab.AUTH.prefix + ":provideAuthInfo";
vocab.AUTH.requestedBy = vocab.AUTH.baseUri + "requestedBy";
vocab.AUTH.requestedByCompacted = vocab.AUTH.prefix + ":requestedBy";
vocab.AUTH.socket = vocab.AUTH.baseUri + "socket";
vocab.AUTH.socketCompacted = vocab.AUTH.prefix + ":socket";
vocab.AUTH.socketType = vocab.AUTH.baseUri + "socketType";
vocab.AUTH.socketTypeCompacted = vocab.AUTH.prefix + ":socketType";
vocab.AUTH.connection = vocab.AUTH.baseUri + "connection";
vocab.AUTH.connectionCompacted = vocab.AUTH.prefix + ":connection";
vocab.AUTH.connections = vocab.AUTH.baseUri + "connections";
vocab.AUTH.connectionsCompacted = vocab.AUTH.prefix + ":connections";
vocab.AUTH.connectionState = vocab.AUTH.baseUri + "connectionState";
vocab.AUTH.connectionStateCompacted = vocab.AUTH.prefix + ":connectionState";
vocab.AUTH.targetAtom = vocab.AUTH.baseUri + "targetAtom";
vocab.AUTH.targetAtomCompacted = vocab.AUTH.prefix + ":targetAtom";
vocab.AUTH.graph = vocab.AUTH.baseUri + "graph";
vocab.AUTH.graphCompacted = vocab.AUTH.prefix + ":graph";
vocab.AUTH.operation = vocab.AUTH.baseUri + "operation";
vocab.AUTH.operationCompacted = vocab.AUTH.prefix + ":operation";
vocab.AUTH.atom = vocab.AUTH.baseUri + "atom";
vocab.AUTH.atomCompacted = vocab.AUTH.prefix + ":atom";
vocab.AUTH.opRead = vocab.AUTH.baseUri + "opRead";
vocab.AUTH.opReadCompacted = vocab.AUTH.prefix + ":opRead";
vocab.AUTH.opConnectClose = vocab.AUTH.baseUri + "opConnectClose";
vocab.AUTH.opConnectCloseCompacted = vocab.AUTH.prefix + ":opConnectClose";
vocab.AUTH.opCommunicate = vocab.AUTH.baseUri + "opCommunicate";
vocab.AUTH.opCommunicateCompacted = vocab.AUTH.prefix + ":opCommunicate";
vocab.AUTH.connectionMessages = vocab.AUTH.baseUri + "connectionMessages";
vocab.AUTH.connectionMessagesCompacted =
  vocab.AUTH.prefix + ":connectionMessages";
vocab.AUTH.inherit = vocab.AUTH.baseUri + "inherit";
vocab.AUTH.inheritCompacted = vocab.AUTH.prefix + ":inherit";
vocab.AUTH.atomExpressionShape = vocab.AUTH.baseUri + "atomExpressionShape";
vocab.AUTH.atomExpressionShapeCompacted =
  vocab.AUTH.prefix + ":atomExpressionShape";
vocab.AUTH.anyone = vocab.AUTH.baseUri + "anyone";
vocab.AUTH.anyoneCompacted = vocab.AUTH.prefix + ":anyone";
vocab.AUTH.operationRequestor = vocab.AUTH.baseUri + "operationRequestor";
vocab.AUTH.operationRequestorCompacted =
  vocab.AUTH.prefix + ":operationRequestor";
vocab.AUTH.tokenScope = vocab.AUTH.baseUri + "tokenScope";
vocab.AUTH.tokenScopeCompacted = vocab.AUTH.prefix + ":tokenScope";
vocab.AUTH.issuer = vocab.AUTH.baseUri + "issuer";
vocab.AUTH.issuerCompacted = vocab.AUTH.prefix + ":issuer";
vocab.AUTH.requestToken = vocab.AUTH.baseUri + "requestToken";
vocab.AUTH.requestTokenCompacted = vocab.AUTH.prefix + ":requestToken";
vocab.AUTH.expiresAfter = vocab.AUTH.baseUri + "expiresAfter";
vocab.AUTH.expiresAfterCompacted = vocab.AUTH.prefix + ":expiresAfter";

vocab.VALUEFLOWS = {};
vocab.VALUEFLOWS.baseUri = "https://w3id.org/valueflows#";
vocab.VALUEFLOWS.prefix = "vf";
vocab.VALUEFLOWS.EconomicResource =
  vocab.VALUEFLOWS.baseUri + "EconomicResource";
vocab.VALUEFLOWS.EconomicResourceCompacted =
  vocab.VALUEFLOWS.prefix + ":EconomicResource";

vocab.WXVALUEFLOWS = {};
vocab.WXVALUEFLOWS.baseUri = "https://w3id.org/won/ext/valueflows#";
vocab.WXVALUEFLOWS.prefix = "wx-vf";
vocab.WXVALUEFLOWS.Activity = vocab.WXVALUEFLOWS.baseUri + "Activity";
vocab.WXVALUEFLOWS.ActivityCompacted = vocab.WXVALUEFLOWS.prefix + ":Activity";
vocab.WXVALUEFLOWS.PrimaryAccountableSocket =
  vocab.WXVALUEFLOWS.baseUri + "PrimaryAccountableSocket";
vocab.WXVALUEFLOWS.PrimaryAccountableSocketCompacted =
  vocab.WXVALUEFLOWS.prefix + ":PrimaryAccountableSocket";
vocab.WXVALUEFLOWS.PrimaryAccountableOfSocket =
  vocab.WXVALUEFLOWS.baseUri + "PrimaryAccountableOfSocket";
vocab.WXVALUEFLOWS.PrimaryAccountableOfSocketCompacted =
  vocab.WXVALUEFLOWS.prefix + ":PrimaryAccountableOfSocket";
vocab.WXVALUEFLOWS.CustodianSocket =
  vocab.WXVALUEFLOWS.baseUri + "CustodianSocket";
vocab.WXVALUEFLOWS.CustodianSocketCompacted =
  vocab.WXVALUEFLOWS.prefix + ":CustodianSocket";
vocab.WXVALUEFLOWS.CustodianOfSocket =
  vocab.WXVALUEFLOWS.baseUri + "CustodianOfSocket";
vocab.WXVALUEFLOWS.CustodianOfSocketCompacted =
  vocab.WXVALUEFLOWS.prefix + ":CustodianOfSocket";
vocab.WXVALUEFLOWS.ResourceSocket =
  vocab.WXVALUEFLOWS.baseUri + "ResourceSocket";
vocab.WXVALUEFLOWS.ResourceSocketCompacted =
  vocab.WXVALUEFLOWS.prefix + ":ResourceSocket";
vocab.WXVALUEFLOWS.ResourceActivitySocket =
  vocab.WXVALUEFLOWS.baseUri + "ResourceActivitySocket";
vocab.WXVALUEFLOWS.ResourceActivitySocketCompacted =
  vocab.WXVALUEFLOWS.prefix + ":ResourceActivitySocket";
vocab.WXVALUEFLOWS.SupportableSocket =
  vocab.WXVALUEFLOWS.baseUri + "SupporatbleSocket";
vocab.WXVALUEFLOWS.SupportableSocketCompacted =
  vocab.WXVALUEFLOWS.prefix + ":SupportableSocket";
vocab.WXVALUEFLOWS.SupporterSocket =
  vocab.WXVALUEFLOWS.baseUri + "SupporterSocket";
vocab.WXVALUEFLOWS.SupporterSocketCompacted =
  vocab.WXVALUEFLOWS.prefix + ":SupporterSocket";
vocab.WXVALUEFLOWS.ActorSocket = vocab.WXVALUEFLOWS.baseUri + "ActorSocket";
vocab.WXVALUEFLOWS.ActorSocketCompacted =
  vocab.WXVALUEFLOWS.prefix + ":ActorSocket";
vocab.WXVALUEFLOWS.ActorActivitySocket =
  vocab.WXVALUEFLOWS.baseUri + "ActorActivitySocket";
vocab.WXVALUEFLOWS.ActorActivitySocketCompacted =
  vocab.WXVALUEFLOWS.prefix + ":ActorActivitySocket";
vocab.WXVALUEFLOWS.PartnerActivitySocket =
  vocab.WXVALUEFLOWS.baseUri + "PartnerActivitySocket";
vocab.WXVALUEFLOWS.PartnerActivitySocketCompacted =
  vocab.WXVALUEFLOWS.prefix + ":PartnerActivitySocket";

// UTILS
vocab.WONMSG.uriPlaceholder = Object.freeze({
  message: "wm:/SELF",
});

vocab.WON.contentNodeBlankUri = Object.freeze({
  seeks: "_:seeksAtomContent",
});

/* This constant is limiting the connected connections
 to a holdableSocket to one, if a socket is not listed
 in this object, the capacity is not limited

 socketCapacity could be retrieved from the socketDefinition
 e.g. see won-ext-hold.ttl and won-ext-core.ttl -> won:socketCapacity */
vocab.socketCapacity = {
  [vocab.HOLD.HoldableSocketCompacted]: 1,
  [vocab.WXSCHEMA.ParentOrganizationSocketCompacted]: 1,
  [vocab.WXSCHEMA.ReviewInverseSocketCompacted]: 1,
  [vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted]: 1,
  [vocab.WXVALUEFLOWS.PrimaryAccountableSocketCompacted]: 1,
  [vocab.WXVALUEFLOWS.CustodianSocketCompacted]: 1,
  [vocab.WXVALUEFLOWS.ActorSocketCompacted]: 1,
  [vocab.WXVALUEFLOWS.SupportableSocketCompacted]: 1,
  [vocab.WXPERSONA.ExpertiseOfSocketCompacted]: 1,
  [vocab.WXPERSONA.InterestOfSocketCompacted]: 1,
};

/* These sockets are defined as holdable sockets (to show the connected atom as a holder of the atom in question)
* Although it is not a strict rule, an atom should only have either one of those sockets, or at least only one
* connected connection over all defined socket
* */
vocab.holdableSocketTypes = [
  vocab.WXPERSONA.ExpertiseOfSocketCompacted,
  vocab.WXPERSONA.InterestOfSocketCompacted,
  vocab.HOLD.HoldableSocketCompacted,
  vocab.WXSCHEMA.ParentOrganizationSocketCompacted,
  vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted,
];

vocab.holderSockets = {
  [vocab.WXPERSONA.ExpertiseOfSocketCompacted]:
    vocab.WXPERSONA.ExpertiseSocketCompacted,
  [vocab.WXPERSONA.InterestOfSocketCompacted]:
    vocab.WXPERSONA.InterestSocketCompacted,
  [vocab.HOLD.HoldableSocketCompacted]: vocab.HOLD.HolderSocketCompacted,
  [vocab.WXSCHEMA.ParentOrganizationSocketCompacted]:
    vocab.WXSCHEMA.SubOrganizationSocketCompacted,
  [vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted]:
    vocab.WXSCHEMA.MemberSocketCompacted,
};

/* This constant is switching the element view of the specific socket
 from a extra tab entry, to listing all the exisiting connections within the detail tab of an atom
*/
vocab.tagViewSockets = {
  [vocab.WXPERSONA.ExpertiseSocketCompacted]: true,
  [vocab.WXPERSONA.InterestSocketCompacted]: true,
};

/* This constant is to prevent connections to non owned atoms for specific sockets */
vocab.refuseAddToNonOwned = {
  [vocab.HOLD.HoldableSocketCompacted]: true,
  [vocab.HOLD.HolderSocketCompacted]: true,
  [vocab.WXPERSONA.ExpertiseOfSocketCompacted]: true,
  [vocab.WXPERSONA.InterestOfSocketCompacted]: true,
  [vocab.WXPERSONA.ExpertiseSocketCompacted]: true,
  [vocab.WXPERSONA.InterestSocketCompacted]: true,
};

vocab.minimalContext = {
  [vocab.WONMSG.prefix]: vocab.WONMSG.baseUri,
  [vocab.WON.prefix]: vocab.WON.baseUri,
  [vocab.WONCON.prefix]: vocab.WONCON.baseUri,
  [vocab.WONMATCH.prefix]: vocab.WONMATCH.baseUri,
  [vocab.DEMO.prefix]: vocab.DEMO.baseUri,
  [vocab.WXPERSONA.prefix]: vocab.WXPERSONA.baseUri,
  [vocab.BOT.prefix]: vocab.BOT.baseUri,
  [vocab.PROJECT.prefix]: vocab.PROJECT.baseUri,
  [vocab.WXSCHEMA.prefix]: vocab.WXSCHEMA.baseUri,
  [vocab.HOLD.prefix]: vocab.HOLD.baseUri,
  [vocab.CHAT.prefix]: vocab.CHAT.baseUri,
  [vocab.GROUP.prefix]: vocab.GROUP.baseUri,
  [vocab.BUDDY.prefix]: vocab.BUDDY.baseUri,
  [vocab.AGR.prefix]: vocab.AGR.baseUri,
  [vocab.PAYMENT.prefix]: vocab.PAYMENT.baseUri,
  [vocab.WORKFLOW.prefix]: vocab.WORKFLOW.baseUri,
  [vocab.VALUEFLOWS.prefix]: vocab.VALUEFLOWS.baseUri,
  [vocab.WXVALUEFLOWS.prefix]: vocab.WXVALUEFLOWS.baseUri,
  [vocab.AUTH.prefix]: vocab.AUTH.baseUri,
  rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
  gr: "http://purl.org/goodrelations/v1#",
  rdfg: "http://www.w3.org/2004/03/trix/rdfg-1/",
};
vocab.defaultContext = {
  ...vocab.minimalContext,
  [vocab.RDFS.prefix]: vocab.RDFS.baseUri,
  webID: "http://www.example.com/webids/",
  dc: "http://purl.org/dc/elements/1.1/",
  geo: "http://www.w3.org/2003/01/geo/wgs84_pos#",
  xsd: "http://www.w3.org/2001/XMLSchema#",
  gr: "http://purl.org/goodrelations/v1#",
  ldp: "http://www.w3.org/ns/ldp#",
  sioc: "http://rdfs.org/sioc/ns#",
  dct: "http://purl.org/dc/terms/",
  cert: "http://www.w3.org/ns/auth/cert#",
  s: "http://schema.org/",
  sh: "http://www.w3.org/ns/shacl#",
  om2: "http://www.ontology-of-units-of-measure.org/resource/om-2/",
  foaf: "http://xmlns.com/foaf/0.1/",
  "msg:messageType": {
    "@id": vocab.WONMSG.messageType,
    "@type": "@id",
  },
};

/** ttl-prefixes e.g. `@prefix msg: <https://w3id.org/won/message#>.\n @prefix...` */
vocab.defaultTurtlePrefixes = context2ttlPrefixes(vocab.defaultContext);

function context2ttlPrefixes(jsonldContext) {
  function is(type, obj) {
    const clas = Object.prototype.toString.call(obj).slice(8, -1);
    return obj !== undefined && obj !== null && clas === type;
  }

  return Object.entries(jsonldContext)
    .filter(([, uri]) => is("String", uri))
    .map(([prefix, uri]) => `@prefix ${prefix}: <${uri}>.`)
    .join("\n");
}

export default vocab;
