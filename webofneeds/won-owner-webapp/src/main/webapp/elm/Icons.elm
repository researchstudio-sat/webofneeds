module Icons exposing
    ( arrowDown
    , arrowUp
    , icon
    , identicon
    , plus
    )

import Color exposing (Color)
import Html exposing (Attribute, Html)
import Html.Attributes as HA


type Icon
    = Icon String


icon : Icon -> Color -> Html msg
icon (Icon name) color =
    Html.node "won-svg-icon"
        [ HA.attribute "icon" name
        , HA.attribute "color" (Color.toCssString color)
        , HA.style "width" "100%"
        , HA.style "height" "100%"
        ]
        []



-- icon : Icon -> Color -> Element msg
-- icon name color =
--     el
--         [ width fill
--         , height fill
--         , htmlAttribute <| HA.style "height" "100%"
--         ]
--     <|
--         html <|
--             htmlIcon name color


arrowDown =
    Icon "ico16_arrow_down"


arrowUp =
    Icon "ico16_arrow_up"


plus =
    Icon "ico36_plus"


identicon : List (Attribute msg) -> String -> Html msg
identicon attrs data =
    Html.node "won-identicon"
        (attrs
            ++ [ HA.attribute "data" data
               , HA.style "width" "100%"
               , HA.style "height" "100%"
               ]
        )
        []
