module Settings exposing (main)

import Element exposing (..)
import Element.Background as Background
import Element.Events as Events
import Element.Font as Font
import Html exposing (Html)
import Settings.Account as Account
import Settings.Personas as Personas
import Skin exposing (Skin)


main =
    Skin.skinnedElement
        { init = init
        , update = update
        , view = view
        , subscriptions = subscriptions
        }



---- MODEL ----


type Model
    = Personas Personas.Model
    | Account Account.Model


type Route
    = PersonasR
    | AccountR


toRoute : Model -> Route
toRoute model =
    case model of
        Personas _ ->
            PersonasR

        Account _ ->
            AccountR


init : () -> ( Model, Cmd Msg )
init () =
    let
        ( model, cmd ) =
            Account.init ()
    in
    ( Account model, Cmd.map AccountMsg cmd )


subInit : (model -> Model) -> (msg -> Msg) -> ( model, Cmd msg ) -> ( Model, Cmd Msg )
subInit modelTag msgTag ( model, cmd ) =
    ( modelTag model
    , Cmd.map msgTag cmd
    )



---- UPDATE ----


type Msg
    = PersonasMsg Personas.Msg
    | AccountMsg Account.Msg
    | ChangeRoute Route


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case ( msg, model ) of
        ( PersonasMsg subMsg, Personas subModel ) ->
            Personas.update subMsg subModel
                |> updateWith Personas PersonasMsg model

        ( AccountMsg subMsg, Account subModel ) ->
            Account.update subMsg subModel
                |> updateWith Account AccountMsg model

        ( ChangeRoute newRoute, _ ) ->
            if toRoute model == newRoute then
                ( model, Cmd.none )

            else
                changeRoute newRoute

        ( _, _ ) ->
            ( model, Cmd.none )


changeRoute : Route -> ( Model, Cmd Msg )
changeRoute route =
    case route of
        AccountR ->
            subInit Account AccountMsg (Account.init ())

        PersonasR ->
            subInit Personas PersonasMsg (Personas.init ())


updateWith : (subModel -> Model) -> (subMsg -> Msg) -> Model -> ( subModel, Cmd subMsg ) -> ( Model, Cmd Msg )
updateWith toModel toMsg model ( subModel, subCmd ) =
    ( toModel subModel
    , Cmd.map toMsg subCmd
    )



---- VIEW ----


view : Skin -> Model -> Html Msg
view skin model =
    layout [] <|
        let
            viewPage toMsg viewFunc viewModel =
                row [ width fill ]
                    [ navigation skin (toRoute model)
                    , Element.map toMsg (viewFunc skin viewModel)
                    ]
        in
        case model of
            Personas subModel ->
                viewPage PersonasMsg Personas.view subModel

            Account subModel ->
                viewPage AccountMsg Account.view subModel


navigation : Skin -> Route -> Element Msg
navigation skin route =
    let
        navItem targetRoute title =
            let
                ( bgColor, textColor ) =
                    if targetRoute == route then
                        ( skin.primaryColor, Skin.white )

                    else
                        ( Skin.setAlpha 0 Skin.white, Skin.black )
            in
            el
                [ Background.color bgColor
                , Font.color textColor
                , width fill
                , Events.onClick (ChangeRoute targetRoute)
                , padding 10
                ]
            <|
                text title
    in
    column
        [ alignTop
        , padding 20
        ]
        [ navItem AccountR "Account"
        , navItem PersonasR "Personas"
        ]



---- SUBSCRIPTIONS ----


subscriptions : Model -> Sub Msg
subscriptions model =
    case model of
        Personas subModel ->
            Personas.subscriptions subModel
                |> Sub.map PersonasMsg

        Account _ ->
            Sub.none
