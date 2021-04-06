import { deepFreeze, isValidNumber, getIn } from "./utils.js";
import vocab from "./service/vocab.js";
import * as atomUtils from "./redux/utils/atom-utils.js";

export const rdfTextfieldHelpText =
  "Expects valid turtle. " +
  `<${vocab.WONMSG.uriPlaceholder.message}> will ` +
  "be replaced by the uri generated for this message. " +
  "Use it, so your TTL can be found when parsing the messages. " +
  "See `vocab.defaultTurtlePrefixes` " +
  "for prefixes that will be added automatically. E.g." +
  `\`<${vocab.WONMSG.uriPlaceholder.message}> con:text "hello world!". \``;

export const noParsableContentPlaceholder =
  "«This message couldn't be displayed as it didn't contain," +
  "any parsable content! " +
  'Click on the "Show raw RDF data"-button in ' +
  'the footer of the page to see the "raw" message-data.»';

const labels = deepFreeze({
  connectionState: {
    [vocab.WON.Suggested]: "Suggested",
    [vocab.WON.RequestSent]: "Requested by you",
    [vocab.WON.RequestReceived]: "Requested",
    [vocab.WON.Connected]: "Open",
    [vocab.WON.Closed]: "Closed",
  },
  messageType: {
    [vocab.WONMSG.connectMessage]: "Connect Message",
    [vocab.WONMSG.closeMessage]: "Close Message",
    [vocab.WONMSG.connectionMessage]: "Chat Message",
    [vocab.WONMSG.atomHintMessage]: "Atom Hint Message",
    [vocab.WONMSG.socketHintMessage]: "Socket Hint Message",
    [vocab.WONMSG.hintFeedbackMessage]: "Hint Feedback Message",
  },
  sockets: {
    [vocab.GROUP.GroupSocketCompacted]: "Group Chat",
    [vocab.CHAT.ChatSocketCompacted]: "Chat",
    [vocab.WXSCHEMA.MemberSocketCompacted]: "Members",
    [vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted]: "Role Of",
    [vocab.HOLD.HoldableSocketCompacted]: "Holdable",
    [vocab.WXVALUEFLOWS.SupportableSocketCompacted]: "Supportable",
    [vocab.HOLD.HolderSocketCompacted]: "Holder",
    [vocab.WXVALUEFLOWS.SupporterSocketCompacted]: "Supporter",
    [vocab.WXSCHEMA.ReviewSocketCompacted]: "Review",
    [vocab.BUDDY.BuddySocketCompacted]: "Buddy",
    [vocab.WXVALUEFLOWS.PrimaryAccountableOfSocketCompacted]:
      "Primary Accountable",
    [vocab.WXVALUEFLOWS.PrimaryAccountableSocketCompacted]:
      "Primary Of Accountable",
    [vocab.WXVALUEFLOWS.CustodianOfSocketCompacted]: "Custodian",
    [vocab.WXVALUEFLOWS.CustodianSocketCompacted]: "Custodian Of",
    [vocab.WXVALUEFLOWS.ResourceSocketCompacted]: "Affected Resources",
    [vocab.WXVALUEFLOWS.ResourceActivitySocketCompacted]: "Activities",
    [vocab.WXVALUEFLOWS.ActorSocketCompacted]: "Actor",
    [vocab.WXVALUEFLOWS.ActorActivitySocketCompacted]: "Activities",
    [vocab.WXVALUEFLOWS.PartnerActivitySocketCompacted]: "Partner Activities",
    [vocab.WXSCHEMA.MemberOfSocketCompacted]: "Memberships",
    [vocab.WXSCHEMA.WorksForSocketCompacted]: "Employers",
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: "Articles",
    [vocab.WXSCHEMA.SubOrganizationSocketCompacted]: "Sub Organizations",
    [vocab.WXSCHEMA.ParentOrganizationSocketCompacted]: "Parent Organization",
    [vocab.WXSCHEMA.EventSocketCompacted]: "Events",
    [vocab.WXPERSONA.InterestOfSocketCompacted]: "Interest Of",
    [vocab.WXPERSONA.InterestSocketCompacted]: "Interests",
    [vocab.WXPERSONA.ExpertiseOfSocketCompacted]: "Expertise Of",
    [vocab.WXPERSONA.ExpertiseSocketCompacted]: "Expertises",
    [vocab.PROJECT.ProjectSocketCompacted]: "Projects",
    [vocab.PROJECT.ProjectOfSocketCompacted]: "Project Of",
    [vocab.PROJECT.RelatedProjectSocketCompacted]: "Related Projects",
    [vocab.WXSCHEMA.AttendeeSocketCompacted]: "Attendees",
    [vocab.WXSCHEMA.EventInverseSocketCompacted]: "Organizers",
    [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: "Publishers",
  },
  helpText: {
    [vocab.GROUP.GroupSocketCompacted]: "TODO",
    [vocab.CHAT.ChatSocketCompacted]: "TODO ",
    [vocab.WXSCHEMA.MemberSocketCompacted]: "TODO",
    [vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted]: "TODO",
    [vocab.HOLD.HoldableSocketCompacted]: "TODO",
    [vocab.WXVALUEFLOWS.SupportableSocketCompacted]: "TODO",
    [vocab.HOLD.HolderSocketCompacted]: "TODO",
    [vocab.WXVALUEFLOWS.SupporterSocketCompacted]: "TODO",
    [vocab.WXSCHEMA.ReviewSocketCompacted]: "TODO",
    [vocab.BUDDY.BuddySocketCompacted]: "TODO",
    [vocab.WXVALUEFLOWS.PrimaryAccountableOfSocketCompacted]: "TODO",
    [vocab.WXVALUEFLOWS.PrimaryAccountableSocketCompacted]: "TODO",
    [vocab.WXVALUEFLOWS.CustodianOfSocketCompacted]: "TODO",
    [vocab.WXVALUEFLOWS.CustodianSocketCompacted]: "TODO",
    [vocab.WXVALUEFLOWS.ResourceSocketCompacted]: "TODO",
    [vocab.WXVALUEFLOWS.ResourceActivitySocketCompacted]: "TODO",
    [vocab.WXVALUEFLOWS.ActorSocketCompacted]: "TODO",
    [vocab.WXVALUEFLOWS.ActorActivitySocketCompacted]: "TODO",
    [vocab.WXVALUEFLOWS.PartnerActivitySocketCompacted]: "TODO",
    [vocab.WXSCHEMA.MemberOfSocketCompacted]: "TODO",
    [vocab.WXSCHEMA.WorksForSocketCompacted]: "TODO",
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: "TODO",
    [vocab.WXSCHEMA.SubOrganizationSocketCompacted]: "TODO",
    [vocab.WXSCHEMA.ParentOrganizationSocketCompacted]: "TODO",
    [vocab.WXSCHEMA.EventSocketCompacted]: "TODO",
    [vocab.WXPERSONA.InterestOfSocketCompacted]: "TODO",
    [vocab.WXPERSONA.InterestSocketCompacted]: "TODO",
    [vocab.WXPERSONA.ExpertiseOfSocketCompacted]: "TODO",
    [vocab.WXPERSONA.ExpertiseSocketCompacted]: "TODO",
    [vocab.PROJECT.ProjectSocketCompacted]: "TODO",
    [vocab.PROJECT.ProjectOfSocketCompacted]: "TODO",
    [vocab.PROJECT.RelatedProjectSocketCompacted]: "TODO",
  },
  socketTabs: {
    [vocab.GROUP.GroupSocketCompacted]: "Group Chat Members",
    [vocab.CHAT.ChatSocketCompacted]: "Chats",
    [vocab.HOLD.HoldableSocketCompacted]: "Held By",
    [vocab.WXVALUEFLOWS.SupportableSocketCompacted]: "Supported By",
    [vocab.HOLD.HolderSocketCompacted]: "Posts",
    [vocab.WXVALUEFLOWS.SupporterSocketCompacted]: "Supports",
    [vocab.WXSCHEMA.ReviewSocketCompacted]: "Reviews",
    [vocab.WXSCHEMA.ReviewInverseSocketCompacted]: "Review of",
    [vocab.BUDDY.BuddySocketCompacted]: "Buddies",
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: "Articles",
    [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: "Published in",
    [vocab.WXSCHEMA.MemberSocketCompacted]: "Members",
    [vocab.WXSCHEMA.MemberOfSocketCompacted]: "Member Of",
    [vocab.WXSCHEMA.WorksForInverseSocketCompacted]: "Employees",
    [vocab.WXSCHEMA.WorksForSocketCompacted]: "Employers",
    [vocab.WXSCHEMA.SubOrganizationSocketCompacted]: "Sub Organizations",
    [vocab.WXSCHEMA.ParentOrganizationSocketCompacted]: "Parent Organization",
    [vocab.WXVALUEFLOWS.PrimaryAccountableOfSocketCompacted]: "Owns",
    [vocab.WXVALUEFLOWS.PrimaryAccountableSocketCompacted]: "Owned By",
    [vocab.WXVALUEFLOWS.CustodianOfSocketCompacted]: "Controls",
    [vocab.WXVALUEFLOWS.CustodianSocketCompacted]: "Controlled by",
    [vocab.WXVALUEFLOWS.ResourceSocketCompacted]: "Affected Resources",
    [vocab.WXVALUEFLOWS.ResourceActivitySocketCompacted]: "Activities",
    [vocab.WXVALUEFLOWS.ActorSocketCompacted]: "Actor",
    [vocab.WXVALUEFLOWS.ActorActivitySocketCompacted]: "Activities",
    [vocab.WXVALUEFLOWS.PartnerActivitySocketCompacted]: "Connections",
    [vocab.WXSCHEMA.AttendeeInverseSocketCompacted]: "Attends",
    [vocab.WXSCHEMA.AttendeeSocketCompacted]: "Attendees",
    [vocab.WXSCHEMA.EventInverseSocketCompacted]: "Organizers",
    [vocab.WXSCHEMA.EventSocketCompacted]: "Organized Events",
    [vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted]: "Role Of",
    [vocab.WXPERSONA.InterestOfSocketCompacted]: "Interest Of",
    [vocab.WXPERSONA.InterestSocketCompacted]: "Interests",
    [vocab.WXPERSONA.ExpertiseOfSocketCompacted]: "Expertise Of",
    [vocab.WXPERSONA.ExpertiseSocketCompacted]: "Expertises",
    [vocab.PROJECT.ProjectSocketCompacted]: "Projects",
    [vocab.PROJECT.ProjectOfSocketCompacted]: "Project Of",
    [vocab.PROJECT.RelatedProjectSocketCompacted]: "Related Projects",
  },
  socketItem: {
    [vocab.GROUP.GroupSocketCompacted]: "Group Member",
    [vocab.CHAT.ChatSocketCompacted]: "Chat",
    [vocab.HOLD.HoldableSocketCompacted]: "Post",
    [vocab.WXVALUEFLOWS.SupportableSocketCompacted]: "Supporter",
    [vocab.HOLD.HolderSocketCompacted]: "Holder",
    [vocab.WXVALUEFLOWS.SupporterSocketCompacted]: "Supportable",
    [vocab.WXSCHEMA.ReviewSocketCompacted]: "Review",
    [vocab.BUDDY.BuddySocketCompacted]: "Buddy",
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: "Publisher",
    [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: "Article",
    [vocab.WXSCHEMA.MemberSocketCompacted]: "Membership",
    [vocab.WXSCHEMA.MemberOfSocketCompacted]: "Member",
    [vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted]: "Role",
    [vocab.WXSCHEMA.WorksForInverseSocketCompacted]: "Employer",
    [vocab.WXSCHEMA.WorksForSocketCompacted]: "Employee",
    [vocab.WXSCHEMA.SubOrganizationSocketCompacted]: "Parent Organization",
    [vocab.WXSCHEMA.ReviewInverseSocketCompacted]: "Review",
    [vocab.WXSCHEMA.ParentOrganizationSocketCompacted]: "Sub Organization",
    [vocab.WXVALUEFLOWS.PrimaryAccountableOfSocketCompacted]: "Owner",
    [vocab.WXVALUEFLOWS.PrimaryAccountableSocketCompacted]: "Owned Thing",
    [vocab.WXVALUEFLOWS.CustodianOfSocketCompacted]: "Controlled by",
    [vocab.WXVALUEFLOWS.CustodianSocketCompacted]: "Controlled Thing",
    [vocab.WXVALUEFLOWS.ResourceSocketCompacted]: "Activity",
    [vocab.WXVALUEFLOWS.ResourceActivitySocketCompacted]: "Affected Resource",
    [vocab.WXVALUEFLOWS.ActorSocketCompacted]: "Activity",
    [vocab.WXVALUEFLOWS.ActorActivitySocketCompacted]: "Actor",
    [vocab.WXVALUEFLOWS.PartnerActivitySocketCompacted]: "Connection",
    [vocab.WXSCHEMA.EventSocketCompacted]: "Organizer",
    [vocab.WXSCHEMA.EventInverseSocketCompacted]: "Organized Event",
    [vocab.WXSCHEMA.AttendeeInverseSocketCompacted]: "Attendee",
    [vocab.WXSCHEMA.AttendeeSocketCompacted]: "Event",
    [vocab.WXPERSONA.InterestOfSocketCompacted]: "Interest",
    [vocab.WXPERSONA.InterestSocketCompacted]: "Interest",
    [vocab.WXPERSONA.ExpertiseOfSocketCompacted]: "Expertise",
    [vocab.WXPERSONA.ExpertiseSocketCompacted]: "Expertise",
    [vocab.PROJECT.ProjectSocketCompacted]: "Project",
    [vocab.PROJECT.ProjectOfSocketCompacted]: "Organization",
    [vocab.PROJECT.RelatedProjectSocketCompacted]: "Related Project",
  },
  socketItems: {
    [vocab.GROUP.GroupSocketCompacted]: "Group Chat Members",
    [vocab.CHAT.ChatSocketCompacted]: "Chats",
    [vocab.HOLD.HoldableSocketCompacted]: "Holders",
    [vocab.WXVALUEFLOWS.SupportableSocketCompacted]: "Supporters",
    [vocab.HOLD.HolderSocketCompacted]: "Posts",
    [vocab.WXVALUEFLOWS.SupporterSocketCompacted]: "Supportables",
    [vocab.WXSCHEMA.ReviewSocketCompacted]: "Reviews",
    [vocab.BUDDY.BuddySocketCompacted]: "Buddies",
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: "Articles",
    [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: "Publishers",
    [vocab.WXSCHEMA.MemberSocketCompacted]: "Members",
    [vocab.WXSCHEMA.MemberOfSocketCompacted]: "Memberships",
    [vocab.WXSCHEMA.WorksForInverseSocketCompacted]: "Employees",
    [vocab.WXSCHEMA.WorksForSocketCompacted]: "Employers",
    [vocab.WXSCHEMA.SubOrganizationSocketCompacted]: "Sub Organizations",
    [vocab.WXSCHEMA.ParentOrganizationSocketCompacted]: "Parent Organizations",
    [vocab.WXVALUEFLOWS.PrimaryAccountableOfSocketCompacted]: "Owned Things",
    [vocab.WXVALUEFLOWS.PrimaryAccountableSocketCompacted]: "Owners",
    [vocab.WXVALUEFLOWS.CustodianOfSocketCompacted]: "Controlled Things",
    [vocab.WXVALUEFLOWS.CustodianSocketCompacted]: "Controller",
    [vocab.WXVALUEFLOWS.ResourceSocketCompacted]: "Activities",
    [vocab.WXVALUEFLOWS.ResourceActivitySocketCompacted]: "Affected Resources",
    [vocab.WXVALUEFLOWS.ActorSocketCompacted]: "Activities",
    [vocab.WXVALUEFLOWS.ActorActivitySocketCompacted]: "Actors",
    [vocab.WXVALUEFLOWS.PartnerActivitySocketCompacted]: "Connections",
    [vocab.WXSCHEMA.EventSocketCompacted]: "Organized Events",
    [vocab.WXSCHEMA.EventInverseSocketCompacted]: "Organizers",
    [vocab.WXSCHEMA.AttendeeSocketCompacted]: "Attendees",
    [vocab.WXSCHEMA.AttendeeInverseSocketCompacted]: "Events",
    [vocab.WXPERSONA.InterestOfSocketCompacted]: "Interest Of",
    [vocab.WXPERSONA.InterestSocketCompacted]: "Interests",
    [vocab.WXPERSONA.ExpertiseOfSocketCompacted]: "Expertise Of",
    [vocab.WXPERSONA.ExpertiseSocketCompacted]: "Expertises",
    [vocab.PROJECT.ProjectSocketCompacted]: "Projects",
    [vocab.PROJECT.ProjectOfSocketCompacted]: "Organizations",
    [vocab.PROJECT.RelatedProjectSocketCompacted]: "Related Projects",
  },
  feedItem: {
    [vocab.WON.RequestReceived]: {
      default: {
        prefix: "Received Request from",
        postfix: undefined,
      },
    },
    [vocab.WON.RequestSent]: {
      default: {
        prefix: "Sent Request to",
        postfix: undefined,
      },
    },
    [vocab.WON.Connected]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        [vocab.CHAT.ChatSocketCompacted]: {
          prefix: "Started Chat with",
          postfix: undefined,
        },
        [vocab.GROUP.GroupSocketCompacted]: {
          prefix: "Joined Groupchat",
          postfix: undefined,
        },
        default: {
          prefix: "Started Chat with",
          postfix: undefined,
        },
      },
      [vocab.HOLD.HoldableSocketCompacted]: {
        default: {
          prefix: "Added",
          postfix: "to Held Atoms",
        },
      },
      [vocab.PROJECT.ProjectSocketCompacted]: {
        default: {
          prefix: "Added",
          postfix: "to Projects",
        },
      },
      [vocab.PROJECT.RelatedProjectSocketCompacted]: {
        default: {
          prefix: "Added",
          postfix: "to Related Projects",
        },
      },
      [vocab.WXSCHEMA.MemberSocketCompacted]: {
        default: {
          prefix: "Added",
          postfix: "as a Member",
        },
      },
      [vocab.WXSCHEMA.MemberOfSocketCompacted]: {
        default: {
          prefix: "Joined",
          postfix: undefined,
        },
      },
      [vocab.HOLD.HolderSocketCompacted]: {
        default: {
          prefix: "Added",
          postfix: "as Holder",
        },
      },
      [vocab.BUDDY.BuddySocketCompacted]: {
        default: {
          prefix: "Added",
          postfix: "as a Buddy",
        },
      },
      default: {
        prefix: "Added",
        postfix: undefined,
      },
    },
    [vocab.WON.Closed]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        [vocab.CHAT.ChatSocketCompacted]: {
          prefix: "Closed Chat with",
          postfix: undefined,
        },
        [vocab.GROUP.GroupSocketCompacted]: {
          prefix: "Left Groupchat",
          postfix: undefined,
        },
        default: {
          prefix: "Closed Chat with",
          postfix: undefined,
        },
      },
      [vocab.HOLD.HoldableSocketCompacted]: {
        default: {
          prefix: "Removed",
          postfix: "from Held Atoms",
        },
      },
      [vocab.WXSCHEMA.MemberSocketCompacted]: {
        default: {
          prefix: "Removed",
          postfix: "from Members",
        },
      },
      [vocab.WXSCHEMA.MemberOfSocketCompacted]: {
        default: {
          prefix: "Left",
          postfix: undefined,
        },
      },
      [vocab.HOLD.HolderSocketCompacted]: {
        default: {
          prefix: "Removed Holder",
          postfix: undefined,
        },
      },
      [vocab.BUDDY.BuddySocketCompacted]: {
        default: {
          prefix: "Removed",
          postfix: "from Buddies",
        },
      },
      default: {
        prefix: "Removed",
        postfix: undefined,
      },
    },
    [vocab.WON.Suggested]: {
      default: {
        prefix: "Suggestion to add",
        postfix: undefined,
      },
    },
  },
});

