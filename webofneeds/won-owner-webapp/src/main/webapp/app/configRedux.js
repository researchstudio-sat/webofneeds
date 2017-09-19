/**
 * Created by ksinger on 08.10.2015.
 */

import reducer from './reducers/reducers.js';
import thunk from 'redux-thunk';

import { piwikMiddleware } from './piwik.js';

export default function configRedux($ngReduxProvider) {
    $ngReduxProvider.createStoreWith(reducer, [
        'ngUiRouterMiddleware',
        thunk,
        loggingMiddleware,
        piwikMiddleware,
    ]);
}

const loggingMiddleware = store => next => action => {

    if(window.won && window.won.debugmode) {
        const state = store.getState();
        console.debug('action:  ', action.type,
            action.payload && action.payload.toJS ? action.payload.toJS() : action.payload
        );
        console.debug('changing state from ',
            state && state.toJS ?
                state.toJS() : state);
    }

    const result = next(action);

    if(window.won && window.won.debugmode) {
        const updatedState = store.getState();

        console.debug('changed state to ',
            updatedState && updatedState.toJS ?
                updatedState.toJS() : updatedState);
    }

    return result;
};
