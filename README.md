# niord-dk [![Build Status](https://travis-ci.com/NiordOrg/niord-dk.svg?branch=master)](https://travis-ci.com/NiordOrg/niord-dk)

The niord-dk project contains Denmark-specific extensions for the   
[niord](https://github.com/NiordOrg) system, i.e. a system
for producing and promulgating NW + NM T&P messages.

Niord is documented extensively at http://docs.niord.org

# niord-dk-web

The *niord-web* web-application, found under the [niord](https://github.com/NiordOrg/niord) project,
constitutes the main system for producing and promulgating navigational warnings and notices to 
mariners.

The *niord-dk-web* project is an overlay web-application that customizes *niord-web* for use in
Denmark. 

## Prerequisites

* Java 11
* Maven 3
* MySQL 5.7.10+ (NB: proper spatial support is a requirement)
* JBoss Wildfly 21+
* JBoss Keycloak 3.1+

## Development Set-Up

The [niord-appsrv](https://github.com/NiordOrg/niord-appsrv) project contains scripts for
setting up Wildfly, Keycloak, etc.

However, the easiest way to get started developing on this project is to use Docker.

### Starting MySQL and Keycloak

You may want to start by creating a *.env* file in your working directory, which overrides the environment variables 
(such as database passwords) used in the docker compose file.

The following commands will start two MySQL databases, one for the application server 
and one for Keycloak, and also run Keycloak itself.

    mkdir $HOME/.niord-dk
    docker-compose -f dev/docker-dev-compose.yml pull
    docker-compose -f dev/docker-dev-compose.yml up -d

The initial *mkdir* command is just to avoid permission problems since docker would otherwise create it as owned
by root.

Once this is up and running, create a Keycloak admin user (default niordadmin/keycloak)
which can be used to create user groups and assign domain roles to the groups:

    ./dev/keycloak-admin-user.sh

Enter [http://localhost:8090/auth/](http://localhost:8090/auth/) and check that you can log in using the Keycloak admin user.

### Configuring Wildfly

Next, create and configure a Wildfly installation using:

    ./dev/install-wildfly.sh

Import or open the niord-dk project in your favorite IDE. Here we describe  the 
*IntelliJ* set-up:
* Firstly, you may want to right-click the wildfly-10.1.0-Final folder and mark
  the folder as excluded.
* From Github, also check out the niord-dk parent 
  [niord](https://github.com/NiordOrg/niord) projects.
* Import the niord-dk project in IntelliJ via its pom.xml.
* Under the "Maven Projects" also import the parent niord project.
* In Run -> Edit configuration..., configure a new local JBoss server based on the Wilfly
  installation produced above.
* Deploy "niord-dk-web:war exploded" to the server.
* If you have only updated web resources, there is no need to re-deploy the web application. Use the "Update resources" 
  function instead.
* To get rid of superfluous IntelliJ code editor warnings, disable the "Declaration access can be weaker" 
  and "Dangling Javadoc comment" inspections.

The Keycloak docker image creates an initial domain, "Master", and a Niord user, sysadmin/sysadmin,
that should be used for the initial configuration of the system, whereupon they should be
deleted.

Enter [http://localhost:8080](http://localhost:8080) and check that you can log in using the Niord sysadmin user.

### Finishing touches

Import the Danish test base data into Niord:

    ./dev/install-base-data.sh
    
Within a minute or so, this will import domains, areas, categories, etc. needed to run the Niord-DK project. 
First clean up a bit:
* In Niord, under Sysadmin -> Domains, click the "Create in Keycloak" button for the "NW" and "NM" domains. 
  This will create the two domains in Keycloak. 
* In Keycloak, edit the "Sysadmin" user group. Under "Role Mappings", in the drop-down "Client Roles" select first 
"niord-nw" then "niord-nm" and assign the "sysadmin" client roles to the group.
* While in Keycloak, you may also want to define new user groups for editors and admins, and assign the appropriate 
  client roles for "niord-nw" and "niord-nm" to the groups. 
  Additionally, for admin-related groups (who should be able to manage users in Niord), assign the "manage-clients" and 
  "manage-users" client roles of the "realm-management" client to the groups.
* Delete the "Master" domain in Niord and the corresponding "niord-client-master" client in Keycloak.
* Go through the configuration and settings of the Niord Sysadmin pages and adjust as 
  appropriate.