export function generateFeedItemLabels(
  senderSocketType,
  targetSocketType,
  targetAtom,
  connectionState
) {
  // const { prefix, postfix } =
  //   // TODO: Currently we do not have any of those thats why we omit fetching them
  //   // getIn(labels.feedItem, [
  //   //   connectionState,
  //   //   senderSocketType,
  //   //   targetSocketType,
  //   // ]) ||
  //   getIn(labels.feedItem, [connectionState, senderSocketType, "default"]) ||
  //   getIn(labels.feedItem, [connectionState, "default"]);

  //********* THIS PART IS JUST FOR DEBUG PURPOSES TO FIND FEEDITEMLABELS THAT DO NOT HAVE A SPECIAL HANDLING YET
  //********* REMOVE THIS PART AND REPLACE WITH THE ABOVE ONCE YOU ARE SURE THAT YOU DONT HAVE ANY SPECIAL CASES
  //********* LEFT TO HANDLE
  let prefix;
  let postfix;

  const labelWithSenderSocket = getIn(labels.feedItem, [
    connectionState,
    senderSocketType,
    "default",
  ]);

  if (labelWithSenderSocket) {
    prefix = labelWithSenderSocket.prefix;
    postfix = labelWithSenderSocket.postfix;
  } else {
    const defaultLabelsForConnState = getIn(labels.feedItem, [
      connectionState,
      "default",
    ]);

    if (defaultLabelsForConnState) {
      console.debug(
        "LABEL MISSING: generateFeedItemLabel defaults for:",
        connectionState,
        senderSocketType,
        targetSocketType
      );
      prefix = defaultLabelsForConnState.prefix;
      postfix = defaultLabelsForConnState.postfix;
    }
  }

  //******************************************************************************

  const targetAtomTypeLabel =
    targetAtom && atomUtils.generateTypeLabel(targetAtom);

  if (prefix) {
    return { prefix: prefix + " " + targetAtomTypeLabel, postfix: postfix };
  } else {
    console.debug(
      "LABEL MISSING: generateFeedItemLabel defaults for:",
      connectionState,
      senderSocketType,
      targetSocketType
    );
    return {
      prefix: targetAtomTypeLabel,
      postfix: senderSocketType + " ---> " + targetSocketType,
    };
  }
}

