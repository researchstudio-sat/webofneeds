module Element.Input.Styled exposing (button)

import Element.Input as Input
import Element.Styled as Element exposing (..)


button :
    List (Attribute msg)
    ->
        { onPress : Maybe msg
        , label : Element msg
        }
    -> Element msg
button attrs opts =
    (\_ label a ->
        Input.button a
            { label = label
            , onPress = opts.onPress
            }
    )
        |> getElement opts.label
        |> getAttrList attrs
        |> element
