# Term-4-CSD-Team-48 API

## Getting started

### Running the Application

.env file needs to be present in root. Please ensure these keys are present.

AI_INFERENCE_IP_ADDRESS=
MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MONGO_CONNECTION_STRING=

AI_INFERENCE_IP_ADDRESS should be 127.0.0.1 if the AI server is being hosted on
the same machine.

MAIL_HOST should be smtp.gmail.com if gmail
MAIL_PORT should be 587 if gmail
MAIL_USERNAME should be email like example@gmail.com
MAIL_PASSWORD should be app password for the email and can be obtained here https://www.myaccount.google.com/apppasswords if gmail

MONGO_CONNECTION_STRING should be obtained by heading to https://www.mongodb.com and creating a free cluster. There should be a "Connect" button and click on "Drivers" to get the connection string.

To host the server on port 8000 run it here. The API server can be run on any port.

gradlew.bat bootRun --args='--server.port=8000'

### Testing the application

gradlew.bat test