export function getConnectionStateLabel(connectionState) {
  return labels.connectionState[connectionState] || connectionState;
}

export function getMessageTypeLabel(messageType) {
  return labels.messageType[messageType] || messageType;
}

export function getSocketLabel(socket) {
  return labels.sockets[socket] || socket;
}

export function getSocketHelpText(socket) {
  return labels.helpText[socket];
}

export function getSocketTabLabel(socketType) {
  return labels.socketTabs[socketType] || socketType;
}

export function getSocketItemLabels(targetSocketType, socketTypes) {
  if (!socketTypes) {
    return undefined;
  }

  const socketTypeLabels = [];
  for (const socketType of socketTypes) {
    socketTypeLabels.push(getSocketItemLabel(targetSocketType, socketType));
  }

  return socketTypeLabels.join("/");
}

export function getSocketPickerLabel(
  addToAtom,
  addToSocketType,
  isAddToAtomOwned
) {
  const specificLabels =
    addToAtom &&
    addToSocketType &&
    atomUtils.getReactionLabel(
      addToAtom,
      "picker",
      isAddToAtomOwned,
      addToSocketType
    );

  if (specificLabels) {
    return specificLabels;
  } else {
    return `Pick an Atom to ${
      isAddToAtomOwned ? "add" : "connect"
    } to the ${getSocketTabLabel(addToSocketType)}`;
  }
}

