module Element.Border.Styled exposing
    ( color
    , roundEach
    , rounded
    , shadow
    , width
    )

import Color exposing (Color)
import Element
import Element.Border as Border
import Element.Styled as Element exposing (Attr, Attribute, pureAttr)


roundEach :
    { topLeft : Int
    , topRight : Int
    , bottomLeft : Int
    , bottomRight : Int
    }
    -> Attribute msg
roundEach corners =
    pureAttr <| Border.roundEach corners


rounded : Int -> Attribute msg
rounded radius =
    pureAttr <| Border.rounded radius


color : Color -> Attribute msg
color col =
    let
        elmUiColor =
            Color.toRgba col |> Element.fromRgb
    in
    pureAttr <| Border.color elmUiColor


width : Int -> Attribute msg
width w =
    pureAttr <| Border.width w


shadow :
    { offset : ( Float, Float )
    , size : Float
    , blur : Float
    , color : Color
    }
    -> Attr decorative msg
shadow opts =
    let
        elmUiColor =
            Color.toRgba opts.color |> Element.fromRgb
    in
    pureAttr <|
        Border.shadow
            { offset = opts.offset
            , size = opts.size
            , blur = opts.blur
            , color = elmUiColor
            }
