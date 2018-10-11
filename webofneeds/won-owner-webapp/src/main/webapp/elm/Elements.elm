module Elements exposing (ButtonConfig, identicon, mainButton, outlinedButton)

import Element exposing (..)
import Element.Events as Events
import Html exposing (node)
import Html.Attributes as HA


type alias ButtonConfig msg =
    { disabled : Bool
    , text : String
    , onClick : msg
    }


mainButton : ButtonConfig msg -> Element msg
mainButton { disabled, text, onClick } =
    el [ Events.onClick onClick ] <|
        html <|
            Html.button
                [ HA.classList
                    [ ( "won-button--filled", True )
                    , ( "red", True )
                    ]
                , HA.disabled disabled
                ]
                [ Html.text text ]


outlinedButton : ButtonConfig msg -> Element msg
outlinedButton { disabled, text, onClick } =
    el [ Events.onClick onClick ] <|
        html <|
            Html.button
                [ HA.classList
                    [ ( "won-button--outlined", True )
                    , ( "thin", True )
                    , ( "red", True )
                    ]
                , HA.disabled disabled
                ]
                [ Html.text text ]


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
