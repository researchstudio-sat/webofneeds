/**
 * Created by ksinger on 08.10.2015.
 */

import won from './won-es6';
import Immutable from 'immutable';
import { actionTypes, actionCreators } from './actions/actions';

import {
    checkLoginStatus,
} from './won-utils';

import {
    selectAllNeeds,
} from './selectors';

import {
    decodeUriComponentProperly,
    checkHttpStatus,
} from './utils';


/**
 * As we have configured our router to keep parameters unchanged,
 * that aren't mentioned in the `stateGo`-calls, you can use this
 * array to reset them explicitly (or merge anything else on top,
 * by using the `makeParams`-function).
 *
 * NOTE: WHEN INTRODUCING NEW PARAMETERS, ADD THEM HERE
 * AS WELL.
 */
export const resetParams = Object.freeze({
    connectionType: undefined,
    connectionUri: undefined,
    focusSignup: undefined,
    layout: undefined,
    myUri: undefined,
    postUri: undefined,
    // privateId: undefined,  // global parameter that we don't want to lose. never reset this one.
});

/**
 * These should not accidentally be removed from the state. See the `stateGo*`-action creators
 * in `actions.js`
 * @type {string[]}
 */
export const constantParams = [
    'privateId',
]


/**
 * Adapted from https://github.com/neilff/redux-ui-router/blob/master/example/index.js
 * @param $urlRouterProvider
 * @param $stateProvider
 */
export const configRouting = [ '$urlRouterProvider', '$stateProvider', ($urlRouterProvider, $stateProvider) => {
    //$urlRouterProvider.otherwise('/landingpage');
    $urlRouterProvider.otherwise(($injector, $location) => {
        console.log('otherwise ', $injector, $location)

        //let updatedRoute =  $location.replace()
        //    .path('/landingpage') // change route to landingpage
        $location.path('/landingpage') // change route to landingpage

        const origParams = $location.search();
        if(origParams) {
            const onlyConstParams = addConstParams({}, origParams);
            //updatedRoute.search(onlyConstParams); // strip all but "constant" parameters
            $location.search(onlyConstParams);
        }

        //return updatedRoute;
    });

    //make sure create-need is called with a draftId
    //$urlRouterProvider.when('/create-need/', [() => '/create-need/' + getRandomPosInt()]);

    [
        { path: '/landingpage?:focusSignup?privateId', component: 'landingpage' },
        { path: '/create-need/?privateId', component: 'create-need' },
        { path: '/feed?privateId', component: 'feed' },
        { path: '/overview/matches?privateId?layout?myUri?connectionUri', component: 'overview-matches', as: 'overviewMatches' },
        { path: '/overview/incoming-requests?privateId?myUri?connectionUri', component: 'overview-incoming-requests', as: 'overviewIncomingRequests' },
        { path: '/overview/sent-requests?privateId?myUri?connectionUri', component: 'overview-sent-requests', as: 'overviewSentRequests' },
        { path: '/overview/posts?privateId', component: 'overview-posts', as: 'overviewPosts' },
        { path: '/post/?privateId?postUri?connectionUri?connectionType?layout', component: 'post', as: 'post' },

    ].forEach( ({path, component, as}) => {

            const cmlComponent = hyphen2Camel(component);

            if(!path) path = `/${component}`;
            if(!as) as = firstToLowerCase(cmlComponent);

            $stateProvider.state(as, {
                url: path,
                templateUrl: `./app/components/${component}/${component}.html`,
                // template: `<${component}></${component>` TODO use directives instead of view+ctrl
                controller: `${cmlComponent}Controller`,
                controllerAs: 'self',
                scope: {}
            });
        })

    $urlRouterProvider.when('/settings/', '/settings/general');

    $stateProvider
        .state('settings', {
            url: '/settings',
            templateUrl: './app/components/settings/settings.html'
        })
        .state('settings.avatars', {
            url: '/avatars',
            template: `<won-avatar-settings></won-avatar-settings>`
        })
        .state('settings.general', {
            url: '/general',
            template: `<won-general-settings></won-general-settings>`
        })

}]

export const runAccessControl = [ '$rootScope', '$ngRedux', '$urlRouter',
    ($rootScope, $ngRedux, $urlRouter) => {
        $rootScope.$on('$stateChangeStart',
            (event, toState, toParams, fromState, fromParams, options) =>
                accessControl({
                    event, toState, toParams, fromState, fromParams, options,
                    dispatch: $ngRedux.dispatch,
                    getState: $ngRedux.getState,
                })
        );
    }
];


function postViewEnsureLoaded(dispatch, getState, encodedPostUri) {
    console.log('in postViewEnsureLoaded');
    const postUri = decodeUriComponentProperly(encodedPostUri);
    const state = getState();

    if (postUri && !selectAllNeeds(state).has(postUri)) {

        /*
         * got an uri but no post loaded to the state yet ->
         * assuming that if you're logged in you either did a
         * page-reload with a valid session or signed in, thus
         * loading your own needs as part of the `initialPageLoad`
         * or `login` action-creators. Thus we assume
         * you loaded the app in some other view,
         * got a link to a non-owned need and pasted it. Thus
         * the `initiaPageLoad` didn't load this need yet. Also
         * we can be sure it's not your need and load it as `theirNeed`.
         */
        won.getNeedWithConnectionUris(postUri)
            .then(need =>
                dispatch({
                    type: actionTypes.router.accessedNonLoadedPost,
                    payload: Immutable.fromJS({ theirNeed: need })
                })
            )
            .catch(error => {
                console.log(
                    `Failed to load need ${postUri}.`,
                    `Reverting to previous router-state.`,
                    `Error: `, error
                );
                dispatch(
                    actionCreators.router__back()
                );
            });
    }
}

