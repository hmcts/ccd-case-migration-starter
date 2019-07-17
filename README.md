# ccd-case-migration-starter

Starter project for data migrations within CCD

## Artifacts

The following artifacts are produced:

- processor framework `uk.gov.hmcts.reform.ccd-case-migration:processor`
- domain classes `uk.gov.hmcts.reform.ccd-case-migration:domain`

### Processor framework

Package offers framework for data migrations within CCD that runs the following process:

![diagram](docs/process.png)

### Domain classes

Package offers base domain classes for standardised case data structures such as party, telephone number etc.  

## Getting started

Assuming that you have Java project scaffolding please include framework artifact as a project dependency by adding following line:

```groovy
compile group: 'uk.gov.hmcts.reform.ccd-case-migration', name: 'processor', version: '2.0.0'
```

If you want to take advantages of standard domain classes please also include domain artifact as a project dependency by adding following line:

```groovy
compile group: 'uk.gov.hmcts.reform.ccd-case-migration', name: 'domain', version: '2.0.0'
```

Having done above please create class that implements `uk.gov.hmcts.reform.migration.service.DataMigrationService` interface in similar way as shown below:

```java
package uk.gov.hmcts.reform.migration.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.function.Predicate;

@Component
public class DataMigrationServiceImpl implements DataMigrationService {
    @Override
    public Predicate<CaseDetails> accepts() {
        return true; // Predicate that allows to narrow number of cases that gets migrated
    }

    @Override
    public void migrate(CaseDetails caseDetails) {
        // Case data migration logic goes here
    }
}
```

Finally add configuration file with the following entries:

```properties
idam.api.url= # IDAM API URL used to authenticate system update user (pointing to localhost version of IDAM API by default)
idam.client.id= # IDAM OAuth2 client ID used to authenticate system update user
idam.client.secret= # IDAM OAuth2 client secret used to authenticate system update user
idam.client.redirect_uri= # IDAM OAuth2 redirect URL used to authenticate system update user

idam.s2s-auth.url= # S2S API URL used to authenticate service (pointing to localhost version of S2S API by default)
idam.s2s-auth.microservice= # S2S micro service name used to authenticate service
idam.s2s-auth.totp_secret= # S2S micro service secret used to authenticate service

core_case_data.api.url= # CCD data store API URL used to fetch / update case details (pointing to localhost version of CCD by default)

migration.idam.username= # IDAM username of a system update user that performs data migration
migration.idam.password= # IDAM password of a system update user that performs data migration
migration.jurisdiction= # CCD jurisdiction that data migration is run against
migration.casetype= # CCD case type that data migration is run against
migration.caseId= # optional CCD case ID in case only one case needs to be migrated
```

## Future development

### Unit tests

To run all unit tests please execute following command:

```bash
    ./gradlew test
```

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the tags on this repository.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
