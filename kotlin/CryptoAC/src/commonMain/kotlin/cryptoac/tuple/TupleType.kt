package cryptoac.tuple

enum class RBACType {
    USER,
    ROLE,
    RESOURCE,
    USERROLE,
    ROLEPERMISSION,
}

enum class RBACElementType {
    USER,
    ROLE,
    RESOURCE,
}

enum class ABACType {
    USER,
    ATTRIBUTE,
    RESOURCE,
    USERATTRIBUTE,
    ACCESSSTRUCTUREPERMISSION,
}

enum class ABACElementType {
    USER,
    ATTRIBUTE,
    RESOURCE,
}
