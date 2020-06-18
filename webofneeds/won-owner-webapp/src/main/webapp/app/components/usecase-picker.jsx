import React, { useState } from "react";
import PropTypes from "prop-types";
import { generateLink } from "../utils.js";
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
    searchResults: [],
  });

  const showGroupsThreshold = 1;
  const customUseCase = useCaseUtils.getCustomUseCase();
  const visibleUseCaseGroups =
    visibleUseCasesByConfig &&
    useCaseUtils.getVisibleUseCaseGroups(
      showGroupsThreshold,
      visibleUseCasesByConfig,
      filterBySocketType
    );
  const ungroupedUseCases =
    visibleUseCasesByConfig &&
    useCaseUtils.getUnGroupedUseCases(
      showGroupsThreshold,
      visibleUseCasesByConfig,
      filterBySocketType
    );

  function startFromRoute(location, selectedUseCase) {
    const selectedUseCaseIdentifier =
      selectedUseCase && selectedUseCase.identifier;

    if (selectedUseCaseIdentifier) {
      return generateLink(location, { useCase: selectedUseCaseIdentifier });
    } else {
      console.warn("No identifier found for given usecase, ", selectedUseCase);
    }
  }

  function viewUseCaseGroupRoute(location, selectedUseCaseGroup) {
    const selectedGroupIdentifier =
      selectedUseCaseGroup && selectedUseCaseGroup.identifier;

    if (selectedGroupIdentifier) {
      return generateLink(location, { useCaseGroup: selectedGroupIdentifier });
    } else {
      console.warn(
        "No identifier found for given usecase group, ",
        selectedUseCaseGroup
      );
    }
  }

  function updateSearch({ value }) {
    let searchResults = [];

    if (value && value.trim().length > 1) {
      searchResults = useCaseUtils.filterUseCasesBySearchQuery(
        value,
        visibleUseCasesByConfig,
        filterBySocketType
      );

      if (searchResults) {
        const sortByLabelAsc = (a, b) => {
          const bValue = b && b.label && b.label.toLowerCase();
          const aValue = a && a.label && a.label.toLowerCase();

          if (aValue < bValue) return -1;
          if (aValue > bValue) return 1;
          return 0;
        };

        searchResults = searchResults.sort(sortByLabelAsc);
      } else {
        searchResults = [];
      }
    }

    setState({
      searchText: value,
      searchResults: searchResults,
    });
  }

  return (
    <won-usecase-picker>
      {/*}<!-- HEADER -->*/}
      <div className="ucp__header">
        <span className="ucp__header__title">Pick one!</span>
      </div>

      <div className="ucp__main">
        {/*<!-- SEARCH FIELD -->*/}
        <div className="ucp__main__search">
          <WonTitlePicker
            onUpdate={updateSearch}
            initialValue={state.searchText}
            detail={{ placeholder: "Filter Usecases" }}
          />
        </div>
        {/*<!-- SEARCH RESULTS -->*/}
        {state.searchText &&
        state.searchText.trim &&
        state.searchText.trim().length > 1 ? (
          state.searchResults.length > 0 ? (
            state.searchResults.map((useCase, index) => (
              <Link
                key={useCase.identifier + "-" + index}
                className="ucp__main__searchresult clickable"
                to={location => startFromRoute(location, useCase)}
              >
                {!!useCase.icon && (
                  <svg className="ucp__main__searchresult__icon">
                    <use xlinkHref={useCase.icon} href={useCase.icon} />
                  </svg>
                )}
                {!!useCase.label && (
                  <div className="ucp__main__searchresult__label">
                    {useCase.label}
                  </div>
                )}
              </Link>
            ))
          ) : (
            <React.Fragment>
              <div className="ucp__main__noresults">
                {"No Results found for '" + state.searchText + "'."}
              </div>
              {!!customUseCase && (
                <Link
                  className="ucp__main__newcustom clickable"
                  to={location => startFromRoute(location, customUseCase)}
                >
                  {!!customUseCase.icon && (
                    <svg className="ucp__main__newcustom__icon">
                      <use
                        xlinkHref={customUseCase.icon}
                        href={customUseCase.icon}
                      />
                    </svg>
                  )}
                  {!!customUseCase.label && (
                    <div className="ucp__main__newcustom__label">
                      {customUseCase.label}
                    </div>
                  )}
                </Link>
              )}
            </React.Fragment>
          )
        ) : (
          <React.Fragment>
            {/*<!-- USE CASE GROUPS -->*/}
            {!!visibleUseCaseGroups &&
              Object.values(visibleUseCaseGroups).map((ucg, index) => (
                <Link
                  key={ucg.identifier + "-" + index}
                  className="ucp__main__usecase-group clickable"
                  to={location => viewUseCaseGroupRoute(location, ucg)}
                >
                  {!!ucg.icon && (
                    <svg className="ucp__main__usecase-group__icon">
                      <use xlinkHref={ucg.icon} href={ucg.icon} />
                    </svg>
                  )}
                  {!!ucg.label && (
                    <div className="ucp__main__usecase-group__label">
                      {ucg.label}
                    </div>
                  )}
                </Link>
              ))}
            {/*<!-- USE CASES WITHOUT GROUPS -->*/}
            {!!ungroupedUseCases &&
              Object.values(ungroupedUseCases).map((useCase, index) => (
                <Link
                  key={useCase.identifier + "-" + index}
                  className="ucp__main__usecase-group clickable"
                  to={location => startFromRoute(location, useCase)}
                >
                  {!!useCase.icon && (
                    <svg className="ucp__main__usecase-group__icon">
                      <use xlinkHref={useCase.icon} href={useCase.icon} />
                    </svg>
                  )}
                  {!!useCase.label && (
                    <div className="ucp__main__usecase-group__label">
                      {useCase.label}
                    </div>
                  )}
                </Link>
              ))}
          </React.Fragment>
        )}
      </div>
    </won-usecase-picker>
  );
}
WonUseCasePicker.propTypes = {
  visibleUseCasesByConfig: PropTypes.object.isRequired,
  filterBySocketType: PropTypes.string,
};