export function getAddNewSocketItemLabel(
  isAddToOwned,
  addToUseCase,
  addToSocketType,
  ucIdentifier,
  socketType,
  atom
) {
  const specificLabels =
    atom &&
    addToSocketType &&
    atomUtils.getReactionLabel(
      atom,
      "addNew",
      isAddToOwned,
      addToSocketType,
      socketType
    );
  if (specificLabels) {
    return specificLabels;
  } else if (addToUseCase === "organization" && ucIdentifier === "persona") {
    return `Join with New Persona`;
  } else {
    if (isAddToOwned) {
      return `New ${getSocketItemLabel(addToSocketType, socketType)}`;
    } else {
      return `New ${getSocketItemLabel(addToSocketType, socketType)}`;
    }
  }
}

export function getSocketItemLabel(targetSocketType, socketType) {
  if (
    (targetSocketType === vocab.CHAT.ChatSocketCompacted ||
      targetSocketType === vocab.GROUP.GroupSocketCompacted) &&
    socketType === vocab.GROUP.GroupSocketCompacted
  ) {
    return "Group";
  } else if (
    targetSocketType === vocab.GROUP.GroupSocketCompacted &&
    (socketType === vocab.CHAT.ChatSocketCompacted ||
      socketType === vocab.GROUP.GroupSocketCompacted)
  ) {
    return "Group Member";
  } else {
    return labels.socketItem[socketType] || socketType;
  }
}

