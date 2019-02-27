module AssocList.Extra exposing (filterMap)

import AssocList exposing (Dict)


filterMap : (key -> value1 -> Maybe value2) -> Dict key value1 -> Dict key value2
filterMap filter dict =
    AssocList.foldl
        (\key value state ->
            case filter key value of
                Just newValue ->
                    AssocList.insert key newValue state

                Nothing ->
                    state
        )
        AssocList.empty
        dict
