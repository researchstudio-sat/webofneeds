import { deepFreeze, isValidNumber } from "./utils.js";
import vocab from "./service/vocab.js";

export const rdfTextfieldHelpText =
  "Expects valid turtle. " +
  `<${vocab.WONMSG.uriPlaceholder.event}> will ` +
  "be replaced by the uri generated for this message. " +
  "Use it, so your TTL can be found when parsing the messages. " +
  "See `won.defaultTurtlePrefixes` " +
  "for prefixes that will be added automatically. E.g." +
  `\`<${vocab.WONMSG.uriPlaceholder.event}> con:text "hello world!". \``;

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
  flags: {
    [vocab.WONMATCH.NoHintForCounterpartCompacted]: "Invisible",
    [vocab.WONMATCH.NoHintForMeCompacted]: "Silent",
    [vocab.WONMATCH.UsedForTestingCompacted]: "Used For Testing",
  },
  sockets: {
    [vocab.GROUP.GroupSocketCompacted]: "Group Chat enabled",
    [vocab.CHAT.ChatSocketCompacted]: "Chat enabled",
    [vocab.HOLD.HoldableSocketCompacted]: "Holdable",
    [vocab.HOLD.HolderSocketCompacted]: "Holder",
    [vocab.WXSCHEMA.ReviewSocketCompacted]: "Review enabled",
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
  },
  socketTabs: {
    [vocab.GROUP.GroupSocketCompacted]: "Group Members",
    [vocab.CHAT.ChatSocketCompacted]: "Chats",
    [vocab.HOLD.HoldableSocketCompacted]: "Held By",
    [vocab.HOLD.HolderSocketCompacted]: "Posts",
    [vocab.WXSCHEMA.ReviewSocketCompacted]: "Reviews",
    [vocab.WXSCHEMA.ReviewInverseSocketCompacted]: "Review of",
    [vocab.BUDDY.BuddySocketCompacted]: "Buddies",
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: "Articles",
    [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: "Published in",
    [vocab.WXSCHEMA.MemberSocketCompacted]: "Members",
    [vocab.WXSCHEMA.MemberOfSocketCompacted]: "Member Of",
    [vocab.WXSCHEMA.WorksForInverseSocketCompacted]: "Employees",
    [vocab.WXSCHEMA.WorksForSocketCompacted]: "Works For",
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
  },
  socketItem: {
    [vocab.GROUP.GroupSocketCompacted]: "Group Member",
    [vocab.CHAT.ChatSocketCompacted]: "Chat",
    [vocab.HOLD.HoldableSocketCompacted]: "Post",
    [vocab.HOLD.HolderSocketCompacted]: "Holder",
    [vocab.WXSCHEMA.ReviewSocketCompacted]: "Review",
    [vocab.BUDDY.BuddySocketCompacted]: "Buddy",
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: "Publisher",
    [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: "Article",
    [vocab.WXSCHEMA.MemberSocketCompacted]: "Membership",
    [vocab.WXSCHEMA.MemberOfSocketCompacted]: "Member",
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
  },
  socketItems: {
    [vocab.GROUP.GroupSocketCompacted]: "Group Members",
    [vocab.CHAT.ChatSocketCompacted]: "Chats",
    [vocab.HOLD.HoldableSocketCompacted]: "Holders",
    [vocab.HOLD.HolderSocketCompacted]: "Posts",
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
  },
});

export function getConnectionStateLabel(connectionState) {
  return labels.connectionState[connectionState] || connectionState;
}

export function getMessageTypeLabel(messageType) {
  return labels.messageType[messageType] || messageType;
}

export function getFlagLabel(flag) {
  return labels.flags[flag] || flag;
}

export function getSocketLabel(socket) {
  return labels.sockets[socket] || socket;
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
