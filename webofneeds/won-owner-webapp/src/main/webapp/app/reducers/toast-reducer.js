import { actionTypes } from '../actions/actions';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import won from '../won-es6';

const initialState = Immutable.fromJS({
});

export default function(allToasts = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.logout:
            return initialState;

        //INFO TOASTS: won.WON.infoToast

        //WARN TOASTS: won.WON.warnToast
        //ERROR TOASTS: won.WON.errorToast
        case actionTypes.needs.failed:
            return pushNewToast(allToasts, "Needlist Retrieval Failed", won.WON.errorToast);

        case actionTypes.messages.closeNeed.failed:
            return pushNewToast(allToasts, "Closing Need failed", won.WON.errorToast);

        case actionTypes.messages.chatMessage.failure:
            return pushNewToast(allToasts, "Sending Chat Message failed", won.WON.errorToast);

        //SPECIFIC TOAST ACTIONS
        case actionTypes.toasts.delete:
            return allToasts.deleteIn([action.payload.get("id")]);

        default:
            return allToasts;
    }
}

function pushNewToast(allToasts, msg, type) {
    var toastType = type;
    if(!toastType){
        toastType = won.WON.infoToast;
    }

    var id="";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    for( var i=0; i < 5; i++ )
        id += possible.charAt(Math.floor(Math.random() * possible.length));

    return allToasts.setIn([id], Immutable.fromJS({id: id, type: toastType, msg: msg}));
}

