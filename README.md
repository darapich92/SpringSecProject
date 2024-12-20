# 1. Objective

In this document, we will illustrate the application of the Identity and Access Management (IAM). Currently, this application focus only on the performance of the Identification and Authentication such as registration, login and logout of the user and Authorization based on the Role-based access control mechanism.

# 2. Technologies stacks

In order to succeed the implementation of the IAM system, I use many different technologies.

- IAM platform: Keycloak is the open-source IAM tools. It provides a based solution of IAM including the authorization. But, we do not use authorization of Keycloak because it is less flexibility for complex or highly dynamic policies.
- Software development framework: Java Springboot framework to develop both front-end and back-end.
- Load balancer tool & reverse proxy server: to optimize and secure traffic to the back-end.
- Authorization tool: Open Policy Engine (OPA) is used because it is extreme flexibility for custom policies and cloud-native and supports infrastructure-level use cases. To download
- Other: Docker is used to build images of IAM Engine, database and NGINX.

# 3. System architecture
![alt text](image.png)

# 4. How to run the project

After pulling the project and you are in the `SpringApp` folder. Before executing command below, I suppose readers installed docker and OPA.

- Execute two keycloaks, NGINX and Postgresl in docker:
    ```bash
  # access to 2keycloaks folder
  cd /2keycloaks/ 
  # run docker
  docker-compose up
Two Keycloak instances run as a cluster to provide Keycloak services while sharing the same postgresql database. NGINX acts as a reverse proxy to distribute requests across the Keycloak cluster. 

After executing above command, readers can access to http://localhost:80 for logging-in to the admin page. To login, username: `admin`, password: `123`. For this test, we created a realm called `Spring_App`. You also can view all testing users in that realm.
- Execute OPA service:
    ```bash
  # access to policies folder
  cd ../demo/src/main/resources/policies/ 
  # run service
  opa run --server accessPolicy.rego
We made a simple access policy based on the role and region of the access. For viewing the whole content of the policy, please access to `accessPolicy.rego` .
- Execute Front-End:
    ```bash
  # access to demo folder 
    cd ../../../..
  # execute spring-boot client 
    mvn clean package 
    mvn spring-boot:run

After executing above command, readers can access to http://localhost:8081 for viewing the login and logout screen. You can test with username: `user1`, password: `123`
- Execute Back-End:
    ```bash
    # access to keycloak admin folder 
    cd ../keycloakAdmin
    # execute spring-boot service 
    mvn clean package 
    mvn spring-boot:run

# 5. Scenarios
1.  Authentication and Authorization an access of a user based on the role and location.

| Requests   | Results   | 
|------------|------------|
| 20/2000 | <span style="color:green;">(Completed)</span> |
| 2k/2k   | <span style="color:orange;">(Progressing) |
| 10k/10k | <span style="color:red;">(Not yet) |

2. Hosting OPA and Keycloak to a public domain.
