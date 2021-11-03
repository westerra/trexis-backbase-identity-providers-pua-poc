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

