# SACMAT 2025 (Submission #140)

The efficiency test consists of four phases:
1. generation;
2. Prolog execution;
3. Kotlin execution;
4. data collection and reporting.


## Generation

Execute the following instructions:
```bash
cd generation
rm domino_*
python generate_configurations.py
```

The output is composed of six files, one for each percentage of entity-predicate assignments with respect to all possible assignments.

Output:
- `domino_0.test.pro`
- `domino_20.test.pro`
- `domino_40.test.pro`
- `domino_60.test.pro`
- `domino_80.test.pro`
- `domino_100.test.pro`


## Prolog Execution

Execute the following instructions:
```bash
cd prolog
rm domino_*
cp ../generation/dominio_* .
./execute.bash
```

The output is composed of four six files, one for each percentage of entity-predicate assignments with respect to all possible assignments.

Output:
- `domino_0.test.txt`
- `domino_20.test.txt`
- `domino_40.test.txt`
- `domino_60.test.txt`
- `domino_80.test.txt`
- `domino_100.test.txt`


## Kotling Execution

In order to execute the state-change rule of CAC with Kotlin follow these steps:
* open the project in the folder `kotlin`
* go to the folder `prolog`
* empty the file final.txt
* copy the contents of an output from the previous step (e.g., `domino_20.test.txt`) and paste it into `instructions.txt`
* run the test named `prolog_test` in `CoreTest.kt`
* the output is in the file `final.txt`
* copy the contents into a file in `counter/domino_<percentage>.final.txt`


## Counter

Execute the following instructions:
```bash
cd counter
python counter.py
```

