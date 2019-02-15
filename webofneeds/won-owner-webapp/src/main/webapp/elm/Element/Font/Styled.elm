module Element.Font.Styled exposing
    ( center
    , color
    , size
    )

import Color exposing (Color)
import Element
import Element.Font as Font
import Element.Styled exposing (..)


size : Int -> Attr decorative msg
size len =
    pureAttr <| Font.size len


color : Color -> Attr decorative msg
color col =
    let
        elmUiColors =
            Color.toRgba col |> Element.fromRgb
    in
    pureAttr <| Font.color elmUiColors


center : Attribute msg
center =
    pureAttr Font.center