//TODO make into router__back AC
function back(hasPreviousState, $ngRedux) {
    if(hasPreviousState) {
        history.back(); //TODO might break if other
                        // route-changes were caused
                        // while this promise evaluated
    } else {
        $ngRedux.dispatch(
            actionCreators.router__stateGoResetParams('landingpage')
        );

    }

}

export function accessControl({event, toState, toParams, fromState, fromParams, options, dispatch, getState}){
    const hasPreviousState = !!fromState.name;
    const state = getState();

    const errorString = "Tried to access view \"" + (toState && toState.name) + "\" that won't work" +
        "without logging in. Blocking route-change.";

    if(!fromParams['privateId'] && toParams['privateId']) {
        // privateId was added, log in
        dispatch(actionCreators.anonymousLogin(toParams['privateId']));
    } else if(fromParams['privateId'] && !toParams['privateId']) {
        //privateId was removed, log out
        dispatch(actionCreators.logout());
    }


    switch(toState.name) {
        case 'post': //Route the 'post' no matter if you are logged in or not since it is accessible at all times
            postViewEnsureLoaded(
                dispatch,
                getState,
                toParams.postUri
            );
            break;

        case 'landingpage': //Route the 'landingpage' view at all times

            checkLoginStatus()
            .then(() => {//logged in -- re-initiate route-change
                console.log("Admiral Ackbar mentioned that this would be a trap, so we will link you to the feed");
                dispatch(
                    actionCreators.router__stateGoAbs('feed', toParams)
                )
            });
            break;

        case 'createNeed':
            return; // can always access this page.

        default: //FOR ALL OTHER ROUTES
            if ( state.get('initialLoadFinished') ) {
                if(state.getIn(['user', 'loggedIn'])) {
                    return; // logged in. continue route-change as intended.
                } else {
                    //sure to be logged out
                    if(event) {
                        // block any route change that is currently happening
                        event.preventDefault();
                    } else {
                        // this is a check with a route that's already fixed, redirect to landing page instead.
                        dispatch(
                            actionCreators.router__stateGoResetParams('landingpage')
                        )
                    }

                    console.error(errorString);
                }
            } else { // still loading
                return; // no access control while loading. the page-load actions will call `accessControl` once the logged-in status has been checked

                //if(hasPreviousState) {
                //    event && event.preventDefault();
                //    console.log('Not sure about login-status -- ' +
                //        'will reinitialize route-change once it\'s been clarified.');
                //}
                //
                //fetch('rest/users/isSignedIn', {credentials: 'include'})
                //    .then(checkHttpStatus) // will reject if not logged in
                //    .then(() => //logged in -- re-initiate route-change
                //        dispatch(
                //            actionCreators.router__stateGoAbs(toState, toParams)
                //        )
                //    )
                //    .catch(error => {
                //        //now certainly not logged in.
                //        console.error(errorString, error)
                //        if (!hasPreviousState) {
                //            dispatch(
                //                actionCreators.router__stateGoResetParams('landingpage')
                //            );
                //        }
                //    });
            }
    }
}

/**
 * Checks if the current route should be accessible
 * and redirects if not.
 * @param dispatch
 * @param getState
 * @returns {*}
 */
export function checkAccessToCurrentRoute(dispatch, getState) {
    const appState = getState();
    const routingState = appState.getIn(['router','currentState']).toJS();
    const params = appState.getIn(['router','currentParams']).toJS();
    return accessControl({
        toState: routingState,
        fromState: routingState,
        toParams: params,
        fromParams: params,
        dispatch,
        getState,
    });
}



/**
 * Merges any "constant"-parameters (e.g. `privateId`) that are contained in `paramsInState`
 * with the `params` object and returns the result.
 *
 * @param params
 * @param paramsInState
 * @returns {*}
 */
export function addConstParams(params, paramsInState){
    const paramsInStateImm = Immutable.fromJS(paramsInState); // ensure that they're immutable
    const currentConstParams = Immutable.Map(
        constantParams.map(p => [p, paramsInStateImm.get(p)]) // [ [ paramName, paramValue] ]
    );
    return currentConstParams.merge(params).toJS();
}

/**
 * As we have configured our router to keep parameters unchanged,
 * that aren't mentioned in the `stateGo`-calls, you can use this
 * function to reset all parameters not mentioned in the arguments
 * and set those to their values.
 * @param params: object with params that should be placed
 * in the url.
 */
//export function makeParams(params) {
//    const currentParams = getState().getIn(['router', 'currentParams']);
//    const currentConstParams = Immutable.Map(
//        constantParams.map(p => [p, currentParams.get(p)]) // [ [ paramName, paramValue] ]
//    );
//    return Immutable.Map().merge(params).merge(currentConstParams).toJS();
//    //let resetParamsCopy = Object.assign({}, resetParams);
//    //return Object.assign(resetParamsCopy, params);
//}
export const makeParams = undefined
