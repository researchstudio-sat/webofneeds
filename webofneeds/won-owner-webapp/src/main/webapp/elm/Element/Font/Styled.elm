module Element.Font.Styled exposing
    ( center
    , color
    , size
    )

import Element.Font as Font
import Element.Styled exposing (..)


size : Int -> Attr decorative msg
size len =
    pureAttr <| Font.size len


color : Color -> Attr decorative msg
color col =
    pureAttr <| Font.color col


center : Attribute msg
center =
    pureAttr Font.center
