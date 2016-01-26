;
import won from './won-es6';

export const labels = Object.freeze({
    type: {
        [won.WON.BasicNeedTypeDemand]: 'I want to have something',
        [won.WON.BasicNeedTypeSupply]: 'I offer something',
        [won.WON.BasicNeedTypeDotogether]: 'I want to do something together',
        [won.WON.BasicNeedTypeCritique]: 'I want to change something',
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
    now = new Date(now);
    previous = new Date(previous);
    const elapsed = now - previous; // in ms

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
        return '< 1 minute ago';
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
