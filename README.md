# Term-4-CSD-Team-48 API

## Getting started

### Running the Application

.env file needs to be present in root. Keys that start with MAIL are optional and can be absent as they only affect the mail sending capabilities of the server. For MONGO_CONNECTION_STRING you are welcome to use the default provided as it is just a free database with no sensitive information.

AI_SERVER_URL=  
MAIL_HOST=  
MAIL_PORT=  
MAIL_USERNAME=  
MAIL_PASSWORD=  
MAIL_SMTP_AUTH=true  
MAIL_SMTP_STARTTLS=true  
MONGO_CONNECTION_STRING=

AI_SERVER_URL should be http://127.0.0.1:8080 if the AI server is being hosted on
the same machine.

MAIL_HOST should be smtp.gmail.com if gmail  
MAIL_PORT should be 587 if gmail  
MAIL_USERNAME should be email like example@gmail.com  
MAIL_PASSWORD should be app password for the email and can be obtained here https://www.myaccount.google.com/apppasswords if gmail

MONGO_CONNECTION_STRING should be obtained by heading to https://www.mongodb.com and creating a free cluster. There should be a "Connect" button and click on "Drivers" to get the connection string.

To host the server on port 80 run the command below. The API server can be run on any port and it can still integrate with the AI as long as you let the AI know which port is the API running on. The API by default runs on port 8080 if you don't specify --args

gradlew.bat bootRun --args='--server.port=80'

### Testing the application

gradlew.bat test
