package example.authz

# Default deny all access
default allow = false

# Define allowed roles
allowed_roles = {"admin", "branch_manager", "cashier", "viewer", "Manager"}

# Define allowed locations
allowed_locations = {"Brussels", "Antwerp", "Paris", "USA", "Namur", "New_York", "Brazil"}

# General rules: Allow access if the role exists in the allowed list
allow {
    input.role == allowed_role
    allowed_role = allowed_roles[_]
    input.location == allowed_location
    allowed_location = allowed_locations[_]
}

# Allow rule for LOGIN method with valid roles and locations
allow {
    input.method == "LOGIN"
    some r
    r = input.role[_]            # Match at least one role
    r == allowed_roles[_]        # Role is in the allowed roles set
    input.location == allowed_locations[_]  # Location matches
}

# Rule 1: Admins have unrestricted access in Namur
allow {
    input.method == "GET"
    some r
    r = input.role[_]
    r == "admin"
    input.location == "Namur"
}

# Rule 2: Managers have GET access in Namur
allow {
    input.method == "GET"
    some r
    r = input.role[_]
    r == "Manager"
    input.location == "Namur"
}

# Rule 3: Branch Managers can DELETE staff data
allow {
    input.method == "DELETE"
    some r
    r = input.role[_]
    r == "branch_manager"
    input.location == "Staff"
}

# Rule 4: Cashiers can GET product details in region A
allow {
    input.method == "GET"
    some r
    r = input.role[_]
    r == "cashier"
    input.location == "Product amount of Supermarket_region_A"
}

# Rule 5: Restrict access to Viewer role, they only have read access to Paris
allow {
    input.method == "GET"
    some r
    r = input.role[_]
    r == "viewer"
    input.location == "Paris"
}

# Rule 6: Dynamic context rules: Allow branch managers to manage their specific locations
allow {
    input.method == "MANAGE"
    some r
    r = input.role[_]
    r == "branch_manager"
    input.location = {"Brussels", "Antwerp", "Namur"}
}

# Edge rule: Deny access explicitly for roles not in the allowed list or unknown methods
deny {
    not input.role == allowed_roles
    not input.method = {"GET", "DELETE", "MANAGE"}
}