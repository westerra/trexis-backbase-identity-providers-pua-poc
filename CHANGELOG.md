
## Changelog - treXis Backbase Identity Providers

## July 04, 2023
- version bump to 1.2.1-SNAPSHOT
- Added UPDATE_PROFILE check while sending email update to finite cause with upgrade we only have an UPDATE_PROFILE event while changing email. 

## March 28, 2023
- version bump to 1.1.12-SNAPSHOT
- Fixing urlEncode issue, For get access token call
- Fixing Ip compare logic for MFA

## March 20, 2023
- version bump to 1.1.11-SNAPSHOT
- Introducing separate error handling on UI for invalid credentials & user disabled.

## March 10, 2023
- version bump to 1.1.10-SNAPSHOT
- Introducing opt out (Text message) configuration

## February 8, 2023
- version bump to 1.1.9-SNAPSHOT
- Fixing MFA bug 
- Introducing email functionality where user get an email after successful MFA.

## November 8, 2022
- Fixing bug with MFA devices page.
- Returning the right error message if user did not select an MFA option.

### October 20, 2022

- version bump to 1.1.7-SNAPSHOT
- Grouping MFA options by Channel type (SMS, Email, Voice) in the MFA selection screen
- Changing timer from 60 seconds to 10 minutes

