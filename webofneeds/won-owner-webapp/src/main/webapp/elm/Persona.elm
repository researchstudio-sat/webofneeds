module Persona exposing
    ( Persona
    , decoder
    , icon
    , inlineView
    )

import Html exposing (Attribute, Html)
import Html.Attributes as Attributes
import Icons
import Json.Decode as Decode exposing (Decoder)
import Json.Decode.Extra as Decode
import Json.Decode.Pipeline as DP
import Json.Encode as Encode
import Time exposing (Posix)
import Widget exposing (Action)


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
