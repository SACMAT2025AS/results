% --- state ---

:- dynamic user/2.          % name, operative
:- dynamic role/2.          % name, operative
:- dynamic file/3.          % filename, threshold, operative
:- dynamic userRole/2.      % user, role
:- dynamic perm/3.          % role, operation, file


% --- imports ---

:- ensure_loaded("utils.pro").
:- ensure_loaded("cac.pro").
:- ensure_loaded("centralized.pro").
:- ensure_loaded("securitymodel.pro").
:- ensure_loaded("operations.pro").
:- ensure_loaded("consistency.pro").


% --- state-change rules ---

:- dynamic rotateKey/1.


% utils


assertPredicate(PRED, ENTITY) :-
    FACT =.. [PRED, ENTITY],
    FACT.

assertPredicate(PRED, ENTITY) :-
    FACT =.. [PRED, ENTITY],
    assert(FACT).


retractPredicate(PRED, ENTITY) :-
    FACT =.. [PRED, ENTITY],
    \+ FACT.

retractPredicate(PRED, ENTITY) :-
    FACT =.. [PRED, ENTITY],
    retractall(FACT).


retractallPredicates(ENTITY) :- 
    foreach(
        predicate(PRED),
        (
            FACT =.. [PRED, ENTITY],
            retractall(FACT);
            true
        )
    ).


% real

init :-
    assert(user(admin, true)),
    assert(role(admin, true)),
    assert(userRole(admin, admin)),

    time(addUserT(admin)),
    time(addRoleT(admin)),
    time(assignUserToRoleT(admin, admin)), 

    time(initAdmC).
    % verifyConsistency.


addUser(USER, []) :-
    \+ user(USER, _),
    assert(user(USER, true)),
    time(addUserT(USER)),
    time(addUserC(USER)).
    % updateUnderlying.

addUser(USER, [PR|PRs]) :-
    \+ user(USER, _),
    assertPredicate(PR, USER),
    addUser(USER, PRs).


deleteUser(USER) :-
    user(USER, true),

    % revoke all roles
    foreach(
        (
            role(ROLE, true),
            userRole(USER, ROLE)
        ),
        (
            revokeUserFromRoleC(USER, ROLE), 
            colludingProne(USER) ->
            (
                time(rotateRoleKeyUserRoleC(ROLE))
            );
            true
        )
    ),

    % delete
    time(deleteUserT(USER)),
    time(deleteUserC(USER)),

    % reencrypt
    foreach(
        (
            file(FILE, _, true), operation(OP), 
            canDo(USER, OP, FILE), enc(FILE)
        ),
        (
            (role(ROLE, true), userRole(USER, ROLE), 
                (cspNoEnforce(FILE), colludingProne(USER)) ->
                time(prepareReencryptionC(FILE)); true),
            (role(ROLE, true), userRole(USER, ROLE), 
                (cspNoEnforce(FILE), colludingProne(USER), eager(FILE)) ->
                time(reencryptResourceC(FILE)); true)
        )
    ),

    % rotate role key
    % foreach(
    %     (
    %         role(ROLE, true),
    %         userRole(USER, ROLE)
    %     ),
    %     (
    %         isRoleKeyRotationNeededOnRUR(USER, ROLE) ->
    %             time(rotateRoleKeyPermissionsC(ROLE));
    %         true
    %     )
    % ),
    foreach(
        (
            role(ROLE, true),
            userRole(USER, ROLE)
        ),
        (
            colludingProne(USER) -> time(rotateRoleKeyPermissionsC(ROLE));
            true
        )
    ),

    retractall(userRole(USER, _)),
    retractallPredicates(USER),
    retractall(user(USER, _)),
    assert(user(USER, false)),

    updateUnderlyingRoleKeyRotationUserRole,
    updateUnderlyingResourceKeyRotationOnRevUR,
    updateUnderlyingEagerReencryptOnRevUR,
    updateUnderlyingRoleKeyRotationPermissions.


