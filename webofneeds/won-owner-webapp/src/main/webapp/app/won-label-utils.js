import { deepFreeze, isValidNumber } from "./utils.js";

export const labels = deepFreeze({
  //FIXME: USE THE CONSTANTS FROM won.js again, but be aware that that might causes a cyclic dependency we need to extract this away from won-es6.js or won.js
  connectionState: {
    ["https://w3id.org/won/core#Suggested"]: "Conversation suggested.",
    ["https://w3id.org/won/core#RequestSent"]: "Conversation requested by you.",
    ["https://w3id.org/won/core#RequestReceived"]: "Conversation requested.",
    ["https://w3id.org/won/core#Connected"]: "Conversation open.",
    ["https://w3id.org/won/core#Closed"]: "Conversation closed.",
  },
  messageType: {
    ["https://w3id.org/won/message#ConnectMessage"]: "Contact Request",
    ["https://w3id.org/won/message#OpenMessage"]: "Accepted Contact Request",
    ["https://w3id.org/won/message#CloseMessage"]: "Close Message",
    ["https://w3id.org/won/message#ConnectionMessage"]: "Chat Message",
    ["https://w3id.org/won/message#AtomHintMessage"]: "Atom Hint Message",
    ["https://w3id.org/won/message#SocketHintMessage"]: "Socket Hint Message",
    ["https://w3id.org/won/message#HintFeedbackMessage"]:
      "Hint Feedback Message",
  },
  flags: {
    ["match:NoHintForCounterpart"]: "Invisible",
    ["match:NoHintForMe"]: "Silent",
    ["match:UsedForTesting"]: "Used For Testing",
  },
  sockets: {
    ["group:GroupSocket"]: "Group Chat enabled",
    ["chat:ChatSocket"]: "Chat enabled",
    ["hold:HoldableSocket"]: "Holdable",
    ["hold:HolderSocket"]: "Holder",
    ["review:ReviewSocket"]: "Review enabled",
    ["buddy:BuddySocket"]: "Buddy",
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
