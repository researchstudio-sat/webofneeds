/**
 * Created by ksinger on 08.10.2015.
 */

import reducer from './reducers/reducers';
import thunk from 'redux-thunk';

export default function configRedux($ngReduxProvider) {
    const loggingReducer = (state, action) => {
        console.log('action:  ', action);
        console.log('changing state from ',
            state && state.toJS?
                state.toJS() : state);
        const updatedState = reducer(state, action);
        console.log('changed state to ',
            updatedState && updatedState.toJS?
                updatedState.toJS() : updatedState);
        return updatedState;
    }
    $ngReduxProvider.createStoreWith(loggingReducer, ['ngUiRouterMiddleware', thunk,/* middlewares here, e.g. 'promiseMiddleware', loggingMiddleware */]);
}
