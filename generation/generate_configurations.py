import random
import string
import math

scienarios = {
    "domino": {
        "size": {
            "users": 79,
            "roles": 20,
            "resources": 231,
            "ur": 75,
            "pa": 629
        },
        "constraints": {
            "roles-user": {
                "max": 3,
                "min": 0
            },
            "users-role": {
                "max": 30,
                "min": 1
            },
            "permissions-role": {
                "max": 209,
                "min": 1
            },
            "roles-permission": {
                "max": 10,
                "min": 1
            }
        }
    },
    # "emea": {
    #     "size": {
    #         "users": 35,
    #         "roles": 34,
    #         "resources": 3046,
    #         "ur": 35,
    #         "pa": 7211,
    #     },
    #     "constraints": {
    #         "roles-user": {
    #             "max": 1,
    #             "min": 1
    #         },
    #         "users-role": {
    #             "max": 2,
    #             "min": 1
    #         },
    #         "permissions-role": {
    #             "max": 554,
    #             "min": 9
    #         },
    #         "roles-permission": {
    #             "max": 31,
    #             "min": 1
    #         }
    #     }
    # },
    # "firewall1": {
    #     "size": {
    #         "users": 365,
    #         "roles": 60,
    #         "resources": 709,
    #         "ur": 1130,
    #         "pa": 3455
    #     },
    #     "constraints": {
    #         "roles-user": {
    #             "max": 14,
    #             "min": 0
    #         },
    #         "users-role": {
    #             "max": 175,
    #             "min": 1
    #         },
    #         "permissions-role": {
    #             "max": 617,
    #             "min": 1
    #         },
    #         "roles-permission": {
    #             "max": 25,
    #             "min": 1
    #         }
    #     }
    # },
    # "firewall2": {
    #     "size": {
    #         "users": 325,
    #         "roles": 10,
    #         "resources": 590,
    #         "ur": 325,
    #         "pa": 1136
    #     },
    #     "constraints": {
    #         "roles-user": {
    #             "max": 1,
    #             "min": 1
    #         },
    #         "users-role": {
    #             "max": 222,
    #             "min": 1
    #         },
    #         "permissions-role": {
    #             "max": 590,
    #             "min": 6
    #         },
    #         "roles-permission": {
    #             "max": 8,
    #             "min": 1
    #         }
    #     }
    # },
    # "healthcare": {
    #     "size": {
    #         "users": 46,
    #         "roles": 13,
    #         "resources": 46,
    #         "ur": 55,
    #         "pa": 359
    #     },
    #     "constraints": {
    #         "roles-user": {
    #             "max": 5,
    #             "min": 1
    #         },
    #         "users-role": {
    #             "max": 17,
    #             "min": 1
    #         },
    #         "permissions-role": {
    #             "max": 45, 
    #             "min": 7
    #         },
    #         "roles-permission": {
    #             "max": 12,
    #             "min": 1
    #         }
    #     }
    # },
    # "university": {
    #     "size": {
    #         "users": 493,
    #         "roles": 16,
    #         "resources": 56,
    #         "ur": 495,
    #         "pa": 202
    #     },
    #     "constraints": {
    #         "roles-user": {
    #             "max": 2,
    #             "min": 1
    #         },
    #         "users-role": {
    #             "max": 288,
    #             "min": 1
    #         },
    #         "permissions-role": {
    #             "max": 40,
    #             "min": 2
    #         },
    #         "roles-permission": {
    #             "max": 12,
    #             "min": 1
    #         }
    #     }
    # }
}

ops = ['read', 'write']
limit = 100000
number_scr = 100

def randName(n=10):
    return ''.join(random.choice(string.ascii_lowercase) for _ in range(n))

