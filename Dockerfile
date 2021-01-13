FROM dmadk/niord-wildfly:2.0.0
COPY niord-dk-web/target/niord-dk-web.war /opt/jboss/wildfly/standalone/deployments/
