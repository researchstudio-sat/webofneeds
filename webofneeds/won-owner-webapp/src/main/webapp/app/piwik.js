/**
 * Created by ksinger on 30.06.2017.
 */

import {
   getIn,
} from './utils';


// ------- INIT  --------------------


let _paq = _paq || [];
window._paq = _paq; // export required for interaction with piwik

const baseUrl = "//matchat-tmp.innocraft.cloud/";
const trackerUrl = baseUrl + 'piwik.php'

/**
 * Use this function to call the piwik API.
 * Before piwik.js loads, this function pushes
 * calls to the `_paq` array, that the tracker reads from
 * when starting up. After that startup it calls the functions
 * directly on the tracker (as the `_paq` array isn't read then anymore)
 * @param args
 */
function piwikCall(args) {
    const tracker = window.Piwik && window.Piwik.getAsyncTracker(trackerUrl);
    if(tracker) {
        // piwik has loaded, we can properly call functions
        tracker[args[0]].call(tracker, ...args.slice(1))
        //console.log('piwik.js -- about to call ', args[0], ' with args: ', ...args.slice(1));
    } else {
        // push calls to _paq array, that piwik tracker will execute automatically
        // once it loads.
        _paq.push(args);
    }
}


/* tracker methods like "setCustomDimension" should be called before "trackPageView" */
piwikCall(['trackPageView']);
piwikCall(['enableLinkTracking']);
piwikCall(['setTrackerUrl', trackerUrl]);
piwikCall(['setSiteId', '2']);



//_paq.push(['setUserId', 'USER_ID_HERE']);
//_paq.push(['enableHeartBeatTimer']); // track time spent with the page open and in focus in 15s increments
const el = document.createElement('script');
const firstScriptEl = document.getElementsByTagName('script')[0];

el.type = 'text/javascript';
el.async = true;
el.defer = true;
el.src = baseUrl + 'piwik.js';
firstScriptEl.parentNode.insertBefore(el, firstScriptEl);


export const piwikQueue = _paq;


// ------- ROUTE-CHANGE-LOGGING  --------------------
/**
 * Single-page url-tracking.
 * Adapted from: https://piwik.org/blog/2017/02/how-to-track-single-page-websites-using-piwik-analytics/
 */
let _currentUrl = window.location.href;
window.addEventListener('hashchange', function() {
    console.log('piwik.js -- onhashchange');
    //logUrlChange('' + window.location.hash.substr(1));
    logUrlChange();
});

function logUrlChange() {
    piwikCall(['setReferrerUrl', _currentUrl]);
    _currentUrl = window.location.href;
    piwikCall(['setCustomUrl', window.location.href]);
    piwikCall(['setDocumentTitle', window.document.title]);

    // remove all previously assigned custom variables, requires Piwik 3.0.2
    piwikCall(['deleteCustomVariables', 'page']);
    piwikCall(['setGenerationTimeMs', 0]);
    piwikCall(['trackPageView']);

    // make Piwik aware of newly added content
    //piwikCall(['MediaAnalytics::scanForMedia', documentOrElement]); // rescans entire document for changes about audio/video. enable when using MediaAnalytics
    //piwikCall(['FormAnalytics::scanForForms', docuemntOrElement]); // enable when using form-analytics
    //piwikCall(['trackContentImpressionsWithinNode', documentOrElement]); // enable when using content-tracking (e.g. ad-views & -clicks)
    //piwikCall(['enableLinkTracking']); //rescan for outgoing- and download-links // TODO causes error
}



// ------- ACTION-LOGGING  --------------------


export const piwikMiddleware = store => next => action => {
    //if(action && action.type === '@@reduxUiRouter/$stateChangeSuccess') {
    //    const urlWithParams = getIn(action, ['payload', 'currentState', 'url']);
    //    const viewName = getIn(action, ['payload', 'currentState', 'name']);
    //    console.log('piwik.js -- sending action to server: ', viewName, ' --- ', urlWithParams);
    //    piwikCall(['trackEvent', 'UiRouter', 'StateChangeSuccess', viewName, urlWithParams]);
    //    piwikCall(['trackEvent', 'StateChangeSuccess', urlWithParams, viewName]); // TODO something's not working out here. maybe it doesn't push specialchars? the values of the params get lost sometimes. sometimes there's no request.
    //    piwikCall(['trackEvent', 'Category2', 'Name2', 'Description2', 0.1]);
    //}

    return next(action);
};
