module Skin exposing
    ( Skin
    , black
    , cssColor
    , decoder
    , default
    , setAlpha
    , white
    )

import Element exposing (..)
import Json.Decode as Decode exposing (Decoder)


type alias Skin =
    { primaryColor : Color
    , lightGray : Color
    , lineGray : Color
    , subtitleGray : Color
    }


black : Color
black =
    rgb255 0 0 0


white : Color
white =
    rgb255 255 255 255


default : Skin
default =
    { primaryColor = rgb255 240 70 70
    , lightGray = rgb255 240 242 244
    , lineGray = rgb255 203 210 209
    , subtitleGray = rgb255 128 128 128
    }


cssColor : Color -> String
cssColor color =
    let
        { red, green, blue, alpha } =
            toRgb color

        to255 col =
            String.fromInt <| round (col * 255)

        colors =
            ([ red
             , green
             , blue
             ]
                |> List.map to255
            )
                ++ [ String.fromFloat alpha ]
    in
    "rgba("
        ++ String.join "," colors
        ++ ")"


setAlpha : Float -> Color -> Color
setAlpha alpha color =
    let
        oldRgb =
            toRgb color
    in
    fromRgb
        { oldRgb
            | alpha = alpha
        }


colorDecoder : Decoder Color
colorDecoder =
    Decode.list Decode.int
        |> Decode.andThen
            (\channels ->
                case channels of
                    [ r, g, b ] ->
                        Decode.succeed <| rgb255 r g b

                    _ ->
                        Decode.fail "Expected [r, g, b]"
            )


decoder : Decoder Skin
decoder =
    Decode.map4 Skin
        (Decode.field "primaryColor" colorDecoder)
        (Decode.field "lightGray" colorDecoder)
        (Decode.field "lineGray" colorDecoder)
        (Decode.field "subtitleGray" colorDecoder)
