/**
 * Created by ksinger on 17.06.2016.
 */

;

import Medium from '../mediumjs-es6';
import angular from 'angular';
import 'ng-redux';
import Immutable from 'immutable';
import 'angular-sanitize';
import { dispatchEvent, attach, delay } from '../utils';
import { actionCreators }  from '../actions/actions';

window.Medium4dbg = Medium;

function genComponentConf() {
    let template = `
        <div class="wdt__left">
            <div class="wdt__text"
                    ng-class="{ 'valid' : self.valid(), 'invalid' : !self.valid() }">
                <div class="medium-mount"></div>
            </div>
            <span class="wdt__charcount">
                {{ self.charactersLeft() }} characters left
            </span>
        </div>
    `;

    const serviceDependencies = ['$scope', '$element', '$ngRedux', /*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);
            window.ntf4dbg = this;

            this.characterLimit = 140; //TODO move to conf

            const selectFromState = (state) => ({
                draftId: state.getIn(['router', 'currentParams', 'draftId'])
            })
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

            this.initMedium();

            this.mediumMountNg().bind('input', e => {
                this.drafts__change__title({
                    draftId: this.draftId,
                    title: this.medium.value()
                });
            });
        }
        charactersLeft() {
            return this.characterLimit - this.medium.value().length;
        }
        valid() {
            return this.charactersLeft() >= 0;
        }

        initMedium() {
            // initialising editor. see http://jakiestfu.github.io/Medium.js/docs/
            this.medium = new Medium({
                element: this.mediumMount(),

                modifier: 'auto',
                placeholder: 'What',
                autoHR: false, //if true, inserts <hr> after two empty lines
                mode: Medium.inlineMode, // no newlines, no styling
                //mode: Medium.partialMode, // allows newlines, no styling
                //maxLength: this.maxChars, // -1 would disable it
                tags: {
                    /*
                     'break': 'br',
                     'horizontalRule': 'hr',
                     'paragraph': 'p',
                     'outerLevel': ['pre', 'blockquote', 'figure'],
                     'innerLevel': ['a', 'b', 'u', 'i', 'img', 'strong']
                     */
                },
                attributes: {
                    //remove: ['style', 'class'] //TODO does this remove the ng-class?
                    remove: ['style'] //TODO does this remove the ng-class?
                },
            });

            //remove the inline-styles placed by medium.js
            this.medium.placeholder.style = "";
            this.mediumMount().addEventListener('blur', e =>
                delay(0) //push to end end of task-queue (and thus all other `blur`-listeners
                    .then(() => {
                        const style = this.medium.placeholder.style;
                        style.minHeight = 0;
                        style.minWidth = 0;
                    })
            );
        }

        mediumMountNg() {
            if(!this._mediumMount) {
                this._mediumMount = this.textFieldNg().find('.medium-mount')
            }
            return this._mediumMount;
        }

        mediumMount() {
            return this.mediumMountNg()[0];
        }

        textFieldNg() {
            if(!this._textField) {
                this._textField = this.$element.find('.wdt__text');
            }
            return this._textField;
        }
        textField() {
            return this.textFieldNg()[0];
        }
    }
    Controller.$inject = serviceDependencies;


    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: { },
        template: template
    }
}


export default angular.module('won.owner.components.needTextfield', [ ])
    .directive('needTextfield', genComponentConf)
    .name;