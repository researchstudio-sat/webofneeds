/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { connect } from "react-redux";
import { get, getIn } from "../../utils.js";
import Immutable from "immutable";
import { actionCreators } from "../../actions/actions.js";
import PropTypes from "prop-types";

import WonAtomMap from "../atom-map.jsx";
import WonAtomSuggestionsIndicator from "../atom-suggestions-indicator.jsx";
import WonAtomConnectionsIndicator from "../atom-connections-indicator.jsx";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { relativeTime } from "../../won-label-utils.js";
import {
  selectLastUpdateTime,
  hasUnreadChatConnections,
} from "../../redux/selectors/general-selectors.js";

import "~/style/_other-card.scss";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const useCaseIcon = atomUtils.getMatchedUseCaseIcon(atom);
  const iconBackground = atomUtils.getBackground(atom);
  const identiconSvg = !useCaseIcon
    ? atomUtils.getIdenticonSvg(atom)
    : undefined;

  const isDirectResponse = atomUtils.isDirectResponseAtom(atom);
  const responseToUri =
    isDirectResponse && getIn(atom, ["content", "responseToUri"]);
  const responseToAtom = responseToUri
    ? getIn(state, ["atoms", responseToUri])
    : undefined;

  const atomImage = atomUtils.getDefaultImage(atom);
  const atomLocation = atomUtils.getLocation(atom);
  const personaUri = atomUtils.getHeldByUri(atom);
  const persona = getIn(state, ["atoms", personaUri]);
  const personaName = get(persona, "humanReadable");
  const personaHolds = persona && get(persona, "holds");
  const personaVerified =
    personaHolds && personaHolds.includes(ownProps.atomUri);
  const personaIdenticonSvg = atomUtils.getIdenticonSvg(persona);
  const personaImage = atomUtils.getDefaultPersonaImage(persona);

  return {
    atomUri: ownProps.atomUri,
    onAtomClick: ownProps.onAtomClick,
    showPersona: ownProps.showPersona,
    showSuggestions: ownProps.showSuggestions,
    currentLocation: ownProps.currentLocation,
    isDirectResponse: isDirectResponse,
    isInactive: atomUtils.isInactive(atom),
    responseToAtom,
    atom,
    persona,
    personaName,
    personaVerified,
    personaWebsite: getIn(persona, ["content", "website"]),
    personaUri,
    atomTypeLabel: atomUtils.generateTypeLabel(atom),
    atomHasHoldableSocket: atomUtils.hasHoldableSocket(atom),
    isGroupChatEnabled: atomUtils.hasGroupSocket(atom),
    isChatEnabled: atomUtils.hasChatSocket(atom),
    friendlyTimestamp:
      atom &&
      relativeTime(selectLastUpdateTime(state), get(atom, "lastUpdateDate")),
    showPersonaImage: !!personaImage,
    showPersonaIdenticon: !personaImage && !!personaIdenticonSvg,
    personaIdenticonSvg,
    personaImage,
    showMap: false, //!atomImage && atomLocation, //if no image is present but a location is, we display a map instead
    atomLocation,
    atomImage,
    showDefaultIcon: !atomImage, //&& !atomLocation, //if no image and no location are present we display the defaultIcon in the card__icon area, instead of next to the title
    useCaseIcon,
    iconBackground,
    identiconSvg,
    hasUnreadChatConnections: hasUnreadChatConnections(state),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    selectAtomTab: (atomUri, selectTab) => {
      dispatch(
        actionCreators.atoms__selectTab(
          Immutable.fromJS({
            atomUri: atomUri,
            selectTab: selectTab,
          })
        )
      );
    },
  };
};