export function getSocketItemsLabel(socketType) {
  return labels.socketItems[socketType] || socketType;
}

/**
 * Used within socket-add-button
 *
 * TODO: generate a meaningful label based on the targetAtom (the atom to connect to) reactions, targetSocketType, and reactions
 * TODO: generate dynamic labels & enum for labels
 * @param targetAtom
 * @param isAtomOwned
 * @param targetSocketType
 * @param senderReactions
 * @returns {string}
 */
export function generateAddButtonLabel(
  targetAtom,
  isAtomOwned,
  targetSocketType,
  senderReactions
) {
  const specificLabels =
    targetAtom &&
    targetSocketType &&
    atomUtils.getReactionLabel(
      targetAtom,
      "default",
      isAtomOwned,
      targetSocketType
    );

  if (specificLabels) {
    return specificLabels;
  } else {
    switch (targetSocketType) {
      case vocab.WXSCHEMA.ReviewSocketCompacted:
        return `Review ${
          atomUtils.getTitle(targetAtom)
            ? atomUtils.getTitle(targetAtom)
            : "Atom"
        }`;
      case vocab.WXSCHEMA.MemberSocketCompacted:
        if (atomUtils.isOrganization(targetAtom)) {
          return isAtomOwned ? "New Member/Role" : "Join Organization";
        }
        break;
      case vocab.BUDDY.BuddySocketCompacted:
        if (atomUtils.isPersona(targetAtom)) {
          return isAtomOwned ? "Buddy" : "Add Buddy";
        }
        break;
      default:
        break;
    }
    return generateDefaultButtonLabel(
      isAtomOwned,
      targetSocketType,
      senderReactions
    );
  }
}

