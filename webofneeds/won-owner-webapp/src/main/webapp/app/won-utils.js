/**
 * Created by ksinger on 11.08.2016.
 */

import Immutable from 'immutable';
import L from './leaflet-bundleable';
import {
    relativeTime,
} from './won-label-utils';
import {
    msStringToDate,
    is,
    arrEq,
} from './utils';
import {
    selectEvents,
    selectAllByConnections,
    selectOpenConnectionUri,
    selectOpenConnection,
} from './selectors';

export function initLeaflet(mapMount) {
    if(!L) {
        throw new Exception("Tried to initialize a leaflet widget while leaflet wasn't loaded.");
    }

    const baseMaps = initLeafletBaseMaps();

    const map = L.map(mapMount,{
        center: [37.44, -42.89], //centered on north-west africa
        zoom: 1, //world-map
        layers: [baseMaps['Detailed default map']], //initially visible layers

    }); //.setView([51.505, -0.09], 13);

    //map.fitWorld() // shows every continent twice :|
    map.fitBounds([[-80, -190],[80, 190]]); // fitWorld without repetition

    L.control.layers(baseMaps).addTo(map);

    // Force it to adapt to actual size
    // for some reason this doesn't happen by default
    // when the map is within a tag.
    // this.map.invalidateSize();
    // ^ doesn't work (needs to be done manually atm);

    return map;
}

export function initLeafletBaseMaps() {
    if(!L) {
        throw new Exception("Tried to initialize leaflet map-sources while leaflet wasn't loaded.");
    }
    const secureOsmSource = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png' // secure osm.org
    const secureOsm = L.tileLayer(secureOsmSource, {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
    });

    const transportSource = 'http://{s}.tile2.opencyclemap.org/transport/{z}/{x}/{y}.png';
    const transport = L.tileLayer(transportSource, {
        attribution: 'Maps &copy; <a href="http://www.thunderforest.com">Thunderforest</a>, Data &copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
    });

    const baseMaps = {
        "Detailed default map": secureOsm,
        "Transport (Insecurely loaded!)": transport,
    };

    return baseMaps;
}

export function selectTimestamp(event, ownNeedUri) {
    /*
     * the "outer" event is from our own event
     * container. The receivedTimestamp there
     * should have been placed by our own node.
     *
     * The exception are events that haven't
     * been confirmed yet. They don't have a
     * received timestamp, as these are optimistic
     * assumptions with only sent timestamps.
     */
    return event.get('hasReceivedTimestamp') || event.get('hasSentTimestamp');
};

export function selectConnectionUris(need) {
    return need
        .getIn(['won:hasConnections', 'rdfs:member'])
        .map(c => c.get('@id'));
}

export function selectEventsOfConnection(state, connectionUri) {
    const eventUris = state.getIn(['connections', connectionUri, 'hasEvents']);
    const eventUrisAndEvents = eventUris &&
        eventUris.map(eventUri => [
            eventUri,
            state.getIn(['events', 'events', eventUri])
        ]);
    return Immutable.Map(eventUrisAndEvents);
}


export function connectionLastUpdatedAt(state, connection) {
    if(!connection) return Immutable.List();
    const events = selectEvents(state);
    const eventUris = connection.get('hasEvents');
    if(!eventUris) return Immutable.List();

    const timestamp = (event) =>
        //msStringToDate(selectTimestamp(event, connectionUri))
        msStringToDate(selectTimestamp(event));

    const timestamps = eventUris
        .map(eventUri => events.get(eventUri))
        .filter(event => event) // filter out events for which we have uris but no data.
        .map(event => timestamp(event));

    const latestTimestamp = timestamps.reduce((t1, t2) =>
        t1 > t2 ? t1 : t2,
        undefined // returned if there's no timestamps
    ); // calculate maximum

    return latestTimestamp;
}

/**
 * Temporary helper function, that selects the "is" or "seeks"-branch
 * depending on which of these two is present. This is a temporary
 * solution that allows using the GUI from before the rework.
 * Ultimately, the GUI should reflect that underlying structure and
 * always display both branches.
 * @param need immutablejs map with a sub-map as `won:seeks` or `won:is`
 * @return the non-null sub-map or an exception if `need` doesn't adhere
 *          to our assumptions about how needs are structured while migrating
 *          the entire owner-application to the is-seeks need-structure.
 */
