import won from "./won-es6.js";
import { deepFreeze, isValidNumber } from "./utils.js";

export const labels = deepFreeze({
  type: {
    [won.WON.BasicNeedTypeDemandCompacted]: "Search", //'I want something',
    [won.WON.BasicNeedTypeSupplyCompacted]: "Post", //'I offer something',
    [won.WON.BasicNeedTypeDotogetherCompacted]: "Post + Search", //'I want to do something together',
    //TODO: Find right declaration
    [won.WON.BasicNeedTypeCombinedCompacted]: "Post + Search", //'I want to post and search',
    [won.WON.BasicNeedTypeCritiqueCompacted]: "Post", //'I want to change something',
    [won.WON.BasicNeedTypeWhatsAroundCompacted]: "What's Around",
    [won.WON.BasicNeedTypeWhatsNewCompacted]: "What's New",
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
    [won.WONMSG.hintMessage]: "Hint Message",
    [won.WONMSG.hintFeedbackMessage]: "Hint Feedback Message",
  },
  flags: {
    [won.WON.BasicNeedTypeWhatsAroundCompacted]: "What's Around",
    [won.WON.BasicNeedTypeWhatsNewCompacted]: "What's New",
    [won.WON.NoHintForCounterpartCompacted]: "No Hint For Others",
    [won.WON.NoHintForMeCompacted]: "No Hint For Me",
    [won.WON.UsedForTestingCompacted]: "Used For Testing",
  },
});

/**
 * Both input parameters can be anything that `Date(...)` can
 * parse (incl. other `Date`s, xsd-strings,...)
 *
 * Adapted from ["Javascript timestamp to relative time" at Stackoverflow](http://stackoverflow.com/questions/6108819/javascript-timestamp-to-relative-time-eg-2-seconds-ago-one-week-ago-etc-best)
 *
 * @param now
 * @param previous
 */
export function relativeTime(now, previous) {
  if (!now || !previous) {
    return undefined;
  }

  const now_ = new Date(now);
  const previous_ = new Date(previous);
  const elapsed = now_ - previous_; // in ms

  if (!isValidNumber(elapsed)) {
    // one of two dates was invalid
    return undefined;
  }

  const msPerMinute = 60 * 1000;
  const msPerHour = msPerMinute * 60;
  const msPerDay = msPerHour * 24;
  const msPerMonth = msPerDay * 30;
  const msPerYear = msPerDay * 365;

  const labelGen = (msPerUnit, unitName) => {
    const rounded = Math.round(elapsed / msPerUnit);
    return rounded + " " + unitName + (rounded !== 1 ? "s" : "") + " ago";
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
