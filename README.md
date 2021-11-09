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

# Included Authenticators
## Ingestion Authentication Username Password Form
Triggers the trexis-backbase-ingstion service on succesful authentication.
## Login Ingestion Authenticator
## Channel Selector
Present OTP Channel Selectors - i.e voice,sms -  from Identity configuration. Example of configuration in Identity

```
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
## OTP Authenticator
Sends OTP to communication outbound


