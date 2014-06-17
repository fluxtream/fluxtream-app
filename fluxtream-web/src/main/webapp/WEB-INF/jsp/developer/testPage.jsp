<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html ng-app="developerWebsiteApp">
<head>
    <meta charset="utf-8">
    <title>Fluxtream Test Page</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>

    <link href="/${release}/css/styles.css" rel="stylesheet">

    <script src="/${release}/js/test.js"></script>

</head>
<body>

<div id="wrapper">

    <!-- Page content -->
    <div id="page-content-wrapper">
        <form class="form-horizontal" action="javascript:void(0)">
            <fieldset>

                <!-- Form Name -->
                <legend>Test jQuery Status Code handling</legend>

                <!-- Text input-->
                <div class="control-group">
                    <label class="control-label" for="statuscode">Desired Status Code</label>
                    <div class="controls">
                        <input id="statuscode" name="statuscode" type="text" placeholder="status code" class="input-xlarge">
                        <p class="help-block">Enter the status code that you want to receive in the Response</p>
                    </div>
                </div>

                <!-- Button -->
                <div class="control-group">
                    <label class="control-label" for="submit">Generate Response</label>
                    <div class="controls">
                        <button id="submit" name="submit" class="btn btn-primary">Test It!</button>
                    </div>
                </div>

            </fieldset>
        </form>

    </div>

</div>


<jsp:include page="partners/footer.jsp"/>
