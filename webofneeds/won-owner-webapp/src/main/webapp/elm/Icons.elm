module Icons exposing
    ( arrowDown
    , arrowUp
    )

import Color exposing (Color)
import Element.Styled as Element exposing (..)
import Html
import Html.Attributes as HA


arrowDown =
    svgIcon "ico16_arrow_down"


arrowUp =
    svgIcon "ico16_arrow_up"


svgIcon : String -> Color -> Element msg
svgIcon name color =
    el
        [ width fill
        , height fill
        , htmlAttribute <| HA.style "height" "100%"
        ]
    <|
        html <|
            Html.node "won-svg-icon"
                [ HA.attribute "icon" name
                , HA.attribute "color" (Color.toCssString color)
                , HA.style "width" "100%"
                , HA.style "height" "100%"
                ]
                []



-- svgIcon :
--     List (Attribute msg)
--     ->
--         { color : Color
--         , name : String
--         }
--     -> Element msg
-- svgIcon attributes { color, name } =
--     el attributes <|
--         html <|
--             Html.node "won-svg-icon"
--                 [ HA.attribute "icon" name
--                 , HA.attribute "color" (Skin.cssColor color)
--                 , HA.style "width" "100%"
--                 , HA.style "height" "100%"
--                 ]
--                 []
