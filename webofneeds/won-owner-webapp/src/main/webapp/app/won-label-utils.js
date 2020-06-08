import { deepFreeze, isValidNumber } from "./utils.js";
import Immutable from "immutable";
import vocab from "./service/vocab.js";

export const rdfTextfieldHelpText =
  "Expects valid turtle. " +
  `<${vocab.WONMSG.uriPlaceholder.event}> will ` +
  "be replaced by the uri generated for this message. " +
  "Use it, so your TTL can be found when parsing the messages. " +
  "See `won.defaultTurtlePrefixes` " +
  "for prefixes that will be added automatically. E.g." +
  `\`<${vocab.WONMSG.uriPlaceholder.event}> con:text "hello world!". \``;

export const labels = deepFreeze({
  connectionState: {
    [vocab.WON.Suggested]: "Conversation suggested.",
    [vocab.WON.RequestSent]: "Conversation requested by you.",
    [vocab.WON.RequestReceived]: "Conversation requested.",
    [vocab.WON.Connected]: "Conversation open.",
    [vocab.WON.Closed]: "Conversation closed.",
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
    [vocab.REVIEW.ReviewSocketCompacted]: "Review enabled",
    [vocab.BUDDY.BuddySocketCompacted]: "Buddy",
  },
  socketTabs: {
    [vocab.GROUP.GroupSocketCompacted]: "Group Members",
    [vocab.CHAT.ChatSocketCompacted]: "Chats",
    [vocab.HOLD.HoldableSocketCompacted]: "Held By",
    [vocab.HOLD.HolderSocketCompacted]: "Posts",
    [vocab.REVIEW.ReviewSocketCompacted]: "Reviews",
    [vocab.BUDDY.BuddySocketCompacted]: "Buddies",
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: "Articles",
    [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: "Published in",
    [vocab.WXSCHEMA.MemberSocketCompacted]: "Members",
    [vocab.WXSCHEMA.MemberOfSocketCompacted]: "Member Of",
    [vocab.WXSCHEMA.WorksForInverseSocketCompacted]: "Employees",
    [vocab.WXSCHEMA.WorksForSocketCompacted]: "Works For",
  },
  socketItem: {
    [vocab.GROUP.GroupSocketCompacted]: "Group Member",
    [vocab.CHAT.ChatSocketCompacted]: "Chat",
    [vocab.HOLD.HoldableSocketCompacted]: "Holder",
    [vocab.HOLD.HolderSocketCompacted]: "Post",
    [vocab.REVIEW.ReviewSocketCompacted]: "Review",
    [vocab.BUDDY.BuddySocketCompacted]: "Buddy",
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: "Publisher",
    [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: "Article",
    [vocab.WXSCHEMA.MemberSocketCompacted]: "Membership",
    [vocab.WXSCHEMA.MemberOfSocketCompacted]: "Member",
    [vocab.WXSCHEMA.WorksForInverseSocketCompacted]: "Employer",
    [vocab.WXSCHEMA.WorksForSocketCompacted]: "Employee",
  },
});

export function getSocketTabLabel(socketType) {
  return labels.socketTabs[socketType] || socketType;
}

export function getSocketItemLabels(socketTypes) {
  if (!socketTypes) {
    return undefined;
  }

  const socketTypeLabels = [];
  for (const socketType of socketTypes) {
    socketTypeLabels.push(labels.socketItem[socketType] || socketType);
  }

  return socketTypeLabels.join("/");
}

export function getSocketItemLabel(socketType) {
  return labels.socketItem[socketType] || socketType;
}

/**
 * Usage: reactionLabels.[enabled|reaction].[senderSocketType].[targetSocketType]
 *
 * use enabled for enabled use-cases, and reaction for reaction usecases
 */
export const reactionLabels = Immutable.fromJS({
  enabled: {
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.GROUP.GroupSocketCompacted]: "Join the Group Chat",
    },
    [vocab.GROUP.GroupSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: "Add to Group Chat",
      [vocab.GROUP.GroupSocketCompacted]: "Add to Group Chat",
    },
    [vocab.HOLD.HolderSocketCompacted]: {
      [vocab.HOLD.HoldableSocketCompacted]: "Add this Holder",
    },
    [vocab.HOLD.HoldableSocketCompacted]: {
      [vocab.HOLD.HolderSocketCompacted]: "Add to Holder",
    },
  },
  reaction: {
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: "Request to Chat",
      [vocab.GROUP.GroupSocketCompacted]: "Request to Join the Group Chat",
    },
    [vocab.GROUP.GroupSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: "Invite to Group Chat",
      [vocab.GROUP.GroupSocketCompacted]: "Invite to Group Chat",
    },
  },
});

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
