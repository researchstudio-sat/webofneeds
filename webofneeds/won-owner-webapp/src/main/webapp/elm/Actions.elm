port module Actions exposing (Action, Event, emitEvent, startAction)

import Json.Encode exposing (Value)



---- PORT ----


port outPort : Action -> Cmd msg



---- DATA ----


type alias Action =
    { action : String
    , payload : Value
    }


type alias Event =
    { eventName : String
    , payload : Value
    }



---- ACTIONS ----


emitEvent : Event -> Cmd msg
emitEvent { eventName, payload } =
    outPort
        { action = eventName
        , payload = payload
        }


startAction : Action -> Cmd msg
startAction action =
    outPort action
