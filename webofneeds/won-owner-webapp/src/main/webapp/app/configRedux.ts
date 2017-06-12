/**
 * Created by ksinger on 08.10.2015.
 */

import reducer from './reducers/reducers';
import thunk from 'redux-thunk';

export default function configRedux($ngReduxProvider) {
    const loggingReducer = (state, action) => {
        console.debug('action:  ', action.type,
            action.payload && action.payload.toJS? action.payload.toJS() : action.payload
        );
        console.debug('changing state from ',
            state && state.toJS?
                state.toJS() : state);
        const updatedState = reducer(state, action);
        console.debug('changed state to ',
            updatedState && updatedState.toJS?
                updatedState.toJS() : updatedState);
        return updatedState;
    }
    $ngReduxProvider.createStoreWith(loggingReducer, ['ngUiRouterMiddleware', thunk,/* middlewares here, e.g. 'promiseMiddleware', loggingMiddleware */]);
}
