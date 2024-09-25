package example.authz

default allow = false

allow {
  input.method == "GET"
  input.role == "admin"
}
