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
        { path: '/landingpage', component: 'landingpage' },
        { path: '/create-need/:draftId', component: 'create-need' },
        { path: '/feed', component: 'feed' },
        { path: '/overview/matches', component: 'overview-matches', as: 'overviewMatches' },
        { path: '/overview/incoming-requests', component: 'overview-incoming-requests', as: 'overviewIncomingRequests' },
        { path: '/overview/posts', component: 'overview-posts', as: 'overviewPosts' },
        { path: '/post/:postId/owner/matches', component: 'landingpage', as: 'postMatches' }, //TODO implement view
        { path: '/post/:postId/visitor', component: 'landingpage', as: 'postVisitor' }, //TODO implement view

    ].forEach( ({path, component, as}) => {

            const cmlComponent = hyphen2Camel(component);

            if(!path) path = `/${component}`;
            if(!as) as = firstToLowerCase(cmlComponent);

            $stateProvider.state(as, {
                url: path,
                templateUrl: `./app/components/${component}/${component}.html`,
                // template: `<${component}></${component>` TODO use directives instead of view+ctrl
                controller: `${cmlComponent}Controller`,
                controllerAs: 'self'
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

    $stateProvider
        .state('routerDemo', {
            url: '/router-demo/:demoVar',
            template: `
                <p>demoVar = {{self.state.getIn(['router', 'currentParams', 'demoVar'])}}</p>
                <div>
                    <a ui-sref="routerDemo.childA">~A~</a> |
                    <a ui-sref="routerDemo.childB">~B~</a>
                </div>
                <div ui-view></div>
            `,
            controller: 'DemoController',
            controllerAs: 'self'
        })
        .state('routerDemo.childA', {
            url: '/router-demo/:demoVar/childA',
            template: ` <p>showing child A {{ self.avatars }}</p> `,
        })
        .state('routerDemo.childB', {
            url: '/router-demo/:demoVar/childB',
            template: ` <p>showing the other child (B)</p>`,
        })
}


