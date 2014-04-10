<%--
  Created by IntelliJ IDEA.
  User: candide
  Date: 10/04/14
  Time: 12:28
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>


<html>
<head>
    <title>Fluxtream Authorization</title>

    <script type="text/javascript">
        // Decode the query string into its fields.
        var QueryString = function () {
            // The map of parameter keys to an array of their values.
            var result = {};

            // Get the query string portion of the URL.
            var query = window.location.search.substring(1);
            // Split each of the parameters.
            var vars = query.split("&");
            // For each parameter,
            for (var i=0;i<vars.length;i++) {
                // Get the key and value pairs.
                var pair = vars[i].split("=");

                // If it is not exactly a key-value pair, then skip it.
                if(pair.length !== 2) {
                    continue;
                }

                // Decode the key.
                var key = decodeURIComponent(pair[0]).replace(/\+/g, " ");

                // Decode the value.
                var value =
                        decodeURIComponent(pair[1]).replace(/\+/g, " ");

                // If this is the first parameter with this key, then
                // create a new array entry and add it.
                if (typeof result[key] === "undefined") {
                    result[key] = [ value ];
                }
                // Otherwise, just add this parameter to the list.
                else {
                    result[key].push(value);
                }
            }

            // Return the mapping.
            return result;
        }();
    </script>
</head>
<body>
<!-- Add the name and description of the third-party. -->
<p>Name:
    <script type="text/javascript">
        document.write(QueryString['name'][0]);
    </script>
</p>
<p>Description:
    <script type="text/javascript">
        document
                .write(QueryString['description'][0]);
    </script>
    <!-- Add the list of desired schema IDs. -->
<p>Scopes:</p>
<ul id="scopes"></ul>
<script type="text/javascript">
    try {
        // Get the only "scope" parameter that should be an array
        // of scopes.
        var scopes = QueryString['scope'][0].split(" ");

        // Get the list.
        var scopeList = document.getElementById("scopes");

        // For each scope element, add it to the scopes list.
        for (var i = 0; i < scopes.length; i++) {
            var scopeElement =
                    scopeList
                            .appendChild(document.createElement("li"));
            scopeElement
                    .appendChild(document.createTextNode(scopes[i]));
        }
    }
    catch(e) {
        // Do nothing.
    }
</script>
<!-- Get the user's credentials. -->
Username: <input type="text" id="username"><br/>
Password: <input type="password" id="password"><br/>
<!--
    Require the user to indicate that they are granting
    authorization.
 -->
<input type="checkbox" value="true" id="granted">Grant<br/>
<!--
    The submit button. This should probably be broken into two
    buttons, and the "Grant" check-box should be removed. Both
    buttons will submit all of the data from the form. But, the
    "Grant" button will include the 'grant' parameter set to
    "true", and the "Deny" button will include the 'grant'
    parameter set to "false".
 -->
<input type="button" value="Submit" onclick="makeRequest();">
<p id="error"></p>
<script type="text/javascript">
    /**
     * Makes the request to the server and, if it fails, updates the
     * error message.
     */
    function makeRequest() {
        // Create the request.
        var request = new XMLHttpRequest();

        // Set the handler for when the response comes back.
        request.onreadystatechange =
        function() {
            if(request.readyState === 4) {
                document.getElementById("error").text =
                request.responseText;
            }
        };

        // Build the request.
        var params = "";

        // Add the username.
        params =
        params +
        "username=" +
        encodeURIComponent(
                document.getElementById("username").value);
        // Add the password.
        params =
        params +
        "&password=" +
        encodeURIComponent(
                document.getElementById("password").value);
        // Add the granted boolean.
        params =
        params +
        "&granted=" +
        encodeURIComponent(
                document.getElementById("granted").checked);
        // Add the code.
        params = params + "&code=" + QueryString['code'][0];

        // Make the request.
        request.open('POST', 'authorization', true);
        request
                .setRequestHeader(
                "Content-type",
                "application/x-www-form-urlencoded");
        request.send(params);
    }
</script>
</body>
</html>