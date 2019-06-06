import won from "./won-es6.js";
import { deepFreeze, isValidNumber } from "./utils.js";

export const labels = deepFreeze({
  type: {
    [won.WON.BasicAtomTypeDemandCompacted]: "Search", //'I want something',
    [won.WON.BasicAtomTypeSupplyCompacted]: "Post", //'I offer something',
    [won.WON.BasicAtomTypeDotogetherCompacted]: "Post + Search", //'I want to do something together',
    //TODO: Find right declaration
    [won.WON.BasicAtomTypeCombinedCompacted]: "Post + Search", //'I want to post and search',
    [won.WON.BasicAtomTypeCritiqueCompacted]: "Post", //'I want to change something',
  },
  connectionState: {
    [won.WON.Suggested]: "Conversation suggested.",
    [won.WON.RequestSent]: "Conversation requested by you.",
    [won.WON.RequestReceived]: "Conversation requested.",
    [won.WON.Connected]: "Conversation open.",
    [won.WON.Closed]: "Conversation closed.",
  },
  messageType: {
    [won.WONMSG.connectMessage]: "Contact Request",
    [won.WONMSG.openMessage]: "Accepted Contact Request",
    [won.WONMSG.closeMessage]: "Close Message",
    [won.WONMSG.connectionMessage]: "Chat Message",
    [won.WONMSG.atomHintMessage]: "Atom Hint Message",
    [won.WONMSG.socketHintMessage]: "Socket Hint Message",
    [won.WONMSG.hintFeedbackMessage]: "Hint Feedback Message",
  },
  flags: {
    [won.WONMATCH.NoHintForCounterpartCompacted]: "Invisible",
    [won.WONMATCH.NoHintForMeCompacted]: "Silent",
    [won.WONMATCH.UsedForTestingCompacted]: "Used For Testing",
  },
  sockets: {
    [won.GROUP.GroupSocketCompacted]: "Group Chat enabled",
    [won.CHAT.ChatSocketCompacted]: "Chat enabled",
    [won.HOLD.HoldableSocketCompacted]: "Holdable",
    [won.HOLD.HolderSocketCompacted]: "Holder",
    [won.REVIEW.ReviewSocketCompacted]: "Review enabled",
    [won.BUDDY.BuddySocketCompacted]: "Buddy",
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