addRole(ROLE, []) :-
    \+ role(ROLE, _),

    assert(role(ROLE, true)),
    assert(userRole(admin, ROLE)),

    time(addRoleT(ROLE)),
    time(addRoleC(ROLE)),
    time(assignUserToRoleT(admin, ROLE)).

addRole(ROLE, [PR|PRs]) :-
    \+ role(ROLE, _),

    assertPredicate(PR, ROLE),
    addRole(ROLE, PRs).


deleteRole(ROLE) :-
    role(ROLE, true),

    % revoke permissions
    foreach(
        (
            file(FILE, _, true), operation(OP), 
            perm(ROLE, OP, FILE), enc(FILE)
        ),
        time(revokePermissionFromRoleC(ROLE, [OP], FILE))
    ),

    % reencrypt files
    foreach(
        file(FILE, _, true),
        (
            operation(OP), 
            perm(ROLE, OP, FILE), 
            enc(FILE),
            isReencryptionNeededOnRP(ROLE, OP, FILE),
            time(prepareReencryptionC(FILE));
            true
        )
    ),
    foreach(
        file(FILE, _, true),
        (
            operation(OP), 
            perm(ROLE, OP, FILE), 
            enc(FILE),
            isEagerReencNeededOnRP(ROLE, OP, FILE),
            time(reencryptResourceC(FILE));
            true
        )
    ),

    % revoke users
    foreach(
        (
            userRole(USER, ROLE),
            user(USER, true)
        ),
        time(revokeUserFromRoleC(USER, ROLE))
    ),

    time(deleteRoleT(ROLE)),
    time(deleteRoleC(ROLE)),

    retractall(userRole(_, ROLE)),
    retractall(perm(ROLE, _, _)),
    retractallPredicates(ROLE),
    retractall(role(ROLE, _)),
    assert(role(ROLE, false)),

    updateUnderlyingResourceKeyRotationOnRevUR,
    updateUnderlyingEagerReencryptOnRevUR,
    updateUnderlyingResourceKeyRotationOnRevP,
    updateUnderlyingEagerReencryptOnRevP.


addResource(USER, FILE, [], THRESHOLD) :-
    \+ file(FILE, _, _),

    assert(file(FILE, THRESHOLD, true)),
    foreach(
        operation(OP),
        assert(perm(admin, OP, FILE))
    ),

    (enc(FILE) -> (
        addResourceC(FILE),
        time(writeResourceC(USER, FILE))
    ); true),

    addResourceT(FILE),
    foreach(
        operation(OP),
        time(assignPermissionToRoleT(admin, [OP], FILE))
    ),

    updateUnderlyingEncryption.

addResource(USER, FILE, [PR|PRs], THRESHOLD) :-
    \+ file(FILE, _, _),

    assertPredicate(PR, FILE),
    time(addResource(USER, FILE, PRs, THRESHOLD)).


deleteResource(FILE) :-
    file(FILE, _, true),

    time(deleteResourceT(FILE)),
    
    (enc(FILE) -> (
        foreach(
            perm(ROLE, OP, FILE),
            (
                time(revokePermissionFromRoleC(ROLE, [OP], FILE));
                true
            )
        ),
        time(deleteResourceC(FILE))
    ); true),

    retractall(file(FILE, _, _)),
    assert(file(FILE, 0, false)),
    retractall(perm(_, _, FILE)),
    retractallPredicates(FILE).


assignUserToRole(USER, ROLE) :- user(USER, true), role(ROLE, true), userRole(USER, ROLE).
assignUserToRole(USER, ROLE) :-
    user(USER, true), role(ROLE, true),

    assert(userRole(USER, ROLE)),

    time(assignUserToRoleT(USER, ROLE)),
    time(assignUserToRoleC(USER, ROLE)).


