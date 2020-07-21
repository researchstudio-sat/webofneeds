import React from "react";
import PropTypes from "prop-types";
import * as useCaseUtils from "../usecase-utils.js";

import "~/style/_usecase-group.scss";
import { getQueryParams, generateLink, get } from "../utils";
import { Link, useHistory } from "react-router-dom";

export default function WonUseCaseGroup({
  visibleUseCasesByConfig,
  filterBySocketType,
}) {
  const history = useHistory();
  const { useCaseGroup } = getQueryParams(history.location);

  const visibleUseCaseGroupImm =
    visibleUseCasesByConfig &&
    useCaseUtils.getUseCaseGroupByIdentifierImm(useCaseGroup);

  const ucgLabel = get(visibleUseCaseGroupImm, "label");
  const ucgIcon = get(visibleUseCaseGroupImm, "icon");
  const ucgIconJS = ucgIcon && ucgIcon.toJS();

  const generateSubItems = () => {
    const subItemElements = [];
    const ucgSubItems = get(visibleUseCaseGroupImm, "subItems");

    if (ucgSubItems) {
      const startFromRoute = (location, subItem) => {
        const subItemIdentifier = get(subItem, "identifier");

        if (subItemIdentifier) {
          return generateLink(
            location,
            useCaseUtils.isUseCaseGroup()
              ? { useCaseGroup: subItemIdentifier }
              : { useCase: subItemIdentifier }
          );
        } else {
          console.warn("No identifier found for given usecase, ", subItem);
        }
      };

      ucgSubItems.map((subItem, subItemIdentifier) => {
        const subItemLabel = get(subItem, "label");
        const subItemIcon = get(subItem, "icon");
        const subItemIconJS = subItemIcon && subItemIcon.toJS();

        if (
          useCaseUtils.isDisplayableItemImm(
            subItem,
            visibleUseCasesByConfig,
            filterBySocketType
          )
        ) {
          subItemElements.push(
            <Link
              key={subItemIdentifier}
              className="ucg__main__usecase clickable"
              to={location => startFromRoute(location, subItem)}
            >
              {!!subItemIconJS && (
                <svg className="ucg__main__usecase__icon">
                  <use xlinkHref={subItemIconJS} href={subItemIconJS} />
                </svg>
              )}
              {!!subItemLabel && (
                <div className="ucg__main__usecase__label">{subItemLabel}</div>
              )}
            </Link>
          );
        }
      });
    }
    return subItemElements;
  };
  return (
    <won-usecase-group>
      {visibleUseCaseGroupImm ? (
        <React.Fragment>
          <div className="ucg__header">
            {ucgIconJS && (
              <svg className="ucg__header__icon">
                <use xlinkHref={ucgIconJS} href={ucgIconJS} />
              </svg>
            )}
            {ucgLabel && <div className="ucg__header__title">{ucgLabel}</div>}
          </div>
          <div className="ucg__main">{generateSubItems()}</div>
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
