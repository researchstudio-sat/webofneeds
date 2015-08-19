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
                    <a href="#" class="topnav__button">
                        <img src="generated/icon-sprite.svg#WON_ico_header" class="topnav__button__icon">
                        <span class="topnav__page-title">Web of Needs</span>
                    </a>
                </div>
                <div class="topnav__center">
                    <a href="#" class="topnav__button">
                        <img src="generated/icon-sprite.svg#ico36_plus" class="topnav__button__icon">
                        <span class="topnav__button__caption">New Need</span>
                    </a>
                </div>
                <div class="topnav__right">
                    <ul class="topnav__list">
                        <li class="">
                            <a href="#" class="topnav__button">
                                <span class="topnav__button__caption">Groups</span>
                                <img src="generated/icon-sprite.svg#ico16_arrow_down" class="topnav__carret">
                                <img src="generated/icon-sprite.svg#ico36_group" class="topnav__button__icon">
                            </a>
                        </li>
                        <li>
                            <a href="#" class="topnav__button">
                                <span class="topnav__button__caption">Username</span>
                                <img src="generated/icon-sprite.svg#ico16_arrow_down" class="topnav__carret">
                                <img src="generated/icon-sprite.svg#ico36_person" class="topnav__button__icon">
                            </a>
                        </li>
                    </ul>
                </div>
            </nav>


            <nav ng-cloak ng-show="{{true}}" class="main-tab-bar">
                <div class="mtb__left"></div>
                <ul class="mtb__center won-tabs">
                    <li class=""><a href="#">Feed</a></li>
                    <li><a href="#">Posts
                        <span class="won-tabs__unread">5</span>
                    </a></li>
                    <li class="selectedTab"><a href="#">Incoming Requests
                        <span class="won-tabs__unread">5</span>
                    </a></li>
                    <li><a href="#">Matches
                        <span class="won-tabs__unread">18</span>
                    </a></li>
                </ul>
                <div class="mtb__right">
                    <a href="#" class="mtb__searchbtn">
                        <img src="generated/icon-sprite.svg#ico36_search_nomargin" class="mtb__icon">
                    </a>
                </div>
            </nav>


            <br/>


            <nav class="create-need-title" ng-cloak ng-show="{{true}}">
                <div class="cnt__left">
                    <img src="generated/icon-sprite.svg#ico27_close" class="cnt__icon">
                </div>
                <h1 class="cnt__center cnt__title">What is your need?</div>
                <div class="cnt__right"></div>
            </nav>


            <br/>



            <nav class="need-tab-bar" ng-cloak ng-show="{{true}}">
                <div class="ntb__left">
                    <img src="generated/icon-sprite.svg#ico36_backarrow" class="ntb__icon">
                    <img class="ntb__left__image" src="images/someNeedTitlePic.png"></img>
                    <div class="ntb__left__titles">
                        <h1 class="ntb__title">New flat, need furniture [TITLE]</h1>
                        <div class="ntb__left__titles__type">I want to have something [TYPE]</div>
                    </div>
                </div>
                <div class="ntb__center"></div>
                <div class="ntb__right">
                    <!--<div class="ntb__right__settingscontainer">-->
                        <img class=" ntb__right__settings ntb__icon" src="generated/icon-sprite.svg#ico_settings">
                    <!--</div>-->
                    <ul class="won-tabs">
                        <li><a href="#">
                            Messages
                            <span class="won-tabs__unread">5</span>
                        </a></li>
                        <li class="selectedTab"><a href="#">
                            Matches
                            <span class="won-tabs__unread">5</span>
                        </a></li>
                        <li><a href="#">
                             Requests
                            <span class="won-tabs__unread">18</span>
                        </a></li>
                        <li><a href="#">
                             Sent Requests
                            <span class="won-tabs__unread">18</span>
                        </a></li>
                    </ul>
                </div>
            </nav>



            <br/>



            <nav class="settings-tab-bar" ng-cloak ng-show="{{true}}">
                <div class="astb__left">
                    <img src="generated/icon-sprite.svg#ico36_backarrow" class="astb__icon">
                </div>
                <div class="astb__center">
                    <h1 class="astb__title">Account Settings</h1>
                    <ul class="won-tabs">
                        <li class="selectedTab"><a href="#">
                            General Settings
                            <span class="won-tabs__unread">5</span>
                        </a></li>
                        <li><a href="#">
                            Manage Avatars
                            <span class="won-tabs__unread">5</span>
                        </a></li>
                    </ul>
                </div>
                <div class="astb__right">
                </div>
            </nav>


        </header>
        <section id="main" class="contentArea">
            <h1 ng-click="ctrl.fooFun()">Hello, from your lovely app-directive! Foo{{ctrl.foo}}! </h1>
        </section>
        <footer></footer>
        `;

    console.log('updated this file at last');

    function link() {
        console.log(`multiline
        template
        string`)
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
    WonAppCtrl.$inject = ['$http'/*injections as strings here*/];


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
