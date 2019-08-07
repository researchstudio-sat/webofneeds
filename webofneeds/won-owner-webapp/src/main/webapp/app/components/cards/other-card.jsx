/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import {get, getIn} from "../../utils.js";
import {actionCreators} from "../../actions/actions.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";

import "~/style/_other-card.scss";
import Immutable from "immutable";
import {relativeTime} from "../../won-label-utils.js";
import {selectLastUpdateTime} from "../../redux/selectors/general-selectors.js";
import WonAtomMap from "../atom-map.jsx";
import WonAtomSuggestionsIndicator from "../atom-suggestions-indicator.jsx";

export default class WonOtherCard extends React.Component {
  componentDidMount() {
    this.atomUri = this.props.atomUri;
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
    this.atomUri = nextProps.atomUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const atom = getIn(state, ["atoms", this.atomUri]);
    const useCaseIcon = atomUtils.getMatchedUseCaseIcon(atom);
    const iconBackground = atomUtils.getBackground(atom);
    const identiconSvg = !useCaseIcon
      ? atomUtils.getIdenticonSvg(atom)
      : undefined;

    const isDirectResponse = atomUtils.isDirectResponseAtom(atom);
    const responseToUri =
      isDirectResponse && getIn(atom, ["content", "responseToUri"]);
    const responseToAtom = getIn(state, ["atoms", responseToUri]);

    const atomImage = atomUtils.getDefaultImage(atom);
    const atomLocation = atomUtils.getLocation(atom);
    const personaUri = atomUtils.getHeldByUri(atom);
    const persona = getIn(state, ["atoms", personaUri]);
    const personaName = get(persona, "humanReadable");
    const personaHolds = persona && get(persona, "holds");
    const personaVerified =
      personaHolds && personaHolds.includes(this.atomUri);
    const personaIdenticonSvg = atomUtils.getIdenticonSvg(persona);
    const personaImage = atomUtils.getDefaultPersonaImage(persona);

    return {
      isDirectResponse: isDirectResponse,
      isInactive: atomUtils.isInactive(atom),
      responseToAtom,
      atom,
      persona,
      personaName,
      personaVerified,
      personaUri,
      atomTypeLabel: atomUtils.generateTypeLabel(atom),
      atomHasHoldableSocket: atomUtils.hasHoldableSocket(atom),
      isGroupChatEnabled: atomUtils.hasGroupSocket(atom),
      isChatEnabled: atomUtils.hasChatSocket(atom),
      friendlyTimestamp:
        atom &&
        relativeTime(
          selectLastUpdateTime(state),
          get(atom, "lastUpdateDate")
        ),
      showPersonaImage: personaImage,
      showPersonaIdenticon: !personaImage && personaIdenticonSvg,
      personaIdenticonSvg,
      personaImage,
      showMap: false, //!atomImage && atomLocation, //if no image is present but a location is, we display a map instead
      atomLocation,
      atomImage,
      showDefaultIcon: !atomImage, //&& !atomLocation, //if no image and no location are present we display the defaultIcon in the card__icon area, instead of next to the title
      useCaseIcon,
      iconBackground,
      identiconSvg,
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div/>;
    }

    const style = (this.state.showDefaultIcon && this.state.iconBackground) ? {
      backgroundColor: this.state.iconBackground
    } : undefined;

    const cardIcon = (
      <div className={"card__icon clickable " + (this.state.isInactive? " inactive " : "") + (this.state.showMap ? "card__icon--map" : "")} onClick={() => this.atomClick()} style={style} >
        {
          (this.state.showDefaultIcon && this.state.useCaseIcon)
            ? (
              <div className="identicon usecaseimage">
                <svg>
                  <use xlinkHref={this.state.useCaseIcon} href={this.state.useCaseIcon}/>
                </svg>
              </div>
            )
            : undefined
        }
        {
          (this.state.showDefaultIcon && this.state.identiconSvg)
          ? <img className="identicon" alt="Auto-generated title image" src={"data:image/svg+xml;base64," + this.state.identiconSvg}/>
          : undefined
        }
        {
          this.state.atomImage
          ? <img className="image" alt={this.state.atomImage.get('name')} src={"data:"+this.state.atomImage.get('type')+";base64,"+this.state.atomImage.get('data')} />
          : undefined
        }
        {
          this.state.showMap
          ? (
            <div className="won-atom-map location">
              <WonAtomMap locations={[this.state.atomLocation]} currentLocation={this.state.currentLocation} disableControls={true}/>
            </div>
          )
          : undefined
        }
      </div>
    );

    const cardMain = (
      <div className={"card__main clickable " + (!this.state.showDefaultIcon ? "card__main--showIcon" : "")} onClick={() => this.atomClick()}>
        {this.createCardMainIcon()}
        {this.createCardMainTopline()}
        {this.createCardMainSubtitle()}
      </div>
    );

    const cardPersonaInfo = this.props.showPersona && this.state.persona && this.state.atomHasHoldableSocket
      ? (
        <div className="card__persona clickable" onClick={() => this.personaClick(this.state.personaUri)}>
          {this.createPersonaInfoIcon()}
          {
            this.state.personaName
            ? (
              <div className="card__persona__name">
                <span className="card__persona__name__label">{this.state.personaName}</span>
                {this.createVerificationLabel()}
              </div>
            )
            : undefined
          }
          {this.createPersonaWebsite()}
        </div>
      )
      : undefined;

    const cardSuggestionIndicators = this.props.showSuggestions
      ? (
        <div className="card__indicators">
          <WonAtomSuggestionsIndicator atomUri={this.atomUri} ngRedux={this.props.ngRedux}/>
        </div>
      )
      : undefined;

    return (
      <won-other-card>
        {cardIcon}
        {cardMain}
        {cardPersonaInfo}
        {cardSuggestionIndicators}
      </won-other-card>
    );
  }

