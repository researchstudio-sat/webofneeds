import React, { useState } from "react";
import PropTypes from "prop-types";
import { generateLink, get } from "../utils.js";
import WonTitlePicker from "./details/picker/title-picker.jsx";
import * as useCaseUtils from "../usecase-utils.js";

import "~/style/_usecase-picker.scss";
import { Link } from "react-router-dom";

export default function WonUseCasePicker({
  visibleUseCasesByConfig,
  filterBySocketType,
}) {
  const [state, setState] = useState({
    searchText: "",
    searchResultsImm: undefined,
  });

  const showGroupsThreshold = 1;
  const customUseCaseImm = useCaseUtils.getCustomUseCaseImm();
  const visibleUseCaseGroupsImm =
    visibleUseCasesByConfig &&
    useCaseUtils.getVisibleUseCaseGroupsImm(
      showGroupsThreshold,
      visibleUseCasesByConfig,
      filterBySocketType
    );

  const ungroupedUseCasesImm =
    visibleUseCasesByConfig &&
    useCaseUtils.getUnGroupedUseCasesImm(
      showGroupsThreshold,
      visibleUseCasesByConfig,
      filterBySocketType
    );

  function updateSearch({ value }) {
    let searchResultsImm = undefined;

    if (value && value.trim().length > 1) {
      searchResultsImm = useCaseUtils.filterUseCasesBySearchQueryImm(
        value,
        visibleUseCasesByConfig,
        filterBySocketType
      );
    }

    setState({
      searchText: value,
      searchResultsImm: searchResultsImm,
    });
  }

  const generatePickerContent = () => {
    const startFromRoute = (location, selectedUseCase) => {
      const selectedUseCaseIdentifier = get(selectedUseCase, "identifier");

      if (selectedUseCaseIdentifier) {
        return generateLink(location, {
          useCase: selectedUseCaseIdentifier,
        });
      } else {
        console.warn(
          "No identifier found for given usecase, ",
          selectedUseCase
        );
      }
    };

    if (
      state.searchText &&
      state.searchText.trim &&
      state.searchText.trim().length > 1
    ) {
      const searchResultElements = [];
      if (state.searchResultsImm && state.searchResultsImm.size > 0) {
        state.searchResultsImm.map((useCaseImm, ucIdentifier) => {
          const ucLabel = get(useCaseImm, "label");
          const ucIcon = get(useCaseImm, "icon");
          const ucIconJS = ucIcon && ucIcon.toJS();

          searchResultElements.push(
            <Link
              key={ucIdentifier}
              className="ucp__main__searchresult clickable"
              to={location => startFromRoute(location, useCaseImm)}
            >
              {!!ucIconJS && (
                <svg className="ucp__main__searchresult__icon">
                  <use xlinkHref={ucIconJS} href={ucIconJS} />
                </svg>
              )}
              {!!ucLabel && (
                <div className="ucp__main__searchresult__label">{ucLabel}</div>
              )}
            </Link>
          );
        });
      } else {
        const customUseCaseLabel = get(customUseCaseImm, "label");
        const customUseCaseIcon = get(customUseCaseImm, "icon");
        const customUseCaseIconJS =
          customUseCaseIcon && customUseCaseIcon.toJS();

        searchResultElements.push(
          <React.Fragment key={get(customUseCaseImm, "identifier")}>
            <div className="ucp__main__noresults">
              {`No Results found for '${state.searchText}'.`}
            </div>
            {!!customUseCaseImm && (
              <Link
                className="ucp__main__newcustom clickable"
                to={location => startFromRoute(location, customUseCaseImm)}
              >
                {!!customUseCaseIconJS && (
                  <svg className="ucp__main__newcustom__icon">
                    <use
                      xlinkHref={customUseCaseIconJS}
                      href={customUseCaseIconJS}
                    />
                  </svg>
                )}
                {!!customUseCaseLabel && (
                  <div className="ucp__main__newcustom__label">
                    {customUseCaseLabel}
                  </div>
                )}
              </Link>
            )}
          </React.Fragment>
        );
      }
      return searchResultElements;
    } else {
      const useCaseGroupElements = [];
      if (visibleUseCaseGroupsImm) {
        const viewUseCaseGroupRoute = (location, selectedUseCaseGroup) => {
          const selectedGroupIdentifier = get(
            selectedUseCaseGroup,
            "identifier"
          );

          if (selectedGroupIdentifier) {
            return generateLink(location, {
              useCaseGroup: selectedGroupIdentifier,
            });
          } else {
            console.warn(
              "No identifier found for given usecase group, ",
              selectedUseCaseGroup
            );
          }
        };

        visibleUseCaseGroupsImm.map((useCaseGroupImm, ucgIdentifier) => {
          const ucgLabel = get(useCaseGroupImm, "label");
          const ucgIcon = get(useCaseGroupImm, "icon");
          const ucgIconJS = ucgIcon && ucgIcon.toJS();

          useCaseGroupElements.push(
            <Link
              key={ucgIdentifier}
              className="ucp__main__usecase-group clickable"
              to={location => viewUseCaseGroupRoute(location, useCaseGroupImm)}
            >
              {!!ucgIconJS && (
                <svg className="ucp__main__usecase-group__icon">
                  <use xlinkHref={ucgIconJS} href={ucgIconJS} />
                </svg>
              )}
              {!!ucgLabel && (
                <div className="ucp__main__usecase-group__label">
                  {ucgLabel}
                </div>
              )}
            </Link>
          );
        });
      }

      const ungroupedUseCaseElements = [];
      if (ungroupedUseCasesImm) {
        ungroupedUseCasesImm.map((useCaseImm, ucIdentifier) => {
          const ucLabel = get(useCaseImm, "label");
          const ucIcon = get(useCaseImm, "icon");
          const ucIconJS = ucIcon && ucIcon.toJS();

          ungroupedUseCaseElements.push(
            <Link
              key={ucIdentifier}
              className="ucp__main__usecase-group clickable"
              to={location => startFromRoute(location, useCaseImm)}
            >
              {!!ucIconJS && (
                <svg className="ucp__main__usecase-group__icon">
                  <use xlinkHref={ucIconJS} href={ucIconJS} />
                </svg>
              )}
              {!!ucLabel && (
                <div className="ucp__main__usecase-group__label">{ucLabel}</div>
              )}
            </Link>
          );
        });
      }

      return (
        <React.Fragment>
          {/*<!-- USE CASE GROUPS -->*/}
          {useCaseGroupElements}
          {/*<!-- USE CASES WITHOUT GROUPS -->*/}
          {ungroupedUseCaseElements}
        </React.Fragment>
      );
    }
  };

  return (
    <won-usecase-picker>
      <div className="ucp__header">
        <span className="ucp__header__title">Pick one!</span>
      </div>

      <div className="ucp__main">
        <div className="ucp__main__search">
          <WonTitlePicker
            onUpdate={updateSearch}
            initialValue={state.searchText}
            detail={{ placeholder: "Filter Usecases" }}
          />
        </div>
        {generatePickerContent()}
      </div>
    </won-usecase-picker>
  );
}
WonUseCasePicker.propTypes = {
  visibleUseCasesByConfig: PropTypes.object.isRequired,
  filterBySocketType: PropTypes.string,
};
