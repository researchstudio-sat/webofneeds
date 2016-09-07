;

import angular from 'angular';
import Immutable from 'immutable';
import squareImageModule from './square-image';
import chatTextFieldModule from './chat-textfield';
import {
    attach,
    is,
    delay,
    msStringToDate,
    urisToLookupMap,
} from '../utils.js'
import { actionCreators }  from '../actions/actions';
import {
    labels,
    relativeTime,
} from '../won-label-utils';
import {
    selectAllByConnections,
    selectOpenConnectionUri,
    selectOpenConnection,
} from '../selectors';
import { selectTimestamp } from '../won-utils'

const serviceDependencies = ['$ngRedux', '$scope', '$element'];

function genComponentConf() {
    let template = `
        <div class="pm__header">
            <a ui-sref="post({connectionUri : null})">
                <img class="pm__header__icon clickable"
                     src="generated/icon-sprite.svg#ico36_close"/>
            </a>
            <div class="pm__header__title">
                Conversation about "{{ self.connectionData.getIn(['remoteNeed', 'won:hasContent', 'dc:title']) }}"
            </div>
            <div class="pm__header__options">
                Options
            </div>
            <img
                class="pm__header__options__icon clickable"
                src="generated/icon-sprite.svg#ico_settings"
                ng-click="self.openConversationOption()"/>
        </div>
        <div class="pm__content">
            <img src="images/spinner/on_white.gif"
                alt="Loading&hellip;"
                ng-show="self.eventsPending"
                class="hspinner"/>
            <div
                class="pm__content__message"
                ng-repeat="message in self.chatMessages"
                ng-class="message.get('hasSenderNeed') == self.connectionData.getIn(['ownNeed', '@id']) ? 'right' : 'left'">
                    <won-square-image
                        title="self.connectionData.getIn(['remoteNeed', 'won:hasContent', 'dc:title'])"
                        src="self.connectionData.getIn(['remoteNeed', 'titleImgSrc'])"
                        uri="self.connectionData.getIn(['remoteNeed', '@id'])"
                        ng-show="message.get('hasSenderNeed') != self.connectionData.getIn(['ownNeed', '@id'])">
                    </won-square-image>
                    <div class="pm__content__message__content">
                        <div class="pm__content__message__content__text">
                            {{ message.get('hasTextMessage') }}
                        </div>
                        <div
                            ng-show="message.unconfirmed"
                            class="pm__content__message__content__time">
                                Pending&nbsp;&hellip;
                        </div>
                        <div
                            ng-hide="message.unconfirmed"
                            class="pm__content__message__content__time">
                                {{ message.get('humanReadableTimestamp') }}
                        </div>
                    </div>
            </div>
        </div>
        <chat-textfield
            class="pm__footer"
            placeholder="::'Your Message'"
            on-input="::self.input(value)"
            on-submit="::self.send()"
            submit-button-label="::'Send'"
            >
        </chat-textfield>
    `;



    class Controller {
        constructor(/* arguments = dependency injections */) {
            attach(this, serviceDependencies, arguments);
            window.pm4dbg = this;
            window.selectOpenConnectionUri4dbg = selectOpenConnectionUri;
            window.selectChatMessages4dbg = selectChatMessages;

            const self = this;

            this.scrollContainerNg().bind('scroll', e => this.onScroll(e));



            // TODO <HACK>

            const loadStuff = () => {
                /*
                 * TODO don't trigger this more than once! When the
                 * events are there, there's no need to dispatch an
                 * action again -- unless paging is involved of course.
                 *
                 * Check during every select?
                 */

                if(self.eventsPending || self.eventsLoaded) return; // only start loading once.

                const state = self.$ngRedux.getState();
                const connectionUri = selectOpenConnectionUri(state);
                const connection = selectOpenConnection(state);

                if(!connectionUri || !connection) return;

                console.log('post-messages.js: testing for selective loading. ', connectionUri, connection);
                console.log('post-messages.js: calling crawlable query soon. ');
                //TODO determine first if component is actually visible (angular calls the constructor long before that)

                //TODO only do self if the events aren't defined!
                //requiringData AC
                self.eventsPending = true; // TODO should be determined in select
                const requesterWebId = connection.get('belongsToNeed');


                const deletme = () =>
                    store4dbg.node(connection.get('hasEventContainer'), (s,res) => {
                        console.log('store4dbg.node: ', s, res);
                        res.triples.forEach(t => console.log('store4dbg.node.triple: ', t.subject.nominalValue, ' -- ', t.predicate.nominalValue, ' -- ', t.object.nominalValue));
                    })
                won.getNode(connection.get('hasEventContainer'), { requesterWebId, pagingSize: 5, deep: true})

                .then(eventContainer => {
                        console.log('store4dbg.eventContainer: ', eventContainer)
                        deletme();
                }) //TODO deletme; for debug
                /*
                .then(eventContainer =>
                    urisToLookupMap(
                        eventContainer.member,
                        uri => won.getNode(uri, { requesterWebId })
                    )
                )
                //.then(events => {}))
                //won.getEventsOfConnection(connectionUri, { requesterWebId: connection.get('belongsToNeed'), pagingSize: 2 })
                .then(events => {
                    self.eventsPending = false; // TODO should be determined in select
                    self.eventsLoaded = true;
                    self.$ngRedux.dispatch({
                        type: 'requiredData',
                        payload: {
                            events: Immutable.fromJS(events)
                        }
                    });

                });
                */
            }

            /*
             * If component has been created
             * after the connection had been loaded
             * we can already start loading events.
             */
            loadStuff();

            //TODO delete unnecessary logging
            //TODO call this when view is visible and the connection has been loaded (sometimes
            // the view is faster, sometimes the connection)
            console.log('post-messages.js: executing constructor.');
            /*
             * for the case that the component
             * is visible before the connection
             * has been loaded: set up a watch.
             */
            const eventWatchDeregister = this.$scope.$watch(
                ({self}) => self.connection && self.connection.get('uri'),
                (newCnctUri, oldCnctUri) => {
                    console.log('post-messages.js: in connection watch', newCnctUri, oldCnctUri);
                    loadStuff();
                });


            // the caching mechanisms should de-dupe the requests.

            // TODO pro-active loading!!!

            // check in every select?
            /*
            use this approach if there's no flags or we need to check
            outside of any component:
            ```
            const ensureLoaded = () => {
                const state = self.$ngRedux.getState();
                const connection = selectOpenConnection(state);
                // pulled the eventsPending check to here, to avoid the expensive subset calculation
                if(!self.eventsPending && !self.eventsLoaded && connection) {
                    var desiredEvents = connection.get('hasEvents');
                    var loadedEvents = state.getIn(['events','events']).keySeq();
                    if(!desiredEvents.isSubset(loadedEvents)) {
                        loadStuff();
                    }
                }
            }
            ```
            */
            // TODO </HACK>

            //this.postmsg = this;
            const selectFromState = state => {

                //TODO seems like rather bad practice to have sideffects here
                //scroll to bottom directly after rendering, if snapped
                delay(0).then(() => {
                    self.updateScrollposition();
                });

                const connectionUri = selectOpenConnectionUri(state);
                const chatMessages = selectChatMessages(state);
                return {
                    connectionUri,
                    connection: selectOpenConnection(state),
                    lastUpdateTime: state.get('lastUpdateTime'),
                    connectionData: selectAllByConnections(state).get(connectionUri),
                    chatMessages: chatMessages && chatMessages.toArray(), //toArray needed as ng-repeat won't work otherwise :|
                    state4dbg: state,
                }
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

            this.snapToBottom();
        }

        snapToBottom() {
            this._snapBottom = true;
            this.scrollToBottom();
        }
        unsnapFromBottom() {
            this._snapBottom = false;
        }
        updateScrollposition() {
            if(this._snapBottom) {
                this.scrollToBottom();
            }
        }
        scrollToBottom() {
            this._programmaticallyScrolling = true;

            this.scrollContainer().scrollTop = this.scrollContainer().scrollHeight;
        }
        onScroll(e) {
            if(!this._programmaticallyScrolling) {
                //only unsnap if the user scrolled themselves
                this.unsnapFromBottom();
            }

            const sc = this.scrollContainer();
            const isAtBottom = sc.scrollTop + sc.offsetHeight >= sc.scrollHeight;
            if(isAtBottom) {
                this.snapToBottom();
            }

            this._programmaticallyScrolling = false
        }
        scrollContainerNg() {
            if(!this._scrollContainer) {
                this._scrollContainer = this.$element.find('.pm__content');
            }
            return this._scrollContainer;
        }
        scrollContainer() {
            return this.scrollContainerNg()[0];
        }

        input(userInput) {
            this.chatMessage = userInput;
        }

        send() {
            const trimmedMsg = this.chatMessage.trim();
            const connectionUri = this.connectionData.getIn(['connection', 'uri']);
            if(trimmedMsg) {
               this.connections__sendChatMessage(trimmedMsg, connectionUri);
            }
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: { },
        template: template,
    }
}

export default angular.module('won.owner.components.postMessages', [
    squareImageModule,
    chatTextFieldModule,
])
    .directive('wonPostMessages', genComponentConf)
    .name;


//TODO refactor so that it always returns an array of immutable messages to
// allow ng-repeat without giving up the cheaper digestion
function selectChatMessages(state) {
    const connectionUri = selectOpenConnectionUri(state);
    const connectionData = selectAllByConnections(state).get(connectionUri);
    const ownNeedUri = connectionData && connectionData.getIn(['ownNeed', '@id']);

    if (!connectionData || !connectionData.get('events')) {
        return Immutable.List();

    } else {
        const timestamp = (event) =>
            //msStringToDate(selectTimestamp(event, connectionUri))
            msStringToDate(selectTimestamp(event))

        const chatMessages = connectionData.get('events')

            /* filter for valid chat messages */
            .filter(event => {
                if (event.get('hasTextMessage')) return true;
                else {
                    let remote = event.get('hasCorrespondingRemoteMessage');
                    if(is('String', remote)) {
                        remote = state.getIn(['events', 'events', remote]);
                    }
                    return remote && remote.get('hasTextMessage');
                }
            }).map(event => {
                const remote = event.get('hasCorrespondingRemoteMessage');
                if (event.get('hasTextMessage'))
                    return event;
                else
                    return remote;
            })

            /* sort them so the latest get shown last */
            .sort((event1, event2) =>
                timestamp(event1) - timestamp(event2)
            )
            /*
             * sort so the latest, optimistic/unconfirmed
             * messages are always at the bottom.
             */
            .sort((event1, event2) => {
                const u1 = event1.get('unconfirmed');
                const u2 = event2.get('unconfirmed');

                if(u1 && !u2) {
                  return 1;
                }
                else if (!u1 && u2) {
                    return -1;
                }
                else {
                    return 0;
                }
            })

            /* add a nice relative timestamp */
            .map(event => event.set(
                    'humanReadableTimestamp',
                    relativeTime(
                        state.get('lastUpdateTime'),
                        timestamp(event)
                    )
                )
            );

        return chatMessages;
    }

}
