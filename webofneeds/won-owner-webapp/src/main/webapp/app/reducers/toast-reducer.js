import { actionTypes } from '../actions/actions.js';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import won from '../won-es6.js';
import {
    getIn,
    generateIdString,
} from '../utils.js';
import config from '../config.js';

const initialState = Immutable.fromJS({
});

export default function(allToasts = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.toasts.test:
            allToasts = pushNewToast(allToasts, "Error Toast", won.WON.errorToast);
            allToasts = pushNewToast(allToasts, "Warning Toast", won.WON.warnToast);
            allToasts = pushNewToast(allToasts, "Info Toast", won.WON.infoToast);
            return allToasts;


        case actionTypes.logout:
            return initialState;

        case actionTypes.registerFailed:
            var privateId = getIn(action, ['payload', 'privateId']);
            if(privateId) {
                return pushNewToast(
                    'Sorry, something failed when posting/generating a new private-ID (the one in ' +
                    'your url-bar). Copy the text you\'ve written somewhere safe, then log out / remove ' +
                    'the ID, then refresh the page and try posting again.',
                    won.WON.errorToast, {}
                )
            } else {
                return allToasts;
            }

        case actionTypes.loginFailed:
            if(getIn(action, ['payload', 'loginError']) === 'invalid privateId') {
                return pushNewToast(
                    allToasts,
                    'Sorry, we couldn\'t find the private ID (the one in your url-bar). If ' +
                    'you copied this address make sure you <strong>copied everything</strong> and try ' +
                    '<strong>reloading the page</strong>. ' +
                    'If this doesn\'t work you can try ' +
                    '<a href="#">' +
                      'removing it' +
                    '</a> to start fresh.',
                    won.WON.errorToast,
                    {unsafeHtmlEnabled: true}
                )
            } else {
                return allToasts;
            }

        case actionTypes.geoLocationDenied:
            return pushNewToast(allToasts,
                'Sorry, we were unable to create your "What\'s Around"-Post, ' +
                'because you have denied us accesss to your current location. ' +
                'To enable it, reload the page and click on "allow access". If' +
                'you\'ve disabled it permanently you can find instructions here for  ' +
                '<a href="https://support.google.com/chrome/answer/142065">Chrome</a>, ' +
                '<a href="https://www.mozilla.org/en-US/firefox/geolocation/">Firefox</a>, ' +
                '<a href="https://support.apple.com/en-us/HT204690">Safari</a> and ' +
                '<a href="https://privacy.microsoft.com/en-us/windows-10-location-and-privacy">Internet Explorer Edge</a>.',
                won.WON.warnToast,
                {unsafeHtmlEnabled: true}
            );


        case actionTypes.lostConnection:
            return pushNewToast(allToasts, "Lost connection - progress " +
                "can't be saved any more. Make sure your " +
                "internet-connection is working, then click \"Reconnect\"", won.WON.warnToast);
        //INFO TOASTS: won.WON.infoToast

        //WARN TOASTS: won.WON.warnToast
        //ERROR TOASTS: won.WON.errorToast
        case actionTypes.messages.closeNeed.failed:
            return pushNewToast(allToasts, "Failed to close posting", won.WON.errorToast);

        case actionTypes.messages.chatMessage.failure:
            return pushNewToast(allToasts, "Failed to send chat message", won.WON.errorToast);

        case actionTypes.messages.needMessageReceived:
            var title = action.payload.needTitle;
            var message = action.payload.message;
            return pushNewToast(allToasts, "Notification for your posting '" + title + "': "+ message, won.WON.infoToast);

        case actionTypes.needs.closedBySystem:
            title = action.payload.needTitle;
            message = action.payload.message;
            return pushNewToast(allToasts, "Closed your posting '" + title + "'. Cause: "+ message, won.WON.infoToast);

        //SPECIFIC TOAST ACTIONS
        case actionTypes.toasts.delete:
            return allToasts.deleteIn([action.payload.get("id")]);

        default:
            return allToasts;
    }
}

/**
 *
 * @param allToasts
 * @param msg
 * @param type
 * @param options
 *   * unsafeHtmlEnabled: set this to true only if it's really necessary and never
 *   with non-static text (e.g. never use this with toasts that show need-contents
 *   as this would open the possibility for XSS-attacks)
 * @returns {*}
 */
function pushNewToast(allToasts, msg, type, options) {
    const options_ = Object.assign({
        unsafeHtmlEnabled: false,
    }, options);

    let toastType = type;
    if(!toastType){
        toastType = won.WON.infoToast;
    }

    const id = generateIdString(6);
    return allToasts.setIn([id], Immutable.fromJS({
        id: id,
        type: toastType,
        msg: msg,
        unsafeHtmlEnabled: options_.unsafeHtmlEnabled,
    }));
}

