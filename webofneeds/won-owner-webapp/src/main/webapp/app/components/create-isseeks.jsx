import React, { useState } from "react";

import "~/style/_create-isseeks.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import PropTypes from "prop-types";
import { clone } from "../utils.js";

export default function WonCreateIsSeeks({
  initialDraft,
  detailList,
  onUpdate,
}) {
  const details = new Set();
  const draftObject = clone(initialDraft);

  for (const draftDetail in initialDraft) {
    details.add(draftDetail);
    draftObject[draftDetail] = initialDraft[draftDetail];
  }

  const [state, setState] = useState({
    details: details,
    openDetail: undefined,
    draftObject: JSON.parse(JSON.stringify(draftObject)),
  });

  function generateHumanReadable(detail) {
    return detail.generateHumanReadable({
      value: state.draftObject[detail.identifier],
      includeLabel: false,
    });
  }

  function toggleOpenDetail(detail) {
    if (state.openDetail === detail) {
      setState({ ...state, openDetail: undefined });
    } else {
      setState({ ...state, openDetail: detail });
    }
  }

  function updateDetail(name, value) {
    const _details = state.details;
    const _draftObject = state.draftObject;
    if (value) {
      if (!_details.has(name)) {
        _details.add(name);
      }
      _draftObject[name] = value;
    } else if (_details.has(name)) {
      _details.delete(name);
      _draftObject[name] = undefined;
    }
    setState({
      ...state,
      details: _details,
      draftObject: _draftObject,
    });

    updateDraft();
  }

  function updateDraft() {
    const _draftObject = JSON.parse(JSON.stringify(state.draftObject));

    for (const detail in detailList) {
      if (!state.details.has(detail)) {
        _draftObject[detail] = undefined;
      }
    }
    setState({
      ...state,
      draftObject: _draftObject,
    });
    onUpdate({ draft: _draftObject });
  }

  const detailElements =
    detailList &&
    Object.values(detailList).map(detail => {
      if (detail.component) {
        const detailItemClasses = ["cis__detail__items__item"];
        state.openDetail === detail.identifier &&
          detailItemClasses.push("cis__detail__items__item--won-expanded");
        state.details.has(detail.identifier) &&
          detailItemClasses.push("cis__detail__items__item--won-hasvalue");

        const detailItemHeaderClasses = ["cis__detail__items__item__header"];
        state.details.has(detail.identifier) &&
          state.openDetail !== detail.identifier &&
          detailItemHeaderClasses.push(
            "cis__detail__items__item__header--won-showvalue"
          );
        detail.mandatory &&
          !(
            state.details.has(detail.identifier) ||
            state.openDetail === detail.identifier
          ) &&
          detailItemHeaderClasses.push(
            "cis__detail__items__item__header--won-showmandatoryindicator"
          );

        return (
          <div key={detail.identifier} className={detailItemClasses.join(" ")}>
            <div
              className={detailItemHeaderClasses.join(" ")}
              onClick={() => toggleOpenDetail(detail.identifier)}
            >
              <svg className="cis__circleicon">
                <use xlinkHref={detail.icon} href={detail.icon} />
              </svg>
              <div className="cis__detail__items__item__header__label">
                {detail.label}
              </div>
              {state.details.has(detail.identifier) &&
                state.openDetail !== detail.identifier && (
                  <div className="cis__detail__items__item__header__content">
                    {generateHumanReadable(detail)}
                  </div>
                )}
              {detail.mandatory &&
                !(
                  state.details.has(detail.identifier) ||
                  state.openDetail === detail.identifier
                ) && (
                  <div
                    className="cis__mandatory"
                    title="This is a mandatory Detail"
                  >
                    <svg className="cis__mandatory__icon">
                      <use
                        xlinkHref={ico16_indicator_warning}
                        href={ico16_indicator_warning}
                      />
                    </svg>
                  </div>
                )}
            </div>
            {state.openDetail === detail.identifier && (
              <div className="cis__detail__items__item__component">
                <detail.component
                  onUpdate={({ value }) =>
                    updateDetail(detail.identifier, value)
                  }
                  initialValue={state.draftObject[detail.identifier]}
                  detail={detail}
                />
              </div>
            )}
          </div>
        );
      }
    });

  return (
    <won-create-isseeks>
      <div className="cis__detail__items">{detailElements}</div>
    </won-create-isseeks>
  );
}
WonCreateIsSeeks.propTypes = {
  detailList: PropTypes.object,
  initialDraft: PropTypes.object,
  onUpdate: PropTypes.func,
};