revokeUserFromRole(USER, ROLE) :- user(USER, true), role(ROLE, true), \+ userRole(USER, ROLE).
revokeUserFromRole(USER, ROLE) :-
    user(USER, true), role(ROLE, true),

    time(revokeUserFromRoleT(USER, ROLE)),
    time(revokeUserFromRoleC(USER, ROLE)),

    % rotate role key
    (colludingProne(USER) ->
        time(rotateRoleKeyUserRoleC(ROLE)); true),

    % file reencryption
    foreach(
        file(FILE, _, true),
        (
            operation(OP),
            perm(ROLE, OP, FILE), 
            enc(FILE),
            cspNoEnforce(FILE), colludingProne(USER),
            time(prepareReencryptionC(FILE));
            true
        )
    ),
    foreach(
        file(FILE, _, true),
        (
            operation(OP),
            perm(ROLE, OP, FILE), 
            enc(FILE),
            cspNoEnforce(FILE), colludingProne(USER), eager(FILE),
            time(reencryptResourceC(FILE));
            true
        )
    ),

    % rotate role key
    (colludingProne(USER) ->
        time(rotateRoleKeyPermissionsC(ROLE)); true),

    retractall(userRole(USER, ROLE)),

    updateUnderlyingRoleKeyRotationUserRole,
    updateUnderlyingResourceKeyRotationOnRevUR,
    updateUnderlyingEagerReencryptOnRevUR,
    updateUnderlyingRoleKeyRotationPermissions.


assignPermissionToRole(_, [], _).

assignPermissionToRole(ROLE, [OP|OPs], FILE) :-
    role(ROLE, true), file(FILE, _, true),

    perm(ROLE, OP, FILE),
    assignPermissionToRole(ROLE, OPs, FILE).

assignPermissionToRole(ROLE, [OP|OPs], FILE) :-
    \+ perm(ROLE, OP, FILE),

    assert(perm(ROLE, OP, FILE)),

    time(assignPermissionToRoleT(ROLE, [OP], FILE)),
    (enc(FILE) ->
        time(assignPermissionToRoleC(ROLE, [OP], FILE)); true),

    assignPermissionToRole(ROLE, OPs, FILE).


revokePermissionFromRoleInt(_, [], FILE, REENC, EAG_REENC) :-
    (
        (
            enc(FILE),
            REENC == true
        ) ->
        time(prepareReencryptionC(FILE)); true
    ),
    (
        (
            enc(FILE),
            EAG_REENC == true
        ) ->
        time(reencryptResourceC(FILE)); true
    ),

    updateUnderlyingResourceKeyRotationOnRevP,
    updateUnderlyingEagerReencryptOnRevP.

revokePermissionFromRoleInt(ROLE, [OP|OPs], FILE, REENC, EAG_REENC) :-
    time(revokePermissionFromRoleT(ROLE, [OP], FILE)),

    (enc(FILE) -> 
        time(revokePermissionFromRoleC(ROLE, [OP], FILE)); true),

    retractall(perm(ROLE, OP, FILE)),

    (isReencryptionNeededOnRP(ROLE, OP, FILE) -> REENC = true; true),
    (isEagerReencNeededOnRP(ROLE, OP, FILE) -> EAG_REENC = true; true),

    revokePermissionFromRoleInt(ROLE, OPs, FILE, REENC, EAG_REENC).

revokePermissionFromRole(ROLE, OPs, FILE) :-
    role(ROLE, true), file(FILE, _, true),
    revokePermissionFromRoleInt(ROLE, OPs, FILE, _, _).


% assignPredicate(PRED, ENTITY) :-
%     assertPredicate(PRED, ENTITY),
%     updateUnderlying.

% revokePredicate(PRED, ENTITY) :-
%     retractPredicate(PRED, ENTITY),
%     updateUnderlying.


readResource(USER, FILE) :-
    canDoT(USER, read, FILE),
    (( enc(FILE) -> time(readResourceC(USER, FILE)) ); true).


