/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import * as atomUtils from "~/app/redux/utils/atom-utils";
import * as connectionUtils from "~/app/redux/utils/connection-utils";
import { useSelector } from "react-redux";
import * as generalSelectors from "~/app/redux/selectors/general-selectors";
import { relativeTime } from "~/app/won-label-utils";
import WonGenericFeedItem from "~/app/components/feed-items/generic-feed-item";
import { get, getUri } from "~/app/utils";

import "~/style/_atom-feed.scss";

export default function WonAtomFeed({
  atom,
  isOwned,
  storedAtoms,
  showItemCount,
}) {
  const globalLastUpdateTime = useSelector(
    generalSelectors.selectLastUpdateTime
  );
  const feedElements = [];

  let lastDivider;

  console.debug("showItemCount: ", showItemCount);

  atomUtils
    .getConnections(atom)
    .toOrderedMap()
    .sortBy(conn => {
      const lastUpdateDate = connectionUtils.getLastUpdateDate(conn);
      return lastUpdateDate && lastUpdateDate.getTime();
    })
    .reverse()
    .map(conn => {
      const lastUpdateDate = connectionUtils.getLastUpdateDate(conn);
      const friendlyLastUpdateDate =
        lastUpdateDate && relativeTime(globalLastUpdateTime, lastUpdateDate);

      if (lastDivider !== friendlyLastUpdateDate) {
        feedElements.push(
          <div className="af__datedivider">{friendlyLastUpdateDate}</div>
        );
      }
      lastDivider = friendlyLastUpdateDate;

      feedElements.push(
        <div className="af__item">
          <WonGenericFeedItem
            key={getUri(conn)}
            connection={conn}
            hideSenderAtom={atom}
            senderAtom={atom}
            targetAtom={get(
              storedAtoms,
              connectionUtils.getTargetAtomUri(conn)
            )}
            isOwned={isOwned}
          />
        </div>
      );
    });

  return (
    <won-atom-feed>
      <div className="af__header">Feed</div>
      {feedElements}
    </won-atom-feed>
  );
}
WonAtomFeed.propTypes = {
  atom: PropTypes.object.isRequired,
  storedAtoms: PropTypes.object.isRequired,
  isOwned: PropTypes.bool,
  showItemCount: PropTypes.number,
};
