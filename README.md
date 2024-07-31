# Trader Goods Profiles

This repository contains the public-facing APIs for the Trader Goods Profiles system. These APIs serve as the interface
for interacting with the Trader Goods Profiles service.

### About

The Trader Goods Profiles system manages profiles of goods traders, facilitating efficient communication and trade
operations. This repository hosts the API definitions and documentation for external parties to integrate with the
Trader Goods Profiles service.

### Usage

These APIs provide endpoints for various operations related to goods traders and profiles. Developers can utilize these
endpoints to retrieve, create, update, and delete trader profiles, as well as perform other relevant actions.

### Test the API locally

Notice: You can use the run_local.sh script file to load all needed services and start the trader-goods-profiles service.
#### Start the services
* Open a terminal window, type the below command and press enter. This will load locally all the services necessary for testing :

    ```sm2 --start TGP_API```

#### Generate an access token
* Use the [Auth wizard](http://localhost:9949/auth-login-stub/session)
  * Fill the following details:
    <br><br>

    **Redirect Url**: http://localhost:9949/auth-login-stub/session <br>
    **Affinity Group**: Organisation or Individual<br>
    **Enrolment Key**: HMRC-CUS-ORG <br>
    **Identifier Name**: EORINumber <br>
    **Identifier Value**: GB123456789001 (or any thing else similer). Refer to the service guide to get a list of EROI 
  number suitable for test or look at the stubs [README.md file](https://github.com/hmrc/trader-goods-profiles-stubs/blob/main/README.md)
    <br><br>
* Press submit. This will redirect to a new page showing an access token.
* Copy the Bearer token

#### Send a request with Postman

You can use Postman for tests for now. But there are plan to replace this which I don't know when it will be.

In Postman
 
* Create a request (GET, POST, PUT, PATCH) depending on  what endpoint you want to test. For a list of endpoint available
    refer to the [API Specification](https://github.com/hmrc/trader-goods-profiles/blob/main/resources/public/api/conf/1.0/application.yaml)
* In the header add the following header:
    ```aidl
    Accept: application/vnd.api+json
    X-Client-ID: <any thing>
    ```

**Notes:** Add the moment we only check if the X-Client-ID is present and do not do any validation

* In the Authorization tab select **Bearer Token** as **Auth Type** 
* Add the access token create on [this step](#generate-an-access-token) 
* Add the right url
* Send the request.

### Communication with EIS

The Trader Goods Profiles system communicates with the Enterprise Integration Service (EIS) via the Trader Goods
Profiles Router microservice. This microservice facilitates seamless communication between the Trader Goods Profiles
system and EIS, ensuring efficient data exchange and integration.

### Dev

Before pushing, you can run [precheck.sh](./precheck.sh) which will run all the tests, as well as check the format.

### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").