  createCardMainSubtitle() {
    const createGroupChatLabel = () => {
      if(this.state.isGroupChatEnabled) {
        return <span className="card__main__subtitle__type__groupchat">{"Group Chat"+ (this.state.isChatEnabled ? " enabled" : "")}</span>;
      }
      return undefined;
    };

    return (
      <div className="card__main__subtitle">
        <span className="card__main__subtitle__type">
          {createGroupChatLabel()}
          <span>
            {this.state.atomTypeLabel}
          </span>
        </span>
        <div className="card__main__subtitle__date">
          {this.state.friendlyTimestamp}
        </div>
      </div>
    );
  }

  createCardMainTopline() {
    const hasTitle = () => {
      if (this.state.isDirectResponse && this.state.responseToAtom) {
        return !!this.state.responseToAtom.get("humanReadable");
      } else {
        return !!this.state.atom && !!this.state.atom.get("humanReadable");
      }
    };

    const generateTitleString = () => {
      if (this.state.isDirectResponse && this.state.responseToAtom) {
        return "Re: " + this.state.responseToAtom.get("humanReadable");
      } else {
        return this.state.atom && this.state.atom.get("humanReadable");
      }
    };

    const generateCardTitle = () => {
      if(hasTitle()) {
        return <div className="card__main__topline__title">{generateTitleString()}</div>;
      } else {
        if(this.state.isDirectResponse) {
          return <div className="card__main__topline__notitle">no title</div>;
        } else {
          return <div className="card__main__topline__notitle">Re: no title</div>
        }
      }
    };

    return (
      <div className="card__main__topline">
        {generateCardTitle()}
      </div>);
  }

  createCardMainIcon() {
    if(!this.state.showDefaultIcon) {
      const style = (!this.state.hasImage && this.state.iconBackground) ? {
        backgroundColor: this.state.iconBackground
      } : undefined;

      return (
        <div className="card__main__icon" style={style}>
          {
            this.state.useCaseIcon
            ? (
              <div className="card__main__icon__usecaseimage">
                <svg>
                  <use xlinkHref={this.state.useCaseIcon} href={this.state.useCaseUtils} />
                </svg>
              </div>
            )
            : undefined
          }
          {
            this.state.identiconSvg
            ? <img className="card__main__icon__identicon" alt="Auto-generated title image" src={"data:image/svg+xml;base64,"+ this.state.identiconSvg} />
            : undefined
          }

        </div>
      );
    }
  }

  createPersonaWebsite() {
    if(this.state.personaWebsite) {
      return (
        [
          <div className="card__persona__websitelabel">Website:</div>,
          <a className="card__persona__websitelink" target="_blank" href={this.state.personaWebsite}>{this.state.personaWebsite}</a>
        ]
      );
    }
  }
  createVerificationLabel() {
    if(this.state.personaVerified) {
      return (
        <span
          className="card__persona__name__verification card__persona__name__verification--verified"
          title="The Persona-Relation of this Post is verified by the Persona">
          Verified
        </span>
      );
    } else {
      return (
        <span
          className="card__persona__name__verification card__persona__name__verification--unverified"
          title="The Persona-Relation of this Post is NOT verified by the Persona">
          Unverified!
        </span>
      );
    }
  }

  createPersonaInfoIcon() {
    if(this.state.showPersonaIdenticon) {
      return (
        <img className="card__persona__icon"
           alt="Auto-generated title image for persona that holds the atom"
           src={"data:image/svg+xml;base64," + this.state.personaIdenticonSvg}
        />
      );
    }
    if(this.state.showPersonaImage) {
      return (
        <img className="card__persona__icon"
            alt={this.state.personaImage.get('name')}
            src={"data:"+ this.state.personaImage.get("type") + ";base64," + this.state.personaImage.get('data')}
        />
      );
    }



  }

  atomClick() {
    if (this.props.onAtomClick) {
      this.props.onAtomClick();
    } else {
      this.props.ngRedux.dispatch(actionCreators.atoms__selectTab(
        Immutable.fromJS({ atomUri: this.atomUri, selectTab: "DETAIL" })
      ));
      this.props.ngRedux.dispatch(actionCreators.router__stateGo("post", { postUri: this.atomUri }));
    }
  }
  personaClick(personaUri) {
    this.props.ngRedux.dispatch(actionCreators.atoms__selectTab(
      Immutable.fromJS({ atomUri: personaUri, selectTab: "DETAIL" })
    ));
    this.props.ngRedux.dispatch(actionCreators.router__stateGo("post", { postUri: personaUri }));
  }
}