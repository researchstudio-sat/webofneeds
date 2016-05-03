/**
 * Created by ksinger on 08.10.2015.
 */

/**
 * Adapted from https://github.com/neilff/redux-ui-router/blob/master/example/index.js
 * @param $urlRouterProvider
 * @param $stateProvider
 */

export default function configRouting($urlRouterProvider, $stateProvider) {
    $urlRouterProvider.otherwise('/landingpage');

    [
        { path: '/landingpage?:focusSignup', component: 'landingpage' },
        { path: '/create-need/:draftId', component: 'create-need' },
        { path: '/feed', component: 'feed' },
        { path: '/overview/matches?viewType?myUri?connectionUri', component: 'overview-matches', as: 'overviewMatches' },
        { path: '/overview/incoming-requests?myUri?connectionUri', component: 'overview-incoming-requests', as: 'overviewIncomingRequests' },
        { path: '/overview/sent-requests?myUri?connectionUri', component: 'overview-sent-requests', as: 'overviewSentRequests' },
        { path: '/overview/posts', component: 'overview-posts', as: 'overviewPosts' },
        { path: '/post/?postUri?connectionUri?connectionType', component: 'post', as: 'post' },
        { path: '/post/visitor/info/?myUri?theirUri', component: 'post-visitor', as: 'postVisitor' },
        { path: '/post/visitor/messages/?myUri?theirUri', component: 'post-visitor-msgs', as: 'postVisitorMsgs' },

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

}


