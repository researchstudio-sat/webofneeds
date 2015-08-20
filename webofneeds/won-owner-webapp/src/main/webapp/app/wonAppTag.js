/**
 *
 * Created by ksinger on 07.07.2015.
 */
;
export default function wonAppTag() {
    let template = `
        <header>



            <nav class="topnav">
                <div class="topnav__inner">
                    <div class="topnav__inner__left">
                        <a href="#" class="topnav__button">
                            <img src="generated/icon-sprite.svg#WON_ico_header" class="topnav__button__icon">
                            <span class="topnav__page-title">Web of Needs</span>
                        </a>
                    </div>
                    <div class="topnav__inner__center">
                        <a href="#" class="topnav__button">
                            <img src="generated/icon-sprite.svg#ico36_plus" class="topnav__button__icon">
                            <span class="topnav__button__caption">New Need</span>
                        </a>
                    </div>
                    <div class="topnav__inner__right">
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
                </div>
            </nav>


            <nav ng-cloak ng-show="{{true}}" class="main-tab-bar">
                <div class="mtb__inner">
                    <div class="mtb__inner__left"></div>
                    <ul class="mtb__inner__center mtb__tabs">
                        <li class=""><a href="#">Feed</a></li>
                        <li><a href="#">Posts
                            <span class="mtb__tabs__unread">5</span>
                        </a></li>
                        <li class="mtb__tabs__selected"><a href="#">Incoming Requests
                            <span class="mtb__tabs__unread">5</span>
                        </a></li>
                        <li><a href="#">Matches
                            <span class="mtb__tabs__unread">18</span>
                        </a></li>
                    </ul>
                    <div class="mtb__inner__right">
                        <a href="#" class="mtb__searchbtn">
                            <img src="generated/icon-sprite.svg#ico36_search_nomargin" class="mtb__icon">
                        </a>
                    </div>
                </div>
            </nav>


            <br/>


            <nav class="create-need-title" ng-cloak ng-show="{{true}}">
                <div class="cnt__inner">
                    <div class="cnt__inner__left">
                        <img src="generated/icon-sprite.svg#ico27_close" class="cnt__icon">
                    </div>
                    <h1 class="cnt__inner__center cnt__title">What is your need?</div>
                    <div class="cnt__inner__right"></div>
                </div>
            </nav>


            <br/>



            <nav class="need-tab-bar" ng-cloak ng-show="{{true}}">
                <div class="ntb__inner">
                    <div class="ntb__inner__left">
                        <img src="generated/icon-sprite.svg#ico36_backarrow" class="ntb__icon">
                        <img class="ntb__inner__left__image" src="images/someNeedTitlePic.png"></img>
                        <div class="ntb__inner__left__titles">
                            <h1 class="ntb__title">New flat, need furniture [TITLE]</h1>
                            <div class="ntb__inner__left__titles__type">I want to have something [TYPE]</div>
                        </div>
                    </div>
                    <div class="ntb__inner__center"></div>
                    <div class="ntb__inner__right">
                        <!--<div class="ntb__inner__right__settingscontainer">-->
                            <img class=" ntb__inner__right__settings ntb__icon" src="generated/icon-sprite.svg#ico_settings">
                        <!--</div>-->
                        <ul class="ntb__tabs">
                            <li><a href="#">
                                Messages
                                <span class="ntb__tabs__unread">5</span>
                            </a></li>
                            <li class="ntb__tabs__selected"><a href="#">
                                Matches
                                <span class="ntb__tabs__unread">5</span>
                            </a></li>
                            <li><a href="#">
                                 Requests
                                <span class="ntb__tabs__unread">18</span>
                            </a></li>
                            <li><a href="#">
                                 Sent Requests
                                <span class="ntb__tabs__unread">18</span>
                            </a></li>
                        </ul>
                    </div>
                </div>
            </nav>



            <br/>



            <nav class="settings-tab-bar" ng-cloak ng-show="{{true}}">
                <div class="astb__inner">
                    <div class="astb__inner__left">
                        <img src="generated/icon-sprite.svg#ico36_backarrow" class="astb__icon">
                    </div>
                    <div class="astb__inner__center">
                        <h1 class="astb__title">Account Settings</h1>
                        <ul class="astb__tabs">
                            <li class="astb__tabs__selected"><a href="#">
                                General Settings
                                <span class="astb__tabs__unread">5</span>
                            </a></li>
                            <li><a href="#">
                                Manage Avatars
                                <span class="astb__tabs__unread">5</span>
                            </a></li>
                        </ul>
                    </div>
                    <div class="astb__inner__right">
                    </div>
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