class WonOtherCard extends React.Component {
  constructor(props) {
    super(props);
    this.atomClick = this.atomClick.bind(this);
    this.personaClick = this.personaClick.bind(this);
  }
  render() {
    const style =
      this.props.showDefaultIcon && this.props.iconBackground
        ? {
            backgroundColor: this.props.iconBackground,
          }
        : undefined;

    const cardIcon = (
      <div
        className={
          "card__icon clickable " +
          (this.props.isInactive ? " inactive " : "") +
          (this.props.showMap ? "card__icon--map" : "")
        }
        onClick={this.atomClick}
        style={style}
      >
        {this.props.showDefaultIcon && this.props.useCaseIcon ? (
          <div className="identicon usecaseimage">
            <svg>
              <use
                xlinkHref={this.props.useCaseIcon}
                href={this.props.useCaseIcon}
              />
            </svg>
          </div>
        ) : (
          undefined
        )}
        {this.props.showDefaultIcon && this.props.identiconSvg ? (
          <img
            className="identicon"
            alt="Auto-generated title image"
            src={"data:image/svg+xml;base64," + this.props.identiconSvg}
          />
        ) : (
          undefined
        )}
        {this.props.atomImage ? (
          <img
            className="image"
            alt={this.props.atomImage.get("name")}
            src={
              "data:" +
              this.props.atomImage.get("type") +
              ";base64," +
              this.props.atomImage.get("data")
            }
          />
        ) : (
          undefined
        )}
        {this.props.showMap ? (
          <WonAtomMap
            className="location"
            locations={[this.props.atomLocation]}
            currentLocation={this.props.currentLocation}
            disableControls={true}
          />
        ) : (
          undefined
        )}
      </div>
    );

    const cardMain = (
      <div
        className={
          "card__main clickable " +
          (!this.props.showDefaultIcon ? "card__main--showIcon" : "")
        }
        onClick={this.atomClick}
      >
        {this.createCardMainIcon()}
        {this.createCardMainTopline()}
        {this.createCardMainSubtitle()}
      </div>
    );

    const cardPersonaInfo =
      this.props.showPersona &&
      this.props.persona &&
      this.props.atomHasHoldableSocket ? (
        <div className="card__persona clickable" onClick={this.personaClick}>
          {this.createPersonaInfoIcon()}
          {this.props.personaName ? (
            <div className="card__persona__name">
              <span className="card__persona__name__label">
                {this.props.personaName}
              </span>
              {this.createVerificationLabel()}
            </div>
          ) : (
            undefined
          )}
          {this.createPersonaWebsite()}
        </div>
      ) : (
        undefined
      );

    // const cardSuggestionIndicators = this.props.showSuggestions ? (
    //   <div className="card__indicators">
    //     <WonAtomSuggestionsIndicator atomUri={this.props.atomUri} />
    //   </div>
    // ) : (
    //   undefined
    // );

    // const cardNewConnectionIndicators = this.props.showSuggestions ? (
    //   <div className="card__indicators">
    //     <WonAtomConnectionsIndicator atomUri={this.props.atomUri} />
    //   </div>
    // ) : (
    //   undefined
    // );

    const cardConnectionIndicators = this.props.showSuggestions ? (
      this.props.hasUnreadChatConnections ? (
        <div className="card__indicators">
          <WonAtomConnectionsIndicator atomUri={this.props.atomUri} />
        </div>
      ) : (
        <div className="card__indicators">
          <WonAtomSuggestionsIndicator atomUri={this.props.atomUri} />
        </div>
      )
    ) : (
      undefined
    );

    // TODO: add if clause for return value to show only suggestions or connections
    return (
      <won-other-card>
        {cardIcon}
        {cardMain}
        {cardPersonaInfo}
        {cardConnectionIndicators}
      </won-other-card>
    );
  }

  createCardMainSubtitle() {
    const createGroupChatLabel = () => {
      if (this.props.isGroupChatEnabled) {
        return (
          <span className="card__main__subtitle__type__groupchat">
            {"Group Chat" + (this.props.isChatEnabled ? " enabled" : "")}
          </span>
        );
      }
      return undefined;
    };

    return (
      <div className="card__main__subtitle">
        <span className="card__main__subtitle__type">
          {createGroupChatLabel()}
          <span>{this.props.atomTypeLabel}</span>
        </span>
        <div className="card__main__subtitle__date">
          {this.props.friendlyTimestamp}
        </div>
      </div>
    );
  }

  createCardMainTopline() {
    const hasTitle = () => {
      if (this.props.isDirectResponse && this.props.responseToAtom) {
        return !!this.props.responseToAtom.get("humanReadable");
      } else {
        return !!this.props.atom && !!this.props.atom.get("humanReadable");
      }
    };

    const generateTitleString = () => {
      if (this.props.isDirectResponse && this.props.responseToAtom) {
        return "Re: " + this.props.responseToAtom.get("humanReadable");
      } else {
        return this.props.atom && this.props.atom.get("humanReadable");
      }
    };

    const generateCardTitle = () => {
      if (hasTitle()) {
        return (
          <div className="card__main__topline__title">
            {generateTitleString()}
          </div>
        );
      } else {
        if (this.props.isDirectResponse) {
          return <div className="card__main__topline__notitle">no title</div>;
        } else {
          return (
            <div className="card__main__topline__notitle">Re: no title</div>
          );
        }
      }
    };

    return <div className="card__main__topline">{generateCardTitle()}</div>;
  }

