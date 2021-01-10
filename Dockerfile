FROM kaspernielsen/niord-wildfly:0.9.1
COPY niord-dk-web/target/niord-dk-web.war /opt/jboss/wildfly/standalone/deployments/
