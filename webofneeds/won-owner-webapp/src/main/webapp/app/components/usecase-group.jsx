import React from "react";
import PropTypes from "prop-types";
import * as useCaseUtils from "../usecase-utils.js";

import "~/style/_usecase-group.scss";
import { getQueryParams, generateLink } from "../utils";
import { Link, useHistory } from "react-router-dom";

export default function WonUseCaseGroup({
  visibleUseCasesByConfig,
  filterBySocketType,
}) {
  const history = useHistory();
  const { useCaseGroup } = getQueryParams(history.location);

  const visibleUseCaseGroup =
    visibleUseCasesByConfig &&
    useCaseUtils.getUseCaseGroupByIdentifier(useCaseGroup);

  function startFromRoute(location, subItem) {
    const subItemIdentifier = subItem && subItem.identifier;

    if (subItemIdentifier) {
      if (useCaseUtils.isUseCaseGroup(subItem)) {
        return generateLink(location, { useCaseGroup: subItemIdentifier });
      } else {
        return generateLink(location, { useCase: subItemIdentifier });
      }
    } else {
      console.warn("No identifier found for given usecase, ", subItem);
    }
  }

  return (
    <won-usecase-group>
      {visibleUseCaseGroup ? (
        <React.Fragment>
          <div className="ucg__header">
            {visibleUseCaseGroup.icon && (
              <svg className="ucg__header__icon">
                <use
                  xlinkHref={visibleUseCaseGroup.icon}
                  href={visibleUseCaseGroup.icon}
                />
              </svg>
            )}
            {visibleUseCaseGroup.label && (
              <div className="ucg__header__title">
                {visibleUseCaseGroup.label}
              </div>
            )}
          </div>
          <div className="ucg__main">
            {!!visibleUseCaseGroup.subItems &&
              Object.values(visibleUseCaseGroup.subItems).map(
                (subItem, index) =>
                  useCaseUtils.isDisplayableItem(
                    subItem,
                    visibleUseCasesByConfig,
                    filterBySocketType
                  ) ? (
                    <Link
                      key={subItem.identifier + "-" + index}
                      className="ucg__main__usecase clickable"
                      to={location => startFromRoute(location, subItem)}
                    >
                      {!!subItem.icon && (
                        <svg className="ucg__main__usecase__icon">
                          <use xlinkHref={subItem.icon} href={subItem.icon} />
                        </svg>
                      )}
                      {!!subItem.label && (
                        <div className="ucg__main__usecase__label">
                          {subItem.label}
                        </div>
                      )}
                    </Link>
                  ) : (
                    undefined
                  )
              )}
          </div>
        </React.Fragment>
      ) : (
        <div className="ucg__header">
          <div className="ucg__header__title">Group not found</div>
        </div>
      )}
    </won-usecase-group>
  );
}
WonUseCaseGroup.propTypes = {
  visibleUseCasesByConfig: PropTypes.object.isRequired,
  filterBySocketType: PropTypes.string,
};
