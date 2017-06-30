/**
 * Created by ksinger on 30.06.2017.
 */

import {
   getIn,
} from './utils';


// ------- INIT  --------------------


let _paq = _paq || [];
window._paq = _paq; // export required for interaction with piwik

/* tracker methods like "setCustomDimension" should be called before "trackPageView" */
_paq.push(['trackPageView']);
_paq.push(['enableLinkTracking']);

const baseUri = "//matchat-tmp.innocraft.cloud/";
_paq.push(['setTrackerUrl', baseUri + 'piwik.php']);
_paq.push(['setSiteId', '2']);
const el = document.createElement('script');
const firstScriptEl = document.getElementsByTagName('script')[0];

el.type = 'text/javascript';
el.async = true;
el.defer = true;
el.src = baseUri + 'piwik.js';
firstScriptEl.parentNode.insertBefore(el, firstScriptEl);

export const piwikQueue = _paq;



// ---------



export const piwikMiddleware = store => next => action => {
    if(action && action.type === '@@reduxUiRouter/$stateChangeSuccess') {

        const urlWithParams = getIn(action, ['payload', 'currentState', 'url']);
        const viewName = getIn(action, ['payload', 'currentState', 'name']);
        console.log('sending action to server: ', viewName, ' --- ', urlWithParams);

        //_paq.push(['trackEvent', 'UiRouter', 'StateChangeSuccess', viewName, urlWithParams]);
        _paq.push(['trackEvent', 'StateChangeSuccess', urlWithParams, viewName]); // TODO something's not working out here. maybe it doesn't push specialchars? the values of the params get lost sometimes. sometimes there's no request.
        _paq.push(['trackEvent', 'Category2', 'Name2', 'Description2', 0.1]);
    }

    return next(action);
};
