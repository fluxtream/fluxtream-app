### OAuth 2 3-legged authorization

#### Create a Fluxtream account

If you don’t already have a Fluxtream account, please [register for one](${homeBaseUrl}welcome?register).

#### Create app keys

Once logged in the developer console, you will be able to create an app instance. Fluxtream will provide you with a `client_id` and a `client_secret`.

#### Request authorization from the user

Send your user to [/auth/oauth2/authorize](${homeBaseUrl}auth/oauth2/authorize) with your `client_id` as a parameter. You are also required to provide a `redirect_uri` parameter where users will be redirected after the authorization process.

Your request will look like this:

    ${homeBaseUrl}/auth/oauth2/authorize?client_id=xxx&redirect_uri=xxx

#### Create an authorization handler

Once the user has granted permission to use their account on the Fluxtream page, Fluxtream will redirect to your callback url, supplying an oauth `code` as a GET variable.

You should extract the `code` parameter from the request and make a HTTP `POST` to Fluxtream's `/token` endpoint. The parameters are as follow:

    grant_type: "authorization_code"
    code: <code>
    client_id: <your_client_id>
    client_secret: <your_client_secret>
    redirect_uri: <your_redirect_uri>

The server will reply with a JSON object containing the following information:

    access_token: a token that you will use to authenticate your requests
    refresh_token: a token that you will use to refresh your access_token when it expires
    expires_in: number of seconds until your access_token expires

#### Access Fluxtream's REST API

To authenticate your requests, just add the following header:

    Authorization: Bearer <your_access_token>

#### Refresh your access token

When your access token has expired, refresh it by sending a HTTP `POST` to Fluxtream's `/token` endpoint with the following parameters:

    grant_type: "refresh_token"
    refresh_token: <your_refresh_token>
    client_id: <your_client_id>
    client_secret: <your_client_secret>

The server will reply with a json object containing the following information:

    access_token: a new access token that you will use to authenticate your requests
    refresh_token: a new token that you will use to refresh your access_token when it expires
    expires_in: number of seconds until your access_token expires again