export function seeksOrIs(need) {
    if(!need) return undefined;
    const seeks = need.get('won:seeks');
    const is = need.get('won:is');

    if(!is && !seeks) {
        throw new Exception(
            'Need ', need.get('@id'), ' doesn\'t contain ' +
            'an `is`- or `seeks`-branch!'
        );
    } else if(is && !seeks) {
        return is;
    } else if (!is && seeks) {
        return seeks;
    } else if(/*is && seeks && */is.get('@id') !== seeks.get('@id')) {
        throw new Exception(
            '`is`- and `seeks`-branch for the need ', need.get('@id'),
            ' are distinct rdf-nodes. The owner-webapp (or this part of it)',
            ' can\'t display such needs yet.'
        );

    /*
     * Due to how the json-ld framing algorithm usually one of `is`
     * or `seeks` only contains the `@id` and the other all of the
     * data. Thus we need to return the bigger one.
     */
    } else if(/* is && seeks && is.@id === seeks.@id && */ seeks.size > is.size) {
        return seeks;
    } else /* is && seeks && is.@id === seeks.@id && seeks.size > is.size */ {
        return is;
    }
}
window.seeksOrIs4dbg = seeksOrIs;

/**
 * Temporary helper function, that infers a legacy need-type (demand/
 * supply/dotogether) depending on the presence of is and/or seeks. This
 * should allow us to use the old GUI with the new is-seeks need-structure.
 * Ultimately, the GUI should reflect that underlying structure and
 * always display both branches and no need-type.
 * @param need immutablejs map with a sub-map as `won:seeks` or `won:is`
 * @return a legacy need-type (demand/supply/dotogether) or an exception
 *          if `need` doesn't adhere to our assumptions about how needs
 *          are structured while migrating the entire owner-application
 *          to the is-seeks need-structure.
 */
export function inferLegacyNeedType(need) {
    if(!need) return undefined;
    const seeks = need.get('won:seeks');
    const is = need.get('won:is');

    if(!is && !seeks) {
        throw new Exception(
            'Need ', need.get('@id'), ' doesn\'t contain ' +
            'an `is`- or `seeks`-branch!'
        );
    } else if(is && !seeks) {
        return won.WON.BasicNeedTypeSupplyCompacted;
    } else if (!is && seeks) {
        return won.WON.BasicNeedTypeDemandCompacted;
    } else if(/*is && seeks && */is.get('@id') !== seeks.get('@id')) {
        throw new Exception(
            '`is`- and `seeks`-branch for the need ', need.get('@id'),
            ' are distinct rdf-nodes. The owner-webapp (or this part of it)',
            ' can\'t display such needs yet.'
        );
    } else /* is && seeks && is.@id === seeks.@id */ {
        return won.WON.BasicNeedTypeDotogetherCompacted;
    }
}

/**
 * Makes sure the select-statement is reevaluated, should
 * one of the watched fields change.
 *
 * example usage:
 * ```
 * reduxSelectDependsOnProperties(['self.needUri', 'self.timestamp'], selectFromState, this)
 * ```
 *
 * @param properties a list of watch expressions
 * @param selectFromState same as $ngRedux.connect
 * @param ctrl the controller to bind the results to. needs to have `$ngRedux` and `$scope` attached.
 * @returns {*}
* @returns a function to unregister the watch
 */
export function reduxSelectDependsOnProperties(properties, selectFromState, ctrl) {
    return ctrl.$scope.$watchGroup(properties, (newVals, oldVals) => {
        if(!arrEq(newVals, oldVals)) {
            const state = ctrl.$ngRedux.getState();
            const stateSlice = selectFromState(state);
            Object.assign(ctrl, stateSlice);
        }
    });
}

/**
 * Connects a component to ng-redux, sets up watches for the
 * properties that `selectFromState` depends on and handles
 * cleanup when the component is destroyed.
 * @param selectFromState
 * @param actionCreators
 * @param properties
 * @param ctrl a controller/component with `$scope` and `$ngRedux` attached
 */
export function connect2Redux(selectFromState, actionCreators, properties, ctrl) {
    const disconnectRdx = ctrl.$ngRedux.connect(selectFromState, actionCreators)(ctrl);
    const disconnectProps = reduxSelectDependsOnProperties(properties, selectFromState, ctrl );
    ctrl.$scope.$on('$destroy', () => {
        disconnectRdx();
        disconnectProps();
    });
}
