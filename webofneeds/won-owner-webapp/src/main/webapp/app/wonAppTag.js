/**
 *
 * Created by ksinger on 07.07.2015.
 */
;
export default function wonAppTag() {
    let template = `
        <header>
            <nav class="topnav">
                <div class="topnav__left">
                    <a href="#">[Logo] Web of Needs</a>
                </div>
                <div class="topnav__center">
                    <a href="#">New Need</a>
                </div>
                <div class="topnav__right">
                    <ul class="topnav__list">
                        <li class="">
                            <a href="#">Groups</a>
                        </li>
                        <li><a href="#">Username</a></li>
                    </ul>
                </div>
            </nav>
            <nav ng-cloak ng-hide="{{true}}">
                <ul>
                    <li><a href="#">Feed</a></li>
                    <li><a href="#">Posts</a></li>
                    <li><a href="#">Incoming Requests</a></li>
                    <li><a href="#">Matches</a></li>
                    <li><a href="#">[Search]</a></li>
                </ul>
            </nav>
        </header>
        <section id="main">
            <h1 ng-click="ctrl.fooFun()">Hello, from your lovely app-directive! Foo{{ctrl.foo}}! </h1>
        </section>
        <footer></footer>
        `;

        //<won-tabs></won-tabs>\
    function link() {
        console.log(`foo
        bar`)
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
