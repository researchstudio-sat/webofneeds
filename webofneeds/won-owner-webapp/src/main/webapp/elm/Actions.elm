port module Actions exposing (connectPersona)

import Json.Encode as Encode exposing (Value)
import Url exposing (Url)


port outPort : { action : String, payload : Value } -> Cmd msg


connectPersona :
    { personaUrl : Url
    , needUrl : Url
    }
    -> Cmd msg
connectPersona { personaUrl, needUrl } =
    outPort
        { action = "personas__connect"
        , payload =
            Encode.list Encode.string
                [ Url.toString needUrl
                , Url.toString personaUrl
                ]
        }
