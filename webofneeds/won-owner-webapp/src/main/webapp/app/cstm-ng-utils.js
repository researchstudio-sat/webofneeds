/**
 *
 * Created by ksinger on 02.09.2015.
 */

import { zipWith } from "./utils.js";

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

/**
 * Attaches the contents of `attachments` to `target` using the constiable names from `names`
 * @param target the object
 * @param names array of constiable names
 * @param attachments array of objects/values
 */
export function attach(target, names, attachments) {
  const pairs = zipWith(
    (name, attachment) => [name, attachment],
    names,
    attachments
  );
  for (const [name, attachment] of pairs) {
    target[name] = attachment;
  }
}