function generateDefaultButtonLabel(
  isAtomOwned,
  targetSocketType,
  senderReactions
) {
  return isAtomOwned
    ? senderReactions
      ? getSocketItemLabels(targetSocketType, senderReactions.keys())
      : "Atom"
    : senderReactions
      ? getSocketItemLabels(targetSocketType, senderReactions.keys())
      : "Atom";
}

export function getSocketActionInfoLabel(
  socketType,
  connectionState,
  targetSocketType
) {
  let infoLabel = "";

  switch (connectionState) {
    case vocab.WON.Connected:
      infoLabel = `${getSocketItemLabel(socketType, targetSocketType)} of`;
      break;
    case vocab.WON.RequestSent:
      infoLabel = `was requested to join ${getSocketItemsLabel(socketType)} of`;
      break;
    case vocab.WON.RequestReceived:
      infoLabel = `requests to join ${getSocketItemsLabel(socketType)} of`;
      break;
    case vocab.WON.Closed:
      infoLabel = `was removed from ${getSocketItemsLabel(socketType)} of`;
      break;
    case vocab.WON.Suggested:
      infoLabel = `suggested for ${getSocketItemsLabel(socketType)} of`;
      break;
    default:
      infoLabel = " <--> ";
      break;
  }

  return infoLabel;
}

