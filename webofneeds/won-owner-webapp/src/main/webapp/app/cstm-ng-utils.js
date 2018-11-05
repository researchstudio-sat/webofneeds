/**
 *
 * Created by ksinger on 02.09.2015.
 */

import angular from "angular";

/**
 * Registers an input listener on the ngElement. The
 * callback will be invoked if the user has stopped
 * typing for `doneTypingInterval` milliseconds.
 * @param listenerCallback
 * @param ngElement
 * @param doneTypingInterval
 */
export function doneTypingBufferNg(
  listenerCallback,
  ngElement,
  doneTypingInterval
) {
  let typingTimer;
  ngElement.bind("input", e => {
    if (typingTimer) {
      clearTimeout(typingTimer);
    }
    typingTimer = setTimeout(() => listenerCallback(e), doneTypingInterval);
  });
}

/**
 * a class for caching dom-query results.
 */
export class DomCache {
  constructor($element) {
    this._elementsNg = {};
    this.$element = $element;
  }

  ng(selector) {
    return angular.element(this.dom(selector));
  }
  dom(selector) {
    if (!this._elementsNg[selector]) {
      this._elementsNg[selector] = this.$element[0].querySelector(selector);
    }
    return this._elementsNg[selector];
  }
}

/**
 * usage:
 *
 * ```js
 * classOnComponentRoot("myclass", () => someCheck(), this)
 * ```
 *
 * @param {*} className
 * @param {*} watchFn
 * @param {*} ctrl controller with `$element` and `$watch` attached
 * @returns an unregister function for the created watches
 */
export function classOnComponentRoot(className, watchFn, ctrl) {
  if (!ctrl || !ctrl.$scope || !ctrl.$element) {
    throw new Error(
      "classesOnComponentRoot: got undefined controller " +
        "or one without either $element or $scope\n\n" +
        JSON.stringify(ctrl)
    );
  }
  return ctrl.$scope.$watch(watchFn, newVal => {
    if (newVal) {
      ctrl.$element[0].classList.add(className);
    } else {
      ctrl.$element[0].classList.remove(className);
    }
  });
}
