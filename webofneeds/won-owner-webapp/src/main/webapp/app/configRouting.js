/**
 * Created by ksinger on 08.10.2015.
 */

import won from './won-es6';
import Immutable from 'immutable';
import { actionTypes, actionCreators } from './actions/actions';

import {
    selectAllNeeds,
} from './selectors';

import {
    decodeUriComponentProperly,
    checkHttpStatus,
} from './utils';


/**
 * Adapted from https://github.com/neilff/redux-ui-router/blob/master/example/index.js
 * @param $urlRouterProvider
 * @param $stateProvider
 */
export const configRouting = [ '$urlRouterProvider', '$stateProvider', ($urlRouterProvider, $stateProvider) => {
    $urlRouterProvider.otherwise('/landingpage');

    //make sure create-need is called with a draftId
    //$urlRouterProvider.when('/create-need/', [() => '/create-need/' + getRandomPosInt()]);

    [
        { path: '/landingpage?:focusSignup', component: 'landingpage' },
        { path: '/create-need/:draftId', component: 'create-need' },
        { path: '/feed', component: 'feed' },
        { path: '/overview/matches?layout?myUri?connectionUri', component: 'overview-matches', as: 'overviewMatches' },
        { path: '/overview/incoming-requests?myUri?connectionUri', component: 'overview-incoming-requests', as: 'overviewIncomingRequests' },
        { path: '/overview/sent-requests?myUri?connectionUri', component: 'overview-sent-requests', as: 'overviewSentRequests' },
        { path: '/overview/posts', component: 'overview-posts', as: 'overviewPosts' },
        { path: '/post/?postUri?connectionUri?connectionType?layout', component: 'post', as: 'post' },

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
                accessControl(event, toState, toParams, fromState, fromParams, options, $ngRedux)
        );
    }
];


function postViewEnsureLoaded($ngRedux, encodedPostUri) {
    console.log('in postViewEnsureLoaded');
    const postUri = decodeUriComponentProperly(encodedPostUri);
    const state = $ngRedux.getState();

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
                $ngRedux.dispatch({
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
                $ngRedux.dispatch(
                    actionCreators.router__back()
                )
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
            actionCreators.router__stateGo('landingpage')
        );

    }

}

function accessControl(event, toState, toParams, fromState, fromParams, options, $ngRedux){
    const hasPreviousState = !!fromState.name;
    const state = $ngRedux.getState();
    
    const errorString = "Tried to access view \"" + (toState && toState.name) + "\" that won't work" +
        "without logging in. Blocking route-change.";

    switch(toState.name) {
        case 'post': //Route the 'post' no matter if you are logged in or not since it is accessible at all times
            postViewEnsureLoaded(
                $ngRedux,
                toParams.postUri
            );
            break;

        case 'landingpage': //Route the 'landingpage' view at all times
            fetch('rest/users/isSignedIn', {credentials: 'include'})
                .then(checkHttpStatus) // will reject if not logged in
                .then(() => {//logged in -- re-initiate route-change
                    console.log("Admiral Ackbar mentioned that this would be a trap, so we will link you to the feed");
                    $ngRedux.dispatch(
                        actionCreators.router__stateGo('feed', toParams)
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
                    event.preventDefault();
                    console.error(errorString);
                }
            } else {
                if(hasPreviousState) {
                    event.preventDefault();
                    console.log('Not sure about login-status -- ' +
                        'will reinitialize route-change once it\'s been clarified.');
                }

                fetch('rest/users/isSignedIn', {credentials: 'include'})
                    .then(checkHttpStatus) // will reject if not logged in
                    .then(() => //logged in -- re-initiate route-change
                        $ngRedux.dispatch(
                            actionCreators.router__stateGo(toState, toParams)
                        )
                    )
                    .catch(error => {
                        //now certainly not logged in.
                        console.error(errorString, error)
                        if (!hasPreviousState) {
                            $ngRedux.dispatch(
                                actionCreators.router__stateGo('landingpage')
                            );
                        }
                    });
            }
    }
}