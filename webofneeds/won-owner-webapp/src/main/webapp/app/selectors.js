/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from 'reselect';
import Immutable from 'immutable';


const selectUnreadEvents = state => state.getIn(['events', 'unreadEventUris']);

/**
 * @param {object} state
 * @return {object} events grouped by need.
 *      `unreadEventsByNeed.get(needUri)`, e.g.:
 *      `unreadEventsByNeed.get('http://example.org/won/resource/need/1234')`
 */
export const selectUnreadEventsByNeed = createSelector(
    selectUnreadEvents,
    // group by need, resulting in:  `{ <needUri>: { <cnctUri>: e1, <cnctUri>: e2, ...}, <needUri>: ...}`
    unreadEvents => unreadEvents.groupBy(e => e.get('hasReceiverNeed'))
)

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



export const selectConnectionsByNeed = createSelector(
    selectAllByConnections,
    connections => connections
        .map(cnct => Immutable.fromJS(cnct)) //TODO this is a workaround. atm connections aren't ImmutableJS-objects
        .groupBy(cnct => cnct.getIn(['ownNeed', 'uri']))
);


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
        .getIn(['connections', 'connections'])
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
    const connection = state.getIn(['connections', 'connections', connectionUri]);
    return allByConnection(connection);
};

window.selectAllByConnections4dbg = selectAllByConnections;
window.allByConnection4db = allByConnection;
