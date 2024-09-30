package example.authz

default allow = false

allowed_roles = {"admin", "editor", "viewer"}

# Allow access if the role is in the allowed roles list
allow {
    input.role == allowed_role
    allowed_role = allowed_roles[_]  
}

allow {
  input.method == "GET"
  input.role == "admin"
}

allow {
  input.method == "GET"
  input.role == "editor"
}
