module Element.Border.Styled exposing
    ( roundEach
    , rounded
    )

import Element.Border as Border
import Element.Styled as Element exposing (Attribute, pureAttr)


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
