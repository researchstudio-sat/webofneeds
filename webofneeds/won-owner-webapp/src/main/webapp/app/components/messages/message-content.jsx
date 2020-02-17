/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import ReactMarkdown from "react-markdown";
import { get, getIn } from "../../utils.js";
import { connect } from "react-redux";
import { getOwnedAtomByConnectionUri } from "../../redux/selectors/general-selectors.js";
import * as usecaseUtils from "../../usecase-utils.js";

import "~/style/_message-content.scss";
import "~/style/_won-markdown.scss";
import vocab from "../../service/vocab.js";

const noParsableContentPlaceholder =
  "«This message couldn't be displayed as it didn't contain," +
  "any parsable content! " +
  'Click on the "Show raw RDF data"-button in ' +
  'the footer of the page to see the "raw" message-data.»';

const mapStateToProps = (state, ownProps) => {
  const ownedAtom =
    ownProps.connectionUri &&
    getOwnedAtomByConnectionUri(state, ownProps.connectionUri);
  const connection =
    ownedAtom && ownedAtom.getIn(["connections", ownProps.connectionUri]);
  const message =
    connection &&
    ownProps.messageUri &&
    getIn(connection, ["messages", ownProps.messageUri]);

  const content = get(message, "content");
  const matchScore = get(content, "matchScore");
  const text = get(content, "text");

  return {
    messageUri: ownProps.messageUri,
    connectionUri: ownProps.connectionUri,
    message,
    messageType: message && message.get("messageType"),
    matchScorePercentage: matchScore && matchScore * 100,
    matchScore,
    text,
    content,
  };
};

class WonMessageContent extends React.Component {
  render() {
    if (this.props.message) {
      const markdownText = this.props.text ? (
        <ReactMarkdown
          className="msg__text markdown"
          source={this.props.text}
        />
      ) : (
        undefined
      );
      const matchScore = this.props.matchScore ? (
        <div className="msg__matchScore">
          MatchScore: {this.props.matchScorePercentage}%
        </div>
      ) : (
        undefined
      );

      const noParsableContent =
        this.props.messageType !== vocab.WONMSG.connectMessage &&
        this.props.messageType !== vocab.WONMSG.openMessage &&
        !get(this.props.message, "isParsable") ? (
          <div className="msg__text markdown">
            {noParsableContentPlaceholder}
          </div>
        ) : (
          undefined
        );

      const allDetailsImm = usecaseUtils.getAllDetailsImm();
      const contentDetailsMap =
        this.props.content &&
        this.props.content.map((contentDetail, contentDetailKey) => {
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
}

WonMessageContent.propTypes = {
  messageUri: PropTypes.string.isRequired,
  connectionUri: PropTypes.string.isRequired,
  message: PropTypes.object,
  messageType: PropTypes.string,
  text: PropTypes.string,
  matchScore: PropTypes.number,
  matchScorePercentage: PropTypes.number,
  content: PropTypes.object,
};

export default connect(mapStateToProps)(WonMessageContent);
