/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import ReactMarkdown from "react-markdown";
import { get } from "../../utils.js";
import { noParsableContentPlaceholder } from "~/app/won-label-utils.js";
import * as messageUtils from "../../redux/utils/message-utils.js";
import * as usecaseUtils from "../../usecase-utils.js";

import "~/style/_message-content.scss";
import "~/style/_won-markdown.scss";
import vocab from "../../service/vocab.js";

export default function WonMessageContent({ message }) {
  const content = messageUtils.getContent(message);
  const matchScore = get(content, "matchScore");
  const text = get(content, "text");

  const messageType = messageUtils.getType(message);
  const matchScorePercentage = matchScore && matchScore * 100;

  if (message) {
    const markdownText = text ? (
      <ReactMarkdown
        className="msg__text markdown"
        source={text}
        linkTarget="_blank"
      />
    ) : (
      undefined
    );
    const matchScore = matchScore ? (
      <div className="msg__matchScore">MatchScore: {matchScorePercentage}%</div>
    ) : (
      undefined
    );

    const noParsableContent =
      messageType !== vocab.WONMSG.connectMessage &&
      !messageUtils.isParsable(message) ? (
        <div className="msg__text markdown">{noParsableContentPlaceholder}</div>
      ) : (
        undefined
      );

    const allDetailsImm = usecaseUtils.getAllDetailsImm();
    const contentDetailsMap =
      content &&
      content.map((contentDetail, contentDetailKey) => {
        const detailDefinitionImm = get(allDetailsImm, contentDetailKey);

        const detailDefinition =
          detailDefinitionImm && detailDefinitionImm.toJS();
        const ReactViewerComponent =
          detailDefinition && detailDefinition.viewerComponent;

        if (detailDefinition && ReactViewerComponent) {
          return (
            <div key={contentDetailKey} className="msg__content">
              <ReactViewerComponent
                className="won-in-message"
                detail={detailDefinition}
                content={contentDetail}
              />
            </div>
          );
        }

        return undefined;
      });

    const contentDetailsArray = contentDetailsMap
      ? contentDetailsMap.toArray()
      : [];

    return (
      <won-message-content>
        {markdownText}
        {contentDetailsArray}
        {matchScore}
        {noParsableContent}
      </won-message-content>
    );
  } else {
    return (
      <won-message-content>
        <div className="msg__text hide-in-responsive clickable">
          «Message not (yet) loaded. Click to Load»
        </div>
        <div className="msg__text show-in-responsive clickable">
          «Message not (yet) loaded. Tap to Load»
        </div>
      </won-message-content>
    );
  }
}

WonMessageContent.propTypes = {
  message: PropTypes.object,
};
