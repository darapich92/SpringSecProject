package example.authz

default allow = false

allowed_roles = {"admin", "branch_manager", "cashier", "viewer"}

# Allow access if the role is in the allowed roles list
allow {
    input.role == allowed_role
    allowed_role = allowed_roles[_]
}

# Specific access rules
allow {
    input.method == "GET"
    some r
    r = input.role[_]
    r == "admin"
    input.location == "namur"
}

allow {
    input.method == "GET"
    some r
    r = input.role[_]
    r == "Manager"
    input.location == "namur"
}

allow {
    input.method == "DELETE"
    some r
    r = input.role[_]
    r == "branch_manager"
    input.location == "a Staff"
}

allow {
    input.method == "GET"
    some r
    r = input.role[_]
    r == "cashier"
    input.location == "Product amount of Supermarket_region_A"
}