The output will be:
```json
--- domino_80.final.txt ---
tot_extended=7311
tot_centralized=4
tot_cacprolog=26
tot_kotlin=4900
tot=12241
defaultdict(<class 'int'>,
            {None: 1,
             'addResource': 10,
             'addRole': 10,
             'addUser': 7,
             'assignPermissionToRole': 12,
             'assignUserToRole': 6,
             'deleteResource': 10,
             'deleteRole': 7,
             'deleteUser': 6,
             'initUserC': 7,
             'readResource': 3,
             'revokePermissionFromRole': 13,
             'revokeUserFromRole': 10,
             'writeResource': 6})
defaultdict(<class 'int'>,
            {'addResourceC': 10,
             'addRoleC': 10,
             'addUserC': 7,
             'assignPermissionToRoleC': 29,
             'assignUserToRoleC': 6,
             'deleteResourceC': 8,
             'deleteRoleC': 7,
             'deleteUserC': 6,
             'initUserC': 7,
             'prepareReencryptionC': 89,
             'readResourceC': 66,
             'reencryptResourceC': 63,
             'revokePermissionFromRoleC': 90,
             'revokeUserFromRoleC': 42,
             'rotateRoleKeyPermissionsC': 9,
             'rotateRoleKeyUserRoleC': 9,
             'writeResourceC': 77})
--- domino_40.final.txt ---
tot_extended=2719
tot_centralized=2
tot_cacprolog=15
tot_kotlin=4320
tot=7056
defaultdict(<class 'int'>,
            {None: 1,
             'addResource': 10,
             'addRole': 10,
             'addUser': 7,
             'assignPermissionToRole': 12,
             'assignUserToRole': 6,
             'deleteResource': 10,
             'deleteRole': 7,
             'deleteUser': 6,
             'initUserC': 7,
             'readResource': 3,
             'revokePermissionFromRole': 13,
             'revokeUserFromRole': 10,
             'writeResource': 6})
defaultdict(<class 'int'>,
            {'addResourceC': 5,
             'addRoleC': 10,
             'addUserC': 7,
             'assignPermissionToRoleC': 16,
             'assignUserToRoleC': 6,
             'deleteResourceC': 5,
             'deleteRoleC': 7,
             'deleteUserC': 6,
             'initUserC': 7,
             'prepareReencryptionC': 37,
             'readResourceC': 22,
             'reencryptResourceC': 19,
             'revokePermissionFromRoleC': 45,
             'revokeUserFromRoleC': 42,
             'rotateRoleKeyPermissionsC': 5,
             'rotateRoleKeyUserRoleC': 5,
             'writeResourceC': 24})
--- domino_0.final.txt ---
tot_extended=34
tot_centralized=2
tot_cacprolog=0
tot_kotlin=270
tot=306
defaultdict(<class 'int'>,
            {None: 1,
             'addResource': 10,
             'addRole': 10,
             'addUser': 7,
             'assignPermissionToRole': 12,
             'assignUserToRole': 6,
             'deleteResource': 10,
             'deleteRole': 7,
             'deleteUser': 6,
             'initUserC': 7,
             'readResource': 3,
             'revokePermissionFromRole': 13,
             'revokeUserFromRole': 10,
             'writeResource': 6})
defaultdict(<class 'int'>,
            {'addRoleC': 10,
             'addUserC': 7,
             'assignUserToRoleC': 6,
             'deleteRoleC': 7,
             'deleteUserC': 6,
             'initUserC': 7,
             'revokeUserFromRoleC': 42})
--- domino_20.final.txt ---
tot_extended=837
tot_centralized=4
tot_cacprolog=4
tot_kotlin=830
tot=1675
defaultdict(<class 'int'>,
            {None: 1,
             'addResource': 10,
             'addRole': 10,
             'addUser': 7,
             'assignPermissionToRole': 12,
             'assignUserToRole': 6,
             'deleteResource': 10,
             'deleteRole': 7,
             'deleteUser': 6,
             'initUserC': 7,
             'readResource': 3,
             'revokePermissionFromRole': 13,
             'revokeUserFromRole': 10,
             'writeResource': 6})
defaultdict(<class 'int'>,
            {'addResourceC': 2,
             'addRoleC': 10,
             'addUserC': 7,
             'assignPermissionToRoleC': 8,
             'assignUserToRoleC': 6,
             'deleteResourceC': 3,
             'deleteRoleC': 7,
             'deleteUserC': 6,
             'initUserC': 7,
             'prepareReencryptionC': 14,
             'readResourceC': 7,
             'reencryptResourceC': 6,
             'revokePermissionFromRoleC': 21,
             'revokeUserFromRoleC': 42,
             'rotateRoleKeyPermissionsC': 7,
             'rotateRoleKeyUserRoleC': 7,
             'writeResourceC': 8})
--- domino_100.final.txt ---
tot_extended=9033
tot_centralized=5
tot_cacprolog=41
tot_kotlin=7420
tot=16499
defaultdict(<class 'int'>,
            {None: 1,
             'addResource': 10,
             'addRole': 10,
             'addUser': 7,
             'assignPermissionToRole': 12,
             'assignUserToRole': 6,
             'deleteResource': 10,
             'deleteRole': 7,
             'deleteUser': 6,
             'initUserC': 7,
             'readResource': 3,
             'revokePermissionFromRole': 13,
             'revokeUserFromRole': 10,
             'writeResource': 6})
defaultdict(<class 'int'>,
            {'addResourceC': 10,
             'addRoleC': 10,
             'addUserC': 7,
             'assignPermissionToRoleC': 32,
             'assignUserToRoleC': 6,
             'deleteResourceC': 10,
             'deleteRoleC': 7,
             'deleteUserC': 6,
             'initUserC': 7,
             'prepareReencryptionC': 108,
             'readResourceC': 78,
             'reencryptResourceC': 75,
             'revokePermissionFromRoleC': 112,
             'revokeUserFromRoleC': 42,
             'rotateRoleKeyPermissionsC': 13,
             'rotateRoleKeyUserRoleC': 13,
             'writeResourceC': 91})
--- domino_60.final.txt ---
tot_extended=4512
tot_centralized=2
tot_cacprolog=22
tot_kotlin=3700
tot=8236
defaultdict(<class 'int'>,
            {None: 1,
             'addResource': 10,
             'addRole': 10,
             'addUser': 7,
             'assignPermissionToRole': 12,
             'assignUserToRole': 6,
             'deleteResource': 10,
             'deleteRole': 7,
             'deleteUser': 6,
             'initUserC': 7,
             'readResource': 3,
             'revokePermissionFromRole': 13,
             'revokeUserFromRole': 10,
             'writeResource': 6})
defaultdict(<class 'int'>,
            {'addResourceC': 7,
             'addRoleC': 10,
             'addUserC': 7,
             'assignPermissionToRoleC': 22,
             'assignUserToRoleC': 6,
             'deleteResourceC': 6,
             'deleteRoleC': 7,
             'deleteUserC': 6,
             'initUserC': 7,
             'prepareReencryptionC': 54,
             'readResourceC': 38,
             'reencryptResourceC': 35,
             'revokePermissionFromRoleC': 66,
             'revokeUserFromRoleC': 42,
             'rotateRoleKeyPermissionsC': 8,
             'rotateRoleKeyUserRoleC': 8,
             'writeResourceC': 45})
```

