/**
 * Created by ksinger on 30.06.2017.
 */

import { piwik } from "config";

// ------- INIT  --------------------

let _paq = _paq || [];
window._paq = _paq; // export required for interaction with piwik

const trackerUrl = piwik.baseUrl + "piwik.php";

/**
 * Use this function to call the piwik API.
 * Before piwik.js loads, this function pushes
 * calls to the `_paq` array, that the tracker reads from
 * when starting up. After that startup it calls the functions
 * directly on the tracker (as the `_paq` array isn't read then anymore)
 *
 * available calls can be found at:
 * https://developer.piwik.org/api-reference/tracking-javascript
 *
 * @param args
 */
function piwikCall(args) {
  const tracker = window.Piwik && window.Piwik.getAsyncTracker(trackerUrl);
  if (
    typeof piwik.baseUrl !== "undefined" &&
    piwik.baseUrl !== "" &&
    piwik.baseUrl !== null
  ) {
    if (tracker) {
      // piwik has loaded, we can properly call functions
      tracker[args[0]].call(tracker, ...args.slice(1));
      //console.debug('piwik.js -- about to call ', args[0], ' with args: ', ...args.slice(1));
    } else {
      // push calls to _paq array, that piwik tracker will execute automatically
      // once it loads.
      _paq.push(args);
    }
  }
}

piwikCall(["setTrackerUrl", trackerUrl]);
piwikCall(["setSiteId", "1"]);

/* tracker methods like "setCustomDimension" should be called before "trackPageView" */
piwikCall(["trackPageView"]); // log that page has loaded
//piwikCall(['enableLinkTracking']);

//_paq.push(['setUserId', 'USER_ID_HERE']);
//_paq.push(['enableHeartBeatTimer']); // track time spent with the page open and in focus in 15s increments
const el = document.createElement("script");
const firstScriptEl = document.getElementsByTagName("script")[0];

el.type = "text/javascript";
el.async = true;
el.defer = true;
el.src = piwik.baseUrl + "piwik.js";

if (
  typeof piwik.baseUrl !== "undefined" &&
  piwik.baseUrl !== "" &&
  piwik.baseUrl !== null
) {
  firstScriptEl.parentNode.insertBefore(el, firstScriptEl);
}

export const piwikQueue = _paq;

// ------- ROUTE-CHANGE-LOGGING  --------------------
/**
 * Single-page url-tracking.
 * Adapted from: https://piwik.org/blog/2017/02/how-to-track-single-page-websites-using-piwik-analytics/
 */
let _currentUrl = window.location.href;
window.addEventListener("hashchange", function() {
  //logUrlChange('' + window.location.hash.substr(1));
  logUrlChange();
});

function logUrlChange() {
  piwikCall(["setReferrerUrl", _currentUrl]);
  _currentUrl = window.location.href;
  piwikCall(["setCustomUrl", window.location.href]);
  piwikCall(["setDocumentTitle", window.document.title]);

  // remove all previously assigned custom variables, requires Piwik 3.0.2
  piwikCall(["deleteCustomVariables", "page"]);
  piwikCall(["setGenerationTimeMs", 0]);
  piwikCall(["trackPageView"]);

  // make Piwik aware of newly added content
  //piwikCall(['MediaAnalytics::scanForMedia', documentOrElement]); // rescans entire document for changes about audio/video. enable when using MediaAnalytics
  //piwikCall(['FormAnalytics::scanForForms', docuemntOrElement]); // enable when using form-analytics
  //piwikCall(['trackContentImpressionsWithinNode', documentOrElement]); // enable when using content-tracking (e.g. ad-views & -clicks)
  //piwikCall(['enableLinkTracking']); //rescan for outgoing- and download-links // TODO causes error
}

// ------- ACTION-LOGGING  --------------------

export const piwikMiddleware = () => next => action => {
  if (!(action && action.type)) return next(action);

  const loggingWhiteList = {
    Connections: [
      // log as category "Connections"
      "connections.sendChatMessage", // log this and following actions
      "messages.connectionMessageReceived",
      "connections.open",
      "messages.processOpenMessage",
      "connections.connect",
      "messages.processConnectMessage",
      //            'messages.hintMessageReceived',
      "connections.close",
    ],
    Needs: ["drafts.publish", "needs.close", "needs.reopen"],
  };

  // check if action.type is in the whitelist
  for (let category of Object.keys(loggingWhiteList)) {
    loggingWhiteList[category].forEach(actionType => {
      if (action.type === actionType) {
        //send HTTP-GET to piwik-server
        piwikCall(["trackEvent", category, actionType, "", 1]);
      }
    });
  }

  return next(action);
};
