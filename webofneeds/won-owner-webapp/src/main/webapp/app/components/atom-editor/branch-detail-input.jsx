import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import { get } from "../../utils.js";

import "~/style/_branch-detail-input.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";

export default function WonBranchDetailInput({
  initialDraftImm,
  detailListImm,
  onUpdateImm,
}) {
  const [draftObjectImm, setDraftObjectImm] = useState(initialDraftImm);
  const [openDetail, toggleOpenDetail] = useState(undefined);

  const updateDetailImm = (name, value) => {
    let _draftObjectImm = draftObjectImm;

    detailListImm.map((detail, detailIdentifier) => {
      if (!get(draftObjectImm, detailIdentifier)) {
        _draftObjectImm = _draftObjectImm.set(detailIdentifier, undefined);
      }
    });

    setDraftObjectImm(_draftObjectImm.set(name, value));
  };

  useEffect(
    () => {
      console.debug("useEffect draftObjectImm changed...", draftObjectImm);
      onUpdateImm(draftObjectImm);
    },
    [draftObjectImm]
  );

  const generateDetailElements = () => {
    const detailElements = [];
    const draftObjectJS = draftObjectImm.toJS();

    if (detailListImm) {
      detailListImm
        .filter(detailImm => get(detailImm, "component"))
        .map(detailImm => {
          const detailIdentifier = get(detailImm, "identifier");
          const detailIcon = get(detailImm, "icon");
          const detailIconJS = detailIcon && detailIcon.toJS();
          const detailLabel = get(detailImm, "label");
          const isDetailMandatory = get(detailImm, "mandatory");

          const detailJS = detailImm.toJS();
          const DetailComponent = detailJS.component;
          const generateHumanReadable = detailJS.generateHumanReadable;

          const detailValueJS =
            draftObjectJS && draftObjectJS[detailIdentifier];

          const hasDetailValue = !!get(draftObjectImm, detailIdentifier);
          const isDetailExpanded = openDetail === detailIdentifier;

          const detailItemClasses = ["bdi__detail__items__item"];
          isDetailExpanded &&
            detailItemClasses.push("bdi__detail__items__item--won-expanded");
          hasDetailValue &&
            detailItemClasses.push("bdi__detail__items__item--won-hasvalue");

          const detailItemHeaderClasses = ["bdi__detail__items__item__header"];
          hasDetailValue &&
            !isDetailExpanded &&
            detailItemHeaderClasses.push(
              "bdi__detail__items__item__header--won-showvalue"
            );
          isDetailMandatory &&
            !(hasDetailValue || isDetailExpanded) &&
            detailItemHeaderClasses.push(
              "bdi__detail__items__item__header--won-showmandatoryindicator"
            );

          detailElements.push(
            <div key={detailIdentifier} className={detailItemClasses.join(" ")}>
              <div
                className={detailItemHeaderClasses.join(" ")}
                onClick={() =>
                  toggleOpenDetail(
                    isDetailExpanded ? undefined : detailIdentifier
                  )
                }
              >
                <svg className="bdi__circleicon">
                  <use xlinkHref={detailIconJS} href={detailIconJS} />
                </svg>
                <div className="bdi__detail__items__item__header__label">
                  {detailLabel}
                </div>
                {hasDetailValue &&
                  !isDetailExpanded && (
                    <div className="bdi__detail__items__item__header__content">
                      {generateHumanReadable({
                        value: detailValueJS,
                        includeLabel: false,
                      })}
                    </div>
                  )}
                {isDetailMandatory &&
                  !(hasDetailValue || isDetailExpanded) && (
                    <div
                      className="bdi__mandatory"
                      title="This is a mandatory Detail"
                    >
                      <svg className="bdi__mandatory__icon">
                        <use
                          xlinkHref={ico16_indicator_warning}
                          href={ico16_indicator_warning}
                        />
                      </svg>
                    </div>
                  )}
              </div>
              {isDetailExpanded && (
                <div className="bdi__detail__items__item__component">
                  <DetailComponent
                    onUpdate={({ value }) =>
                      updateDetailImm(detailIdentifier, value)
                    }
                    initialValue={detailValueJS}
                    detail={detailJS}
                  />
                </div>
              )}
            </div>
          );
        });
    }
    return detailElements;
  };

  return (
    <won-branch-detail-input>
      <div className="bdi__detail__items">{generateDetailElements()}</div>
    </won-branch-detail-input>
  );
}
WonBranchDetailInput.propTypes = {
  detailListImm: PropTypes.object,
  initialDraftImm: PropTypes.object,
  onUpdateImm: PropTypes.func,
};
