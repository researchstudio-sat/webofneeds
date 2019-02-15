module Element.Background.Styled exposing (color)

import Color exposing (Color)
import Element
import Element.Background as Background
import Element.Styled as Element exposing (..)


color : Color -> Attr decorative msg
color col =
    let
        elmUiColor =
            Color.toRgba col |> Element.fromRgb
    in
    pureAttr <| Background.color elmUiColor
