module Element.Extra exposing (toCssString)

import Element exposing (..)


toCssString : Color -> String
toCssString color =
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
