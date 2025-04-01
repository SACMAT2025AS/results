% --- imports ---

:- ensure_loaded("extended.pro").


% --- predicates ---

:- dynamic enc/1.               % file
:- dynamic eager/1.             % file
:- dynamic cspNoEnforce/1.      % file
:- dynamic colludingProne/1.    % user

predicate(enc).
predicate(eager).
predicate(cspNoEnforce).
predicate(colludingProne).


% --- queries ---

% Garrison et al

% isEncryptionNeeded(_) :- true.
% isRoleKeyRotationNeededOnRUR(_, _) :- true.
% isReencryptionNeededOnRP(_, _, _) :- true.
% isEagerReencNeededOnRP(_, _, _) :- false.
% isReencryptionNeededOnRUR(_, _, _, _) :- true.
% isEagerReencNeededOnRUR(_, _, _, _) :- false.


% our proposal

% isEncryptionNeeded(FILE) :- 
%     % file(FILE, _, _), 
%     enc(FILE).


% isRoleKeyRotationNeededOnRUR(USER, _) :-
%     % user(USER, _),
%     colludingProne(USER).


isReencryptionNeededOnRP(_, _, FILE) :-
    % role(ROLE, _), file(FILE, _, _),
    enc(FILE), cspNoEnforce(FILE),
    user(USER, _), colludingProne(USER), 
    operation(OP), canDo(USER, OP, FILE).

isEagerReencNeededOnRP(ROLE, OP, FILE) :-
    isReencryptionNeededOnRP(ROLE, OP, FILE),
    eager(FILE).

% isReencryptionNeededOnRUR(USER, _, _, FILE) :-
%     % user(USER, _), file(FILE, _, _),
%     enc(FILE), cspNoEnforce(FILE), colludingProne(USER).

isEagerReencNeededOnRUR(USER, _, _, FILE) :-
    enc(FILE), cspNoEnforce(FILE), colludingProne(USER),
    eager(FILE).

