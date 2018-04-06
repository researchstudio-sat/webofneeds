/**
 * Also a resizing textfield that can produce messages but it only uses 
 * a standard text-area instead of contenteditable. Thus it should be 
 * stabler but can't do rich text / wysiwyg-formatting.
 * 
 * Created by ksinger on 16.02.2018.
 */

;

// import Medium from '../mediumjs-es6.js';
import angular from 'angular';
// import 'ng-redux';
import Immutable from 'immutable';
import 'angular-sanitize';
// import Medium from 'medium.js';
import {
    dispatchEvent,
    attach,
    delay,
    is,
} from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import autoresizingTextareaModule from '../directives/textarea-autogrow.js';

function genComponentConf() {
    let template = `
        <div class="wdt__left">
            <textarea 
                class="wdt__text"
                ng-class="{ 'valid' : self.valid(), 'invalid' : !self.valid() }"
                won-textarea-autogrow 
                style="resize: none; height: auto;" 
                tabindex="0"
                placeholder="{{::self.placeholder}}"></textarea>
            <span class="wdt__charcount" ng-show="self.maxChars">
                {{ self.charactersLeft() }} characters left
            </span>
        </div>
        <button
            class="wdt__submitbutton red"
            ng-show="::self.submitButtonLabel"
            ng-click="::self.submit()">
            {{ ::(self.submitButtonLabel || 'Send') }}
        </button>
    `;

    const serviceDependencies = ['$scope', '$element', /*'$ngRedux',/*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);
            window.ctfs4dbg = this;

            /*
            const selectFromState = (state) => ({
                draftId: state.getIn(['router', 'currentParams', 'draftId'])
            })
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
            */

            this.textFieldNg().bind('input', e => {
                this.input()
                return false;
            });
            this.textFieldNg().bind('paste', e => {
                this.paste()
            });
            this.textFieldNg().bind('keydown', e => {
                this.keydown(e)
                return false;
            });
        }
        keydown(e) {
            if(e.keyCode === 13 && !e.shiftKey) {
                this.submit();
                return false;
            }
        }
        paste() {
            const payload = {
                value: this.value(),
                valid: this.valid(),
            };
            this.onPaste(payload);
            dispatchEvent(this.$element[0], 'paste', payload);
        }
        input() {
            const payload = {
                value: this.value(),
                valid: this.valid(),
            };
            this.onInput(payload);
            dispatchEvent(this.$element[0], 'input', payload);
        }
        submit() {
            const value = this.value();
            const valid = this.valid();
            if(value && valid) {
                const txtEl = this.textField();
                if(txtEl) {
                    txtEl.value = "";
                    txtEl.focus(); //refocus so people can keep writing
                }
                const payload = { value, valid };
                this.onSubmit(payload);
                dispatchEvent(this.$element[0], 'submit', payload);
            }
        }
        charactersLeft() {
            return this.maxChars - this.value().length;
        }
        valid() {
            return !this.maxChars || this.charactersLeft() >= 0;
        }
        value() {
            const txtEl = this.textField();
            if(txtEl) {
                return txtEl.value.trim();
            }
        }

        textFieldNg() {
            return angular.element(this.textField())
        }
        textField() {
            if(!this._textField) {
                this._textField = this.$element[0].querySelector('.wdt__text');
            }
            return this._textField;
        }
    }
    Controller.$inject = serviceDependencies;


    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            placeholder: '=', // NOTE: bound only once
            maxChars: '=',
            /*
             * Usage:
             *  on-input="::myCallback(value, valid)"
             */
            onInput: '&',
            /*
             * Usage:
             *  on-paste="::myCallback(value, valid)"
             */
            onPaste: '&',

            submitButtonLabel: '=',
            /*
             * Usage:
             *  on-submit="::myCallback(value)"
             */
            onSubmit: '&',

        },
        template: template
    }
}


export default angular.module('won.owner.components.chatTextfieldSimple', [ 
    autoresizingTextareaModule,
])
    .directive('chatTextfieldSimple', genComponentConf)
    .name;