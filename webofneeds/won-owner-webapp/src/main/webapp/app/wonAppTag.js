/**
 *
 * Created by ksinger on 07.07.2015.
 */


export default function wonAppTag() {
    let template = '\
        <header>\
            <nav class="topnav">\
                <ul class="topnav__list">\
                    <li class="topnav__list__title">[Web of Needs]</li>\
                    <li>New Need</li>\
                    <li>Groups</li>\
                    <li>Username</li>\
                </ul>\
            </nav>\
            <nav ng-cloak ng-hide="{{true}}">\
                <ul>\
                    <li>Feed</li>\
                    <li>Posts</li>\
                    <li>Incoming Requests</li>\
                    <li>Matches</li>\
                    <li>[Search]</li>\
                </ul>\
            </nav>\
        </header>\
        <section id="main">\
            <h1 ng-click="ctrl.fooFun()">Hello, from your lovely app-directive! Foo{{ctrl.foo}}! </h1>\
        </section>\
        <footer></footer>\
        ';

        //<won-tabs></won-tabs>\
    function link() {
    }

    /*
    function wonAppCtrl() {
        this.foo = 'bar';
        this.fooFun = () => {
            console.log('fooFun called!');
        }
    }
    */
    class WonAppCtrl {
        constructor() {
            this.foo = 'bar';
        }
        fooFun () {
            console.log('fooFun called!');
        }
    }
    WonAppCtrl.$inject = [/*injections as strings here*/];


    let directive = {
        restrict: 'E',
        link: link,
        //http://blog.thoughtram.io/angularjs/2015/01/02/exploring-angular-1.3-bindToController.html
        controllerAs: 'ctrl',
        controller: WonAppCtrl,
        template: template
    }
    return directive
}
