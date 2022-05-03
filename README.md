# Backbase Authentication SPI

Backbase Identity is designed to cover most use-cases without requiring custom code, but we also want it to be customizable.
To achieve this Backbase Identity has a number of Service Provider Interfaces (SPI) from which you can implement your
own providers.

To implement an SPI you need to provide implementation for its Provider and ProviderFactory interfaces.
You also need to create a service configuration file META-INF/services/org.keycloak.authentication.AuthenticatorFactory.

The 'MyCustomAuthenticator' class provides an example for a Backbase Identity authenticator which when implemented can be configured in an authentication flow. 
MyCustomAuthenticatorFactory is responsible for creating an Authenticator instance.
You must implement the Authenticator as well as the AuthenticatorFactory interfaces as shown in this example package.

This file must remain in the jar that the AuthenticatorFactory implementation class is contained in.
The file must have the fully qualified class name of all your AuthenticatorFactory classes.

> Note: The output jar of this build is intended for use by backbase-identity.
> For this reason it needs to include any dependencies that are not provided by the runtime.
> These additional dependencies are included in this project's build artifact (see the [pom.xml's](./pom.xml) build configuration)

## Theme Setup

Ensure freemarker templates are available when using channel selector and otp.

Sample included in resources/theme.login folder

## Included Authenticators

### Ingestion Authentication Username Password Form

Triggers the trexis-backbase-ingestion service on successful authentication.

### Login Ingestion Authenticator
### Channel Selector Authenticator

Present OTP Channel Selectors - i.e voice,sms -  from Identity configuration. Example of configuration in Identity

```yaml
INGESTION_SERVICE_HOST: "ingestion-integration-service"
INGESTION_SERVICE_BASEPATH: "service-api/v1"
INGESTION_SERVICE_INGESTION_PATH: "/ingestion"
INGESTION_SERVICE_PORT: "8080"
INGESTION_SERVICE_SCHEME: "HTTP"
"keycloak.backbase.authenticators.otp-authenticator.communications-service-endpoint": "http://host.docker.internal:8204/identity-communication-outbound-integration-service/service-api/v1/communications/batches"
"keycloak.backbase.authenticators.otp-authenticator.otp-channels.text.channel": "sms-otp"
"keycloak.backbase.authenticators.otp-authenticator.otp-channels.text.from": "xxx"
"keycloak.backbase.authenticators.otp-authenticator.otp-channels.text.identity-attributes.smsMobile1": "1"
"keycloak.backbase.authenticators.otp-authenticator.otp-channels.text.identity-attributes.smsMobile2": "2"
"keycloak.backbase.authenticators.otp-authenticator.otp-channels.voice.channel": "voice-otp"
"keycloak.backbase.authenticators.otp-authenticator.otp-channels.voice.from": "xxx"
"keycloak.backbase.authenticators.otp-authenticator.otp-channels.voice.identity-attributes.voiceMobile1": "10"
"keycloak.backbase.authenticators.otp-authenticator.otp-channels.voice.identity-attributes.voiceMobile2": "11"
```

### OTP Authenticator

Sends OTP to communication outbound

### Enrollment

Allow for rebase of user relationship/service agreements.  And also allow for evaluating if user should be placed in a limited/restricted state
```yaml
ENROLLMENT_SERVICE_SCHEME: "HTTP"
ENROLLMENT_SERVICE_HOST: "host.docker.internal"
ENROLLMENT_SERVICE_PORT: "8051"
ENROLLMENT_SERVICE_BASEPATH: "enrollment/service-api/v2"
ENROLLMENT_SERVICE_REBASE_PATH: "/enrollments/backbase/rebase"
ENROLLMENT_SERVICE_EVALUATE_LIMITED_PATH: "/enrollments/backbase/limited"
```

### Identity Provider

Example of configuration in Identity for Identity Provider:

```yaml
GET_ACCESS_TOKEN_BASE_URL: "http://host.docker.internal:8180/auth/realms/master/protocol/openid-connect/token"
GET_USER_EVENTS_BASE_URL: "http://host.docker.internal:8180/auth/admin/realms/backbase/events"
CLIENT_ID: admin-cli
USERNAME: admin
PASSWORD: admin
GRANT_TYPE: password
LAST_LOGIN_DAYS: 5
```

## Keycloak Event Handlers

### Update Profile Listener

This listener currently only handles the `UPDATE_EMAIL`. This propogates email updates to the core system via finite.

example of relevant configuration in Identity for this event handler:

```yaml
FINITE_HOSTURL: http://localhost:9090
FINITE_API_CORE_APIKEY: xxx
MAPPINGS_PRIMARYEMAIL: email
IDENTITY_FINITE_ENTITY_IDENTIFIER_CLAIM: entityId
```