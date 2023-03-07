ARG APP_INSIGHTS_AGENT_VERSION=3.2.10
FROM hmctspublic.azurecr.io/base/java:17-distroless

USER hmcts
COPY build/libs/ccd-case-migration.jar /opt/app/

EXPOSE 4999
CMD [ "ccd-case-migration.jar" ]
