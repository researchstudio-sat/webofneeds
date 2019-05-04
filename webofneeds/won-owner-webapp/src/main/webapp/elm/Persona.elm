module Persona exposing
    ( Persona
    , connect
    , decoder
    , icon
    , inlineView
    , review
    )

import Actions
import Html exposing (Attribute, Html)
import Html.Attributes as Attributes
import Icons
import Json.Decode as Decode exposing (Decoder)
import Json.Decode.Extra as Decode
import Json.Decode.Pipeline as DP
import Json.Encode as Encode
import Time exposing (Posix)


type alias Persona =
    { uri : String
    , name : String
    , created : Posix
    }


type alias Review =
    { value : Int
    , message : String
    }



---- VIEW ----


icon : Persona -> Html msg
icon persona =
    Icons.identicon [ Attributes.class "won-persona-icon" ] persona.uri


inlineView : List (Attribute msg) -> Persona -> Html msg
inlineView attrs persona =
    Html.div
        ([ Attributes.classList
            [ ( "won-persona-list-entry", True )
            ]
         ]
            ++ attrs
        )
        [ icon persona
        , Html.span [ Attributes.class "won-persona-name" ]
            [ Html.text persona.name ]
        ]



---- DECODER ----


compareCheck : a -> Decoder a -> Decoder ()
compareCheck value dec =
    dec
        |> Decode.andThen
            (\decoded ->
                if decoded == value then
                    Decode.succeed ()

                else
                    Decode.fail "Unexpected value"
            )


checkDecoder : Decoder () -> Decoder a -> Decoder a
checkDecoder guard dec =
    guard
        |> Decode.andThen (\() -> dec)


decoder : Decoder Persona
decoder =
    Decode.succeed Persona
        |> DP.required "url" Decode.string
        |> DP.required "displayName" Decode.string
        |> DP.required "timestamp" Decode.datetime
        |> checkDecoder (Decode.field "saved" <| compareCheck True Decode.bool)



---- ACTIONS ----


connect :
    { persona : Persona
    , atomUrl : String
    }
    -> Cmd msg
connect { persona, atomUrl } =
    Actions.startAction
        { action = "personas__connect"
        , payload =
            Encode.list Encode.string
                [ atomUrl
                , persona.uri
                ]
        }


review :
    { connection : String
    , review : Review
    }
    -> Cmd msg
review data =
    let
        encodedReview =
            Encode.object
                [ ( "value", Encode.int data.review.value )
                , ( "message", Encode.string data.review.message )
                ]
    in
    Actions.startAction
        { action = "personas__review"
        , payload =
            Encode.list identity
                [ Encode.string data.connection
                , encodedReview
                ]
        }