writeResource(USER, FILE) :-
    canDoT(USER, write, FILE),
    (( enc(FILE) -> time(writeResourceC(USER, FILE)) ); true).


setThreshold(FILE, THRESHOLD) :-
    retractall(file(FILE, _, true)),
    assert(file(FILE, THRESHOLD, true)).
    % TODO: append


updateUnderlyingEncryption :- 
    % file that need encryption
    foreach(
        (
            enc(FILE), 
            \+ isProtectedWithCAC(FILE)
        ),
        (
            time(addResourceC(FILE)),
            foreach(
                perm(ROLE, OP, FILE),
                time(assignPermissionToRoleC(ROLE, [OP], FILE))
            )
        )
    ).

updateUnderlyingDecryption :-
    % file that no longer need encryption
    foreach(
        (
            \+ enc(FILE),
            isProtectedWithCAC(FILE)
        ),
        (
            foreach(
                perm(ROLE, OP, FILE),
                time(revokePermissionFromRoleC(ROLE, [OP], FILE))
            ),
            time(deleteResourceC(FILE))
        )
    ).

% updateUnderlyingRoleKeyRotationUserRole :-
%     user(USER, _),
%     role(ROLE, _),
%     isRoleKeyRotationNeededOnRUR(USER, ROLE),
%     \+ canUserBeC(USER, ROLE),
%     canUserBeCacheC(USER, ROLE),
%     assert(rotateKey(ROLE)),
%     time(rotateRoleKeyUserRoleC(ROLE)),
%     false.
% updateUnderlyingRoleKeyRotationUserRole.

updateUnderlyingRoleKeyRotationUserRole :-
    % role key rotation
    foreach(
        (
            user(USER, _),
            role(ROLE, _)
        ),
        (
            (
                colludingProne(USER),
                \+ canUserBeC(USER, ROLE),
                canUserBeCacheC(USER, ROLE),
                assert(rotateKey(ROLE)),
                time(rotateRoleKeyUserRoleC(ROLE))
            );
            true
        )
    ).

% updateUnderlyingResourceKeyRotationOnRevUR :-
%     user(USER, _),
%     file(FILE, _, _),
%     role(ROLE, _),
%     operation(OP),
%     isReencryptionNeededOnRUR(USER, ROLE, OP, FILE),
%     \+ canUserDoC(USER, OP, FILE),
%     canUserDoViaRoleCacheLastC(USER, ROLE, OP, FILE),
%     time(prepareReencryptionC(FILE)),
%     false.
% updateUnderlyingResourceKeyRotationOnRevUR.

updateUnderlyingResourceKeyRotationOnRevUR :-
    % reencryption on revoke user role
    foreach(
        (
            user(USER, _),
            enc(FILE)
        ),
        (
            (
                role(ROLE, _),
                cspNoEnforce(FILE), 
                colludingProne(USER),
                operation(OP),
                \+ canUserDoC(USER, OP, FILE),
                canUserDoViaRoleCacheLastC(USER, ROLE, OP, FILE),
                time(prepareReencryptionC(FILE))
            );
            true
        )
    ).

% updateUnderlyingEagerReencryptOnRevUR :-
%     user(USER, _),
%     file(FILE, _, _),
%     role(ROLE, _),
%     isEagerReencNeededOnRUR(USER, ROLE, OP, FILE),
%     enc(FILE),
%     operation(OP),
%     \+ canUserDoC(USER, OP, FILE),
%     canUserDoViaRoleCacheC(USER, ROLE, OP, FILE),
%     time(reencryptResourceC(FILE)),
%     false.
% updateUnderlyingEagerReencryptOnRevUR.

updateUnderlyingEagerReencryptOnRevUR :-
    % eager reencryption on revoke user role
    foreach(
        (
            user(USER, _),
            enc(FILE)
        ),
        (
            (
                role(ROLE, _),
                cspNoEnforce(FILE), colludingProne(USER), eager(FILE),
                operation(OP),
                \+ canUserDoC(USER, OP, FILE),
                canUserDoViaRoleCacheC(USER, ROLE, OP, FILE),
                time(reencryptResourceC(FILE))
            );
            true
        )
    ).

