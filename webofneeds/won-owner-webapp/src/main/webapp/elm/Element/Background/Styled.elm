module Element.Background.Styled exposing (color)

import Element.Background as Background
import Element.Styled as Element exposing (..)


color : Color -> Attr decorative msg
color col =
    pureAttr <| Background.color col