def generateScienario(s, sname):
    # state

    users = ['user_' + randName() for _ in range(s['size']['users'])]
    roles = ['roles_' + randName() for _ in range(s['size']['roles'])]
    resources = ['resources_' + randName() for _ in range(s['size']['resources'])]
    userRoles = set()
    rolePermissions = set()


    # define functions for suitable assignments
    allUserRoles = []
    for u in users:
        for r in roles:
            allUserRoles.append((u, r))
    random.shuffle(allUserRoles)
    def suitableUserRole(fuser = None, frole = None):    
        for (user, role) in allUserRoles:
            if fuser is not None and user != fuser:
                continue
            if frole is not None and role != frole:
                continue
            # user = fuser if fuser is not None else random.choice(users)
            # role = frole if frole is not None else random.choice(roles)

            if (user, role) in userRoles:
                continue

            nuser = sum([1 if (user, r) in userRoles else 0 for r in roles])
            nrole = sum([1 if (u, role) in userRoles else 0 for u in users])
            allUserRoles.remove((user, role))

            if(nuser < s['constraints']['users-role']['max'] and
               nrole < s['constraints']['roles-user']['max']):
                break
        else: 
            # print("skipped")
            return None, None
        
        return user, role
    
    allRolePermissions = []
    for r in roles:
        for f in resources:
            for o in ops:
                allRolePermissions.append((r, o, f))
    print(len(allRolePermissions))
    random.shuffle(allRolePermissions)
    def suitableRolePermissions(frole = None, fop = None, fresource = None):
        # print("---"*10)
        for (role, op, resource) in allRolePermissions:
            # role = frole if frole is not None else random.choice(roles)
            # resource = fresource if fresource is not None else random.choice(resources)
            # op = fop if fop is not None else random.choice(ops)
            if frole is not None and role != frole:
                continue
            if fop is not None and op != fop:
                continue
            if fresource is not None and resource != fresource:
                # if resource == 'resources_sepbqzhldr':
                #     print("Skipped {} {} {}".format(role, op, resource))
                continue

            if (role, op, resource) in rolePermissions:
                continue

            nrole = sum([1 if (role, o, f) in rolePermissions else 0 for o in ops for f in resources])
            nresources = sum([1 if (r, o, resource) in rolePermissions else 0 for o in ops for r in roles])
            print("{}".format(len(allRolePermissions)))
            allRolePermissions.remove((role, op, resource))

            if(nrole < s['constraints']['roles-permission']['max'] and
               nresources < s['constraints']['permissions-role']['max']):
                break
        else:
            # print("skipped")
            return None, None, None

        return role, op, resource


    # contraints (min)

    for r2 in roles:
        for i in range(s['constraints']['users-role']['min']):
            user, _ = suitableUserRole(frole=r2)
            if user is not None: 
                userRoles.add((user, r2))
    for u2 in users:
        for i in range(s['constraints']['roles-user']['min']):
            _, role = suitableUserRole(fuser=u2)
            if role is not None:
                userRoles.add((u2, role))

    for r2 in roles:
        for i in range(s['constraints']['permissions-role']['min']):
            _, op, resource = suitableRolePermissions(frole=r2)
            if resource is not None:
                rolePermissions.add((r2, op, resource))
    for f2 in resources:
        for i in range(s['constraints']['roles-permission']['min']):
            role, op, _ = suitableRolePermissions(fresource=f2)
            if role is not None: 
                rolePermissions.add((role, op, f2))


    # initialize assignements

    for i in range(s['size']['ur'] - len(userRoles)):
        print(i)
        user, role = suitableUserRole()
        if user is not None:
            userRoles.add((user, role))
    
    for i in range(s['size']['pa'] - len(rolePermissions)):
        print(i)
        role, op, resource = suitableRolePermissions()
        if role is not None: 
            rolePermissions.add((role, op, resource))


    # # generate random operations after the initial state
    # def generate_policy_updates(n = 100):
    p_users = users.copy()
    p_roles = roles.copy()
    p_resources = resources.copy()
    p_userRoles = list(userRoles.copy())
    p_rolePermissions = list(rolePermissions.copy())

    p_rules = []

    def addUser():
        username = 'user_' + randName()
        p_users.append(username)
        p_rules.append(('addUser', [username]))
        p_rules.append(('initUserC', [username]))
    
    def deleteUser():
        username = random.choice(p_users)
        p_users.remove(username)
        p_rules.append(('deleteUser', [username]))
        for r in p_roles:
            if (username, r) in p_userRoles:
                p_userRoles.remove((username, r))

    def addRole():
        name = 'role_' + randName()
        p_roles.append(name)
        p_rules.append(('addRole', [name]))

    def deleteRole():
        name = random.choice(p_roles)
        p_roles.remove(name)
        p_rules.append(('deleteRole', [name]))
        for u in p_users:
            if (u, name) in p_userRoles:
                p_userRoles.remove((u, name))
        for f in p_resources:
            for o in ops:
                if (name, o, f) in p_rolePermissions:
                    p_rolePermissions.remove((name, o, f))

    def addResource():
        name = 'file_' + randName()
        p_resources.append(name)
        p_rules.append(('addResource', ['admin', name]))
    
    def deleteResource():
        name = random.choice(p_resources)
        p_resources.remove(name)
        p_rules.append(('deleteResource', [name]))
        for r in p_roles:
            for o in ops:
                if (r, o, name) in p_rolePermissions:
                    p_rolePermissions.remove((r, o, name))

    def assignUserToRole():
        for _ in range(limit):
            user = random.choice(p_users)
            role = random.choice(p_roles)
            if (user, role) not in p_userRoles:
                break
        else: 
            raise Exception()

        p_userRoles.append((user, role))
        p_rules.append(('assignUserToRole', [user, role]))

    def revokeUserFromRole():
        (user, role) = random.choice(p_userRoles)
        p_userRoles.remove((user, role))
        p_rules.append(['revokeUserFromRole', [user, role]])

    def assignPermissionToRole():
        for _ in range(limit):
            role = random.choice(p_roles)
            op = random.choice(ops)
            resource = random.choice(p_resources)
            if (role, op, resource) not in p_rolePermissions:
                break
        else: 
            raise Exception()
        
        p_rolePermissions.append((role, op, resource))
        p_rules.append(('assignPermissionToRole', [role, [op], resource]))

    def revokePermissionFromRole():
        (role, op, resource) = random.choice(p_rolePermissions)
        p_rolePermissions.remove((role, op, resource))
        p_rules.append(('revokePermissionFromRole', [role, [op], resource]))

    def writeResource():
        resource = random.choice(p_resources)
        p_rules.append(('writeResource', ['admin', resource]))

    def readResource():
        resource = random.choice(p_resources)
        p_rules.append(('readResource', ['admin', resource]))

    p_rules = []
    for i in range(number_scr):
        func = random.choice([
            addUser, deleteUser,
            addRole, deleteRole,
            addResource, deleteResource,

            assignUserToRole, revokeUserFromRole,
            assignPermissionToRole, revokePermissionFromRole,

            readResource, writeResource
        ])
        func()
            
    def appply_predicates(p):
        random.seed(1)
        cac = random.sample(resources, k=math.floor(p * len(resources)))
        # eager = random.sample(resources, k=math.floor(p * len(resources)))
        # cspNoEnforce = random.sample(resources, k=math.floor(p * len(resources)))
        untrusted = random.sample(users, k=math.floor(p * len(users)))
        # print("hello={}/{} -> {}".format(math.floor(p * len(users)), len(users), len(untrusted)))

        with open('{}_{}.test.pro'.format(sname, int(100 * p)), 'w') as fs:
            def println(txt):
                fs.write(':- format(":- {}.~n", []).\n'.format(txt.replace('"', '')))
                fs.write(":- time({}).\n".format(txt))

            println('ensure_loaded("extended.pro")')
            println('ensure_loaded("consistency.pro")')
            println('init')

            # init state
            for u in users:
                println('addUser({}, [{}])'.format(
                    u,
                    'colludingProne' if u in untrusted else ''
                ))
                println('initUserC({})'.format(u))
            
            for r in roles:
                println('addRole({}, [])'.format(r))

            for f in resources:
                selected = random.choices(
                        [
                            ['enc'], 
                            ['enc', 'cspNoEnforce'], 
                            ['enc', 'eager'],
                            ['enc', 'cspNoEnforce', 'eager']
                        ],
                        [
                            0.25,
                            0.25,
                            0.25,
                            0.25
                        ],
                        k=1
                    )[0]
                preds = []
                if f in cac:
                    # preds.append('enc')
                    for e in selected:
                        preds.append(e)

                # if f in eager:
                #     preds.append('eager')
                # if f in cspNoEnforce:
                #     preds.append('cspNoEnforce')
                println('addResource({}, {}, [{}], 1)'.format(
                    'admin',
                    f,
                    ','.join(preds)
                ))

            for (u, r) in userRoles:
                println('assignUserToRole({}, {})'.format(u, r))

            for (r, o, f) in rolePermissions:
                println('assignPermissionToRole({}, [{}], {})'.format(r, o, f))
            
        
            # print additional state-change rules
            for (rule, oparams) in p_rules:
                params = oparams.copy()
                if rule == 'addUser':
                    params.append(random.choices(
                        [['colludingProne'], []],
                        [p, 1-p], k = 1)[0])
                elif rule == 'addResource':
                    params.append(random.choices(
                        [
                            [], 
                            ['enc'], 
                            ['enc', 'cspNoEnforce'], 
                            ['enc', 'eager'],
                            ['enc', 'cspNoEnforce', 'eager']
                        ],
                        [
                            1-p,
                            0.25*p,
                            0.25*p,
                            0.25*p,
                            0.25*p
                        ],
                        k=1
                    )[0])
                    params.append("1")
                elif rule == 'addRole':
                    params.append([])

                println(
                    '{}({})'.format(
                        rule,
                        ', '.join([
                            '[{}]'.format(', '.join(e))
                            if isinstance(e, list)
                            else e
                            for e in params
                        ])
                    )
                )

            println("halt")

    for prob in [0, .20, .40, .60, .80, 1]:
        appply_predicates(prob)

def main():
    # initializing seed
    for sname in scienarios:
        random.seed(0)
        generateScienario(scienarios[sname], sname)


if __name__ == '__main__':
    main()