% updateUnderlyingResourceKeyRotationOnRevP :-
%     role(ROLE, _),
%     file(FILE, _, _),
%     operation(OP),
%     isReencryptionNeededOnRP(ROLE, OP, FILE),
%     enc(FILE),
%     \+ canRoleDoC(ROLE, OP, FILE),
%     canRoleDoCacheLastC(ROLE, OP, FILE),
%     time(prepareReencryptionC(FILE)),
%     false.
% updateUnderlyingResourceKeyRotationOnRevP.

updateUnderlyingResourceKeyRotationOnRevP :-
    % reencryption on revoke permission
    foreach(
        (
            role(ROLE, _),
            enc(FILE)
        ),
        (
            (
                operation(OP),
                isReencryptionNeededOnRP(ROLE, OP, FILE),
                \+ canRoleDoC(ROLE, OP, FILE),
                canRoleDoCacheLastC(ROLE, OP, FILE),
                time(prepareReencryptionC(FILE))
            );
            true
        )
    ).

% updateUnderlyingEagerReencryptOnRevP :-
%     role(ROLE, _),
%     file(FILE, _, _),
%     operation(OP),
%     isEagerReencNeededOnRP(ROLE, OP, FILE),
%     enc(FILE),
%     \+ canRoleDoC(ROLE, OP, FILE),
%     canRoleDoCacheC(ROLE, OP, FILE),
%     time(reencryptResourceC(FILE)),
%     false.
% updateUnderlyingEagerReencryptOnRevP.

updateUnderlyingEagerReencryptOnRevP :-
    % eager reencryption on revoke permission
    foreach(
        (
            role(ROLE, _),
            enc(FILE)
        ),
        (
            (
                operation(OP),
                isEagerReencNeededOnRP(ROLE, OP, FILE), 
                \+ canRoleDoC(ROLE, OP, FILE),
                canRoleDoCacheC(ROLE, OP, FILE),
                time(reencryptResourceC(FILE))
            );
            true
        )
    ).

% updateUnderlyingRoleKeyRotationPermissions :-
%     rotateKey(ROLE),
%     time(rotateRoleKeyPermissionsC(ROLE)),
%     retractall(rotateKey(ROLE)),
%     false.
% updateUnderlyingRoleKeyRotationPermissions.


updateUnderlyingRoleKeyRotationPermissions :-
    % rotate role key (2)
    foreach(
        rotateKey(ROLE),
        (
            time(rotateRoleKeyPermissionsC(ROLE))
        );
        true
    ),
    retractall(rotateKey(ROLE)).

    % % verify canDo consistency
    % verifyConsistency.


% % --- queries ---

canDo(USER, OP, FILE) :-
    % user(USER, true), role(ROLE, true), 
    userRole(USER, ROLE),
    perm(ROLE, OP, FILE).


% --- debug ---

% :- init.
% :- addUser(simone, []), initUserC(simone).
% :- addUser(alessandro, [colludingProne]), initUserC(alessandro).

% :- addRole(staff, []),
%     assignUserToRole(simone, staff),
%     assignUserToRole(alessandro, staff).

% :- addResource(admin, bilancio, [], 10).
% :- addResource(admin, presenze, [enc], 10).
% :- addResource(admin, stipendi, [enc, cspNoEnforce], 10).
% :- addResource(admin, navicella, [enc, eager, cspNoEnforce], 10).
% :- assignPermissionToRole(staff, [read, write], bilancio).
% :- assignPermissionToRole(staff, [read, write], presenze).
% :- assignPermissionToRole(staff, [read, write], stipendi).
% :- assignPermissionToRole(staff, [read, write], navicella).

