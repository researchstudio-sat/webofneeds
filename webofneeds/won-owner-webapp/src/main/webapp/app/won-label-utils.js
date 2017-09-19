;
import won from './won-es6.js';
import {deepFreeze} from './utils.js';

export const labels = deepFreeze({
    type: {
        [won.WON.BasicNeedTypeDemandCompacted]: 'I want something',
        [won.WON.BasicNeedTypeSupplyCompacted]: 'I offer something',
        [won.WON.BasicNeedTypeDotogetherCompacted]: 'I want to do something together',
        [won.WON.BasicNeedTypeCritiqueCompacted]: 'I want to change something',
        [won.WON.BasicNeedTypeWhatsAroundCompacted]: 'What\'s Around',
    },
    connectionState: {
        [won.WON.Suggested]: 'Conversation suggested. You can send a request.',
        [won.WON.RequestSent]: 'Conversation requested by you. Close the connection if no longer interested.',
        [won.WON.RequestReceived]: 'Conversation requested. You can accept or deny this request.',
        [won.WON.Connected]: 'Conversation open. You can exchange messages with your counterpart.',
        [won.WON.Closed]: 'Conversation closed. You can reopen it.',
    }
});

/**
 * Both input parameters can be anything that `Date(...)` can
 * parse (incl. other `Date`s, xsd-strings,...)
 *
 * Adapted from ["Javascript timestamp to relative time" at Stackoverflow](http://stackoverflow.com/questions/6108819/javascript-timestamp-to-relative-time-eg-2-seconds-ago-one-week-ago-etc-best)
 *
 * @param now
 * @param previous
 */
export function relativeTime(now, previous) {
    if(!now || !previous) {
            return undefined;
    }

    now = new Date(now);
    previous = new Date(previous);
    const elapsed = now - previous; // in ms

    if(isNaN(elapsed)) {
        // one of two dates was invalid
        return undefined;
    }


    const msPerMinute = 60 * 1000;
    const msPerHour = msPerMinute * 60;
    const msPerDay = msPerHour * 24;
    const msPerMonth = msPerDay * 30;
    const msPerYear = msPerDay * 365;

    const labelGen = (msPerUnit, unitName) => {
        const rounded = Math.round(elapsed/msPerUnit);
        return rounded + ' ' + unitName + (rounded !== 1 ? 's' : '') + ' ago';
    }

    if (elapsed < msPerMinute) {
        return 'Just now';
    } else if (elapsed < msPerHour) {
        return labelGen(msPerMinute, 'minute');
    } else if (elapsed < msPerDay ) {
        return labelGen(msPerHour, 'hour');
    } else if (elapsed < msPerMonth) {
        return labelGen(msPerDay, 'day');
    } else if (elapsed < msPerYear) {
        return 'approximately ' + labelGen(msPerMonth, 'month');
    } else {
        return 'approximately ' + labelGen(msPerYear, 'year');
    }
}

/**
 * This utility function handles the whole setup of timers and generation of human
 * friendly, relative timestamps. You only need to bind them to your controller
 * (or scope) in the callback function. The callback is called once per minute
 * in accordance with the maximum granularity of our relative timestamps.
 *
 * @param $scope {object} the $scope injected into your controller
 * @param $interval {object} the $interval injected into your controller
 * @param creationDate {Date} the time stamp used as baseline of the relative timestamps
 * @param callback {function} it gets a string with a human readable, relative timestamp that you can
 *                            directly bind to your controller or scope in this callback.
 */
export function updateRelativeTimestamps($scope, $interval, creationDate, callback) {
    const updateTimeStamp = () => callback(relativeTime(Date.now(), creationDate));
    updateTimeStamp(); //initial call for t=0
    const interval = $interval(updateTimeStamp, 60000);
    $scope.$on('$destroy', () => $interval.cancel(interval)); //clean up
}
