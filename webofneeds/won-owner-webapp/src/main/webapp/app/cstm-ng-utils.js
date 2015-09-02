/**
 * Created by ksinger on 02.09.2015.
 */
/**
 * Expects an object with a $scope and/or $element variable (e.g. a controller
 * with those two bound two it) and publishes an event to both of these, yielding
 * a solution that's both angular-1.X- as well as standard-js-conform.
 * @param scopeAndElem
 * @param eventName
 * @param eventData
 */
export function broadcastEvent(scopeAndElem, eventName, eventData) {
    if(scopeAndElem.$scope) {
        scopeAndElem.$scope.$broadcast(eventName, eventData);
    }
    if(scopeAndElem.$element && scopeAndElem.$element[0]) {
        let event = undefined;
        if(eventData) {
            event = new CustomEvent(eventName, {'detail': eventData});
        } else {
            event = new Event(eventName);
        }
        scopeAndElem.$element[0].dispatchEvent( event );

    }
}
