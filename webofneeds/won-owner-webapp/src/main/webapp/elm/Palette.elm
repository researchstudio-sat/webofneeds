module Palette exposing (wonButton)

import Html exposing (..)
import Html.Attributes as HA


wonButton : List (Attribute msg) -> List (Html msg) -> Html msg
wonButton attrs children =
    button
        (attrs
            ++ [ HA.class "secondary"
               , HA.class "won-button--filled"
               ]
        )
        children
