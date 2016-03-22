/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from 'reselect';
import Immutable from 'immutable';


//TODO update to reflect simplfied state (drop one 'connections')
const selectConnections = state => state.getIn(['connections']);
const selectEvents = state => state.getIn(['events', 'events']);

export const selectUnreadEventUris = state => state
    .getIn(['events', 'unreadEventUris']);

//TODO the earlier unreadEvents was organised by need-uri!

export const selectUnreadEvents = createSelector(
    selectEvents,
    selectUnreadEventUris,
    (events, unreadEventUris) =>
        unreadEventUris.map(eventUri => events.get(eventUri))
);

//const selectUnreadEvents = state => state.getIn(['events', 'unreadEventUris']);

/**
 * @param {object} state
 * @return {object} events grouped by need.
 *      `unreadEventsByNeed.get(needUri)`, e.g.:
 *      `unreadEventsByNeed.get('http://example.org/won/resource/need/1234')`
 */
export const selectUnreadEventsByNeed = createSelector(
    selectUnreadEvents, selectConnections,
    // group by need, resulting in:  `{ <needUri>: { <cnctUri>: e1, <cnctUri>: e2, ...}, <needUri>: ...}`
    //TODO hasReceiverNeed is not guaranteed to exist.
    (unreadEvents, connections) => unreadEvents.groupBy(e => {
        const connectionUri = e.get('hasReceiver');
        return connections.getIn([connectionUri, 'belongsToNeed']);
    })
);

/**
 * from: state.events.unreadEventUris  of "type" ~Map<connection,latestevent>
 * to: ~Map<receiverneeduri, Map<connection,latestevent>>, e.g.:
 *     { <needUri>: { <eventType> : { <cnctUri>: e1, <cnctUri>: e2, ...}, <eventType> :... }, <needUri>: ...}
 *
 * access events as `const event = groupedEvents.getIn([needUri, eventType, cnctUri])`
 *
 * @param {object} state
 * @return {object} events grouped primarily by need and secondarily by type
 */
export const selectUnreadEventsByNeedAndType = createSelector(
    selectUnreadEventsByNeed,
    eventsGroupedByNeed =>
        // group by event-type
        eventsGroupedByNeed.map(cnctsOfNeed => cnctsOfNeed.groupBy(e => e.get('eventType')))
);

/**
 * @param {object} state
 * @return {object} event counts for each need. access via
 *      `unreadCounts.getIn([needUri, eventType])`, e.g.:
 *      `unreadCounts.getIn(['http://example.org/won/resource/need/1234', won.EVENT.HINT_RECEIVED])`
 */
export const selectUnreadCountsByNeedAndType = createSelector(
    selectUnreadEventsByNeedAndType,
    unreadEventsByNeedAndType =>
        unreadEventsByNeedAndType.map(eventsByType => //looking at single need's events grouped by type
            eventsByType.map(evnts => evnts.size) // looking at specific need and type -> just count now
        )

);

/**
 * @param {object} state
 * @return {object} event counts for each event type. access via
 *      `unreadCountsByType.get(eventType)`, e.g.:
 *      `unreadCountsByType.getIn(won.EVENT.HINT_RECEIVED)`
 */
export const selectUnreadCountsByType = createSelector(
    selectUnreadEvents,
    unreadEvents => unreadEvents
        .groupBy(e => e.get('eventType'))
        .map(eventsOfType => eventsOfType.size)
)


/**
 * selects a map of `connectionUri -> { connection, events, ownNeed, remoteNeed }`
 * - thus: everything a connection has direct references to. Use this selector
 * when you're needing connection-centric data (e.g. for a view with a strong
 * focus on the connection)
 *
 * NOTE: the app-state used to have events and needs stored in this fashion.
 * Thus, this selector is also used to allow older code to use the new
 * state-structure with minimal changes.
 */
export const selectAllByConnections = createSelector(
    state => state, //reselect's createSelector always needs a dependency
    state => state
        .getIn(['connections'])
        .map(connection => allByConnection(connection)(state))
);
const allByConnection = (connection) => (state) => {
    const ownNeedUri = connection.get('belongsToNeed');
    const ownNeed = state.getIn(['needs', 'ownNeeds', ownNeedUri]);

    const remoteNeedUri = connection.get('hasRemoteNeed');
    const remoteNeed = state.getIn(['needs', 'theirNeeds', remoteNeedUri]);

    const events = connection
        .get('hasEvents')
        .map(eventUri => state.getIn(['events', 'events', eventUri]));

    return Immutable.Map({ connection, events, ownNeed, remoteNeed });
};
const allByConnectionUri = (connectionUri)  => {
    const connection = state.getIn(['connections', connectionUri]);
    return allByConnection(connection);
};

//TODO there's certainly more elegant ways to implement this selector than first grouping by connection then by need
export const selectConnectionsByNeed = createSelector(
    selectAllByConnections,
        connections => connections
        .map(cnct => Immutable.fromJS(cnct)) //TODO this is a workaround. atm connections aren't ImmutableJS-objects
        .groupBy(cnct => cnct.getIn(['ownNeed', 'uri']))
);

const selectRouterParams = state => state.getIn(['router', 'currentParams']);

export const selectOpenConnectionUri = createSelector(
    selectRouterParams,
    selectConnections,
    (routerParams, connections) => {
        //de-escaping is lost in transpiling if not done in two steps :|
        const openConversationEscaped = routerParams.get('openConversation');
        const openConversation = decodeURIComponent(openConversationEscaped);

        const myUriEscaped = routerParams.get('myUri');
        const myUri = decodeURIComponent(myUriEscaped);

        const theirUriEscaped = routerParams.get('theirUri');
        const theirUri = decodeURIComponent(theirUriEscaped);

        if(openConversation) {
            return openConversation;
        } else if (myUri && theirUri) {
            /*
             returns undefined when there's no
             connection like that in the state.
             */
            return connections
                .filter(c =>
                    c.get('belongsToNeed') === myUri  &&
                    c.get('hasRemoteNeed') === theirUri
                ).keySeq().first()
        } else {
            return undefined;
        }
    }
);


window.selectAllByConnections4dbg = selectAllByConnections;
window.allByConnection4db = allByConnection;
