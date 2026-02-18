 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.7
FROM hmctspublic.azurecr.io/base/java:21-distroless

USER hmcts
COPY lib/applicationinsights.json /opt/app/
COPY build/libs/ccd-case-migration.jar /opt/app/

EXPOSE 4999
CMD [ "ccd-case-migration.jar" ]
