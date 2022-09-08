ARG APP_INSIGHTS_AGENT_VERSION=3.2.10
FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY build/libs/ccd-migration.jar /opt/app/

EXPOSE 4999
CMD [ "ccd-migration.jar" ]
