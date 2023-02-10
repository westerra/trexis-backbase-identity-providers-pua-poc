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
Sends email after successful MFA, Example of configuration in Identity
```yaml
MFA_EMAIL_MESSAGE: "If you did not login to your account or if you have any questions, please contact us immediately at our contact number (123-456-7890).<br><br>Please do not reply directly to this email as we will not receive your message."
MFA_EMAIL_SUBJECT: "Alert: Digital Banking OTP has been verified!"
MFA_EMAIL_TEMPLATE: "sendMfaSuccessfulEmail.ftl"
MFA_EMAIL_FOOTER: "&copy;2022 By Your Bank.<br>All rights reserved."
MFA_EMAIL_ENABLED: "true"
```

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
> Note for MFA : We need to compare Login events for MFA,For this we need to turn on Login Events from Identity -> Event ->  Config -> Login Events Settings (Where in Saved Types "LOGIN" must be there)
else it will do MFA always.
> Note for LAST_LOGIN_DAYS : LAST_LOGIN_DAYS must be there. It's number of days to check last login and also select login event for those days to compare IpAddress If we have enabled Login events.
> Note for LAST_IP_CHECK : It's number of IpAddresses(Login Event) to compare with current login IpAddress. Default value is 4
> For Example (Today: 10th January,2022) if we set LAST_LOGIN_DAYS to 5 and LAST_IP_CHECK to 6 it will do MFA if user did not login for last 5 days.(From 5th January,2022). If we have enabled Login event then we will take Login events for last 5 days (From 5th January,2022) and compare last 6 Login IpAddress with current one.
> Note for OTP_DIGIT : It's number of digit for OTP. Default value is 8
> Note for OTP_PERIOD : It's number of Second for OTP period. Default value is 60
> Note for Password update Email : We need add Event Listeners from Identity -> Event -> Config -> Event Listeners (Select "update-password-email" from dropdown) and also need to turn on Admin Events from Identity -> Event ->  Config -> Admin Events Settings to get email for user password update. We can set email body message, footer and subject from configuration (EMAIL_SUBJECT,MESSAGE,EMAIL_FOOTER). 
> Note for UPDATE_PASSWORD_EMAIL : After adding Event Listener and Admin Events we can enable/disable user password update email with this configuration. Default value is true

> Note for MFA cases : we can set 4 different values(case-insensitive) for user attribute (identity->users->attributes->key:mfaRequired) : true,false,alwaysTrue,alwaysFalse
> For alwaysTrue, It will always do MFA and after successful login with MFA user attribute(mfaRequired) will be as it is(alwaysTrue).
> For alwaysFalse, It will never do MFA.
> For true, It will do MFA on next immediate login and after successful login with MFA user attribute(mfaRequired) will set to false.
> For false, It will do MFA based on condition.If condition returns true, We will make user attribute(mfaRequired) to true while doing login with MFA and after successful login we will set to false.
> In condition check we will check based on LAST_LOGIN_DAYS. For Example (Today: 10th January,2022) if we set LAST_LOGIN_DAYS to 5 and LAST_IP_CHECK to 6 it will do MFA if user did not login for last 5 days.(From 5th January,2022). If we have enabled Login event then we will take Login events for last 5 days (From 5th January,2022) and compare last 6 Login IpAddress with current one.

> Note for LOOKAHEAD_WINDOW : It can be 0, 1 or any other numeric value. Default value is 1.
> For example, if the time interval for a token(OTP) is 30 seconds,the value of 0 means it will accept valid tokens in the 30-second window only. (time interval of 30 seconds)
> For example, if the time interval for a token(OTP) is 30 seconds,the default value of 1 means it will accept valid tokens in the 90-second window (time interval 30 seconds + look ahead 30 seconds + look behind 30 seconds). Every increment of this value increases the valid window by 60 seconds.

```yaml
GET_ACCESS_TOKEN_BASE_URL: "http://host.docker.internal:8180/auth/realms/master/protocol/openid-connect/token"
GET_USER_EVENTS_BASE_URL: "http://host.docker.internal:8180/auth/admin/realms/backbase/events"
CLIENT_ID: admin-cli
USERNAME: admin
PASSWORD: admin
GRANT_TYPE: password
LAST_LOGIN_DAYS: 5
OTP_DIGIT: 8
OTP_PERIOD : 60
LOOKAHEAD_WINDOW : 0
MESSAGE: "If you did not make this change or if you have any questions, please contact us immediately at our contact number (123-456-7890).<br><br>Please do not reply directly to this email as we will not receive your message."
EMAIL_SUBJECT: "Alert: Digital Banking Password Changed"
TEMPLATE: "sendContactUpdateEmail.ftl"
EMAIL_FOOTER: "&copy;2022 By Your Bank.<br>All rights reserved."
UPDATE_PASSWORD_EMAIL: "true"
```

## Keycloak Event Handlers

### Update Profile Listener

This listener currently only handles the `UPDATE_EMAIL`. This propogates email updates to the core system via finite.

example of relevant configuration in Identity for this event handler:

```yaml
FINITE_HOSTURL: http://host.docker.internal:9090
FINITE_API_CORE_APIKEY: xxx
MAPPINGS_PRIMARYEMAIL: email
IDENTITY_FINITE_ENTITY_IDENTIFIER_CLAIM: entityId
```
