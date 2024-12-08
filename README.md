# 1. Objective

In this document, we will illustrate the application of the Identity and Access Management (IAM). Currently, this application focus only on the performance of the Identification and Authentication such as registration, login and logout of the user and Authorization based on the Role-based access control mechanism.

# 2. Technologies stacks

In order to succeed the implementation of the IAM system, I use many different technologies.

- IAM platform: Keycloak that is the open-source IAM tools. It provides a based solution of IAM including the authorization. But, we do not use authorization of Keycloak.
- Software development framework: Java Springboot framework to develop both front-end and back-end.
- Load balancer tool & reverse proxy server: to optimize and secure traffic to the back-end.
- Authorization tool: Open Policy Engine (OPA) is used.
- Other: Docker is used to build images of IAM Engine, database and NGINX.

# 3. System architecture
![alt text](image.png)
# 4. Scenarios