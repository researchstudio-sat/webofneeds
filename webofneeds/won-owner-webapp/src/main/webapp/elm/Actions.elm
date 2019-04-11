port module Actions exposing (connectPersona, emitEvent)

import Json.Encode as Encode exposing (Value)
import Persona exposing (Persona)


port outPort : { action : String, payload : Value } -> Cmd msg


emitEvent :
    { eventName : String
    , payload : Value
    }
    -> Cmd msg
emitEvent { eventName, payload } =
    outPort
        { action = eventName
        , payload = payload
        }


connectPersona :
    { persona : Persona
    , needUrl : String
    }
    -> Cmd msg
connectPersona { persona, needUrl } =
    outPort
        { action = "personas__connect"
        , payload =
            Encode.list Encode.string
                [ needUrl
                , persona.uri
                ]
        }