/**
 * Both input parameters can be anything that `Date(...)` can
 * parse (incl. other `Date`s, xsd-strings,...)
 *
 * Adapted from ["Javascript timestamp to relative time" at Stackoverflow](http://stackoverflow.com/questions/6108819/javascript-timestamp-to-relative-time-eg-2-seconds-ago-one-week-ago-etc-best)
 *
 * @param now
 * @param timeToCheck
 */
export function relativeTime(now, timeToCheck) {
  if (!now || !timeToCheck) {
    return undefined;
  }

  const now_ = new Date(now);
  const timeToCheck_ = new Date(timeToCheck);
  let elapsed = now_ - timeToCheck_; // in ms

  if (!isValidNumber(elapsed)) {
    // one of two dates was invalid
    return undefined;
  }

  const future = elapsed < 0;
  if (future) {
    elapsed = elapsed * -1;
  }

  const msPerMinute = 60 * 1000;
  const msPerHour = msPerMinute * 60;
  const msPerDay = msPerHour * 24;
  const msPerMonth = msPerDay * 30;
  const msPerYear = msPerDay * 365;

  const labelGen = (msPerUnit, unitName) => {
    const rounded = Math.round(elapsed / msPerUnit);
    return future
      ? "in " + rounded + " " + unitName + (rounded !== 1 ? "s" : "")
      : rounded + " " + unitName + (rounded !== 1 ? "s" : "") + " ago";
  };

  if (elapsed < msPerMinute) {
    return "Just now";
  } else if (elapsed < msPerHour) {
    return labelGen(msPerMinute, "minute");
  } else if (elapsed < msPerDay) {
    return labelGen(msPerHour, "hour");
  } else if (elapsed < msPerMonth) {
    return labelGen(msPerDay, "day");
  } else if (elapsed < msPerYear) {
    return labelGen(msPerMonth, "month");
  } else {
    return labelGen(msPerYear, "year");
  }
}
