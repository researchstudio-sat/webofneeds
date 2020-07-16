module Elements exposing
    ( Button
    , identicon
    , mainButton
    , outlinedButton
    , svgIcon
    )

import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Font as Font
import Element.Input as Input
import Html exposing (node)
import Html.Attributes as HA
import Old.Skin as Skin exposing (Skin)


type alias Button msg =
    { label : Element Never
    , onPress : Maybe msg
    }


mainButton : Skin -> List (Attribute msg) -> Button msg -> Element msg
mainButton skin attr { label, onPress } =
    case onPress of
        Just handler ->
            Input.button
                ([ Background.color skin.secondaryColor
                 , Border.rounded 3
                 , paddingEach
                    { top = 10
                    , bottom = 10
                    , left = 15
                    , right = 15
                    }
                 , Font.color Skin.white
                 ]
                    ++ attr
                )
                { onPress = Just handler
                , label = Element.map never label
                }

        Nothing ->
            Input.button
                ([ Background.color (Skin.setAlpha 0.5 skin.secondaryColor)
                 , Border.rounded 3
                 , paddingEach
                    { top = 10
                    , bottom = 10
                    , left = 15
                    , right = 15
                    }
                 , Font.color Skin.white
                 ]
                    ++ attr
                )
                { onPress = Nothing
                , label = Element.map never label
                }


outlinedButton : Skin -> Button msg -> Element msg
outlinedButton skin { label, onPress } =
    case onPress of
        Just handler ->
            Input.button
                [ Border.color skin.secondaryColor
                , Border.rounded 3
                , paddingEach
                    { top = 10
                    , bottom = 10
                    , left = 15
                    , right = 15
                    }
                , Font.color skin.secondaryColor
                , Border.width 1
                ]
                { onPress = Just handler
                , label = Element.map never label
                }

        Nothing ->
            Input.button
                [ Border.color (Skin.setAlpha 0.5 skin.secondaryColor)
                , Border.rounded 3
                , paddingEach
                    { top = 10
                    , bottom = 10
                    , left = 15
                    , right = 15
                    }
                , Font.color (Skin.setAlpha 0.5 skin.secondaryColor)
                ]
                { onPress = Nothing
                , label = Element.map never label
                }


identicon : List (Attribute msg) -> String -> Element msg
identicon attributes string =
    el
        attributes
    <|
        html <|
            node "won-identicon"
                [ HA.attribute "data" string
                , HA.style "width" "100%"
                , HA.style "height" "100%"
                ]
                []


svgIcon :
    List (Attribute msg)
    ->
        { color : Color
        , name : String
        }
    -> Element msg
svgIcon attributes { color, name } =
    el attributes <|
        html <|
            Html.node "won-svg-icon"
                [ HA.attribute "icon" name
                , HA.attribute "color" (Skin.cssColor color)
                , HA.style "width" "100%"
                , HA.style "height" "100%"
                ]
                []