  createCardMainIcon() {
    if (!this.props.showDefaultIcon) {
      const style =
        !this.props.atomImage && this.props.iconBackground
          ? {
              backgroundColor: this.props.iconBackground,
            }
          : undefined;

      return (
        <div className="card__main__icon" style={style}>
          {this.props.useCaseIcon ? (
            <div className="card__main__icon__usecaseimage">
              <svg>
                <use
                  xlinkHref={this.props.useCaseIcon}
                  href={this.props.useCaseIcon}
                />
              </svg>
            </div>
          ) : (
            undefined
          )}
          {this.props.identiconSvg ? (
            <img
              className="card__main__icon__identicon"
              alt="Auto-generated title image"
              src={"data:image/svg+xml;base64," + this.props.identiconSvg}
            />
          ) : (
            undefined
          )}
        </div>
      );
    }
  }

  createPersonaWebsite() {
    if (this.props.personaWebsite) {
      return (
        <React.Fragment>
          <div className="card__persona__websitelabel">Website:</div>,
          <a
            className="card__persona__websitelink"
            target="_blank"
            rel="noopener noreferrer"
            href={this.props.personaWebsite}
          >
            {this.props.personaWebsite}
          </a>
        </React.Fragment>
      );
    }
  }
  createVerificationLabel() {
    if (this.props.personaVerified) {
      return (
        <span
          className="card__persona__name__verification card__persona__name__verification--verified"
          title="The Persona-Relation of this Post is verified by the Persona"
        >
          Verified
        </span>
      );
    } else {
      return (
        <span
          className="card__persona__name__verification card__persona__name__verification--unverified"
          title="The Persona-Relation of this Post is NOT verified by the Persona"
        >
          Unverified!
        </span>
      );
    }
  }

  createPersonaInfoIcon() {
    if (this.props.showPersonaIdenticon) {
      return (
        <img
          className="card__persona__icon"
          alt="Auto-generated title image for persona that holds the atom"
          src={"data:image/svg+xml;base64," + this.props.personaIdenticonSvg}
        />
      );
    }
    if (this.props.showPersonaImage) {
      return (
        <img
          className="card__persona__icon"
          alt={this.props.personaImage.get("name")}
          src={
            "data:" +
            this.props.personaImage.get("type") +
            ";base64," +
            this.props.personaImage.get("data")
          }
        />
      );
    }
  }

  atomClick() {
    if (this.props.onAtomClick) {
      this.props.onAtomClick();
    } else {
      this.props.selectAtomTab(this.props.atomUri, "DETAIL");
      this.props.routerGo("post", { postUri: this.props.atomUri });
    }
  }
  personaClick() {
    this.props.selectAtomTab(this.props.personaUri, "DETAIL");
    this.props.routerGo("post", { postUri: this.props.personaUri });
  }
}
WonOtherCard.propTypes = {
  atomUri: PropTypes.string.isRequired,
  showPersona: PropTypes.bool,
  showSuggestions: PropTypes.bool,
  currentLocation: PropTypes.object,
  onAtomClick: PropTypes.func,
  routerGo: PropTypes.func,
  selectAtomTab: PropTypes.func,

  isDirectResponse: PropTypes.bool,
  isInactive: PropTypes.bool,
  responseToAtom: PropTypes.object,
  atom: PropTypes.object,
  persona: PropTypes.object,
  personaName: PropTypes.string,
  personaVerified: PropTypes.bool,
  personaWebsite: PropTypes.string,
  personaUri: PropTypes.string,
  atomTypeLabel: PropTypes.string,
  atomHasHoldableSocket: PropTypes.bool,
  isGroupChatEnabled: PropTypes.bool,
  isChatEnabled: PropTypes.bool,
  friendlyTimestamp: PropTypes.any,
  showPersonaImage: PropTypes.bool,
  showPersonaIdenticon: PropTypes.bool,
  personaIdenticonSvg: PropTypes.string,
  personaImage: PropTypes.string,
  showMap: PropTypes.bool,
  atomLocation: PropTypes.object,
  atomImage: PropTypes.string,
  showDefaultIcon: PropTypes.bool,
  useCaseIcon: PropTypes.string,
  iconBackground: PropTypes.string,
  identiconSvg: PropTypes.string,
  hasUnreadChatConnections: PropTypes.bool,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonOtherCard);
