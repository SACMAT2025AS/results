% --- import ---

:- ensure_loaded("extended.pro").
:- ensure_loaded("securitymodel.pro").


% --- consistency ---

canDoTC(USER, OP, FILE) :-
    enc(FILE), 
    canUserDoC(USER, OP, FILE),
    canDoT(USER, OP, FILE).
canDoTC(USER, OP, FILE) :-
    \+ enc(FILE),
    canDoT(USER, OP, FILE).

verifyConsistency :-
    % canDo
    foreach(
        (
            canDo(USER, OP, FILE), 
            \+ canDoTC(USER, OP, FILE)
        ),
        ansi_format([fg(red)], "Access (~a, ~a, ~a) is possibile in extended but not in TC~n", [USER, OP, FILE])
    ),
    foreach(
        (
            canDoTC(USER, OP, FILE), 
            \+ canDo(USER, OP, FILE)
        ),
        ansi_format([fg(red)], "Access (~a, ~a, ~a) is possibile in TC but not in extended~n", [USER, OP, FILE])
    ).

