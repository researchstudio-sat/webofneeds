/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import ReactMarkdown from "react-markdown";
import { get, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { getOwnedAtomByConnectionUri } from "../../redux/selectors/general-selectors.js";
import * as usecaseUtils from "../../usecase-utils.js";

import "~/style/_message-content.scss";
import "~/style/_won-markdown.scss";
import won from "../../won-es6";

export default class WonMessageContent extends React.Component {
  componentDidMount() {
    this.noParsableContentPlaceholder =
      "«This message couldn't be displayed as it didn't contain," +
      "any parsable content! " +
      'Click on the "Show raw RDF data"-button in ' +
      'the footer of the page to see the "raw" message-data.»';

    this.messageUri = this.props.messageUri;
    this.connectionUri = this.props.connectionUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.messageUri = nextProps.messageUri;
    this.connectionUri = nextProps.connectionUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const ownedAtom =
      this.connectionUri &&
      getOwnedAtomByConnectionUri(state, this.connectionUri);
    const connection =
      ownedAtom && ownedAtom.getIn(["connections", this.connectionUri]);
    const message =
      connection &&
      this.messageUri &&
      getIn(connection, ["messages", this.messageUri]);

    const content = get(message, "content");
    const matchScore = get(content, "matchScore");
    const text = get(content, "text");

    return {
      connection,
      message,
      messageType: message && message.get("messageType"),
      matchScorePercentage: matchScore && matchScore * 100,
      matchScore,
      text,
      content,
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    if (this.state.message) {
      const markdownText = this.state.text ? (
        <ReactMarkdown
          className="msg__text markdown"
          source={this.state.text}
        />
      ) : (
        undefined
      );
      const matchScore = this.state.matchScore ? (
        <div className="msg__matchScore">
          MatchScore: {this.state.matchScorePercentage}%
        </div>
      ) : (
        undefined
      );

      const noParsableContent =
        this.state.messageType !== won.WONMSG.connectMessage &&
        this.state.messageType !== won.WONMSG.openMessage &&
        !get(this.state.message, "isParsable") ? (
          <div className="msg__text markdown">
            {this.noParsableContentPlaceholder}
          </div>
        ) : (
          undefined
        );

      const allDetailsImm = usecaseUtils.getAllDetailsImm();
      const contentDetailsMap =
        this.state.content &&
        this.state.content.map((contentDetail, contentDetailKey) => {
          const detailDefinition = get(allDetailsImm, contentDetailKey);
          const ReactViewerComponent = get(
            detailDefinition,
            "reactViewerComponent"
          );

          if (detailDefinition && ReactViewerComponent) {
            return (
              <div key={contentDetailKey} className="msg__content">
                <ReactViewerComponent
                  className="won-in-message"
                  detail={detailDefinition.toJS()}
                  content={contentDetail}
                  ngRedux={this.props.ngRedux}
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
  ngRedux: PropTypes.object.isRequired,
};
