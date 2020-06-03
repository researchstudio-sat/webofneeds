import React from "react";
import { get } from "../utils.js";
import * as usecaseUtils from "../usecase-utils.js";

import "~/style/_atom-content-details.scss";
import PropTypes from "prop-types";

export default function WonAtomContentDetails({ atom, branch }) {
  const details = branch && get(atom, branch);

  /**
   * Checks if the given contentDetailKey is "title" and the corresponding branch to view is "content"
   * @param contentDetailKey
   * @returns {boolean}
   */
  function shouldOmitDetail(contentDetail, contentDetailKey) {
    return (
      !contentDetail || (contentDetailKey === "title" && branch === "content")
    );
  }
  const allDetailsImm = usecaseUtils.getAllDetailsImm();
  const contentDetailsMap =
    details &&
    details.map((contentDetail, contentDetailKey) => {
      const detailDefinitionImm = get(allDetailsImm, contentDetailKey);
      if (
        detailDefinitionImm &&
        !shouldOmitDetail(contentDetail, contentDetailKey)
      ) {
        const detailDefinition = detailDefinitionImm.toJS();
        const ReactViewerComponent =
          detailDefinition && detailDefinition.viewerComponent;

        if (ReactViewerComponent) {
          return (
            <div key={contentDetailKey} className="pis__component">
              <ReactViewerComponent
                detail={detailDefinition}
                content={contentDetail}
              />
            </div>
          );
        }
      }

      return undefined;
    });

  const contentDetailsArray = contentDetailsMap
    ? contentDetailsMap.toArray()
    : [];

  return (
    <won-atom-content-details>{contentDetailsArray}</won-atom-content-details>
  );
}
WonAtomContentDetails.propTypes = {
  atom: PropTypes.object.isRequired,
  branch: PropTypes.string.isRequired,
};
