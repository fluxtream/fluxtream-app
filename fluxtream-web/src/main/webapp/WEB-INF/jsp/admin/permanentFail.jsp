<%@ page import="java.util.List" %>
<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"%><%@ taglib
        uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><%@ taglib
        uri="http://granule.com/tags" prefix="g"
        %><%@   page import="org.fluxtream.core.auth.AuthHelper"
        %><%@ page import="org.fluxtream.core.domain.ApiKey"
        %>
<%@ page import="org.fluxtream.mvc.controllers.AdminViewHelper" %>
<%
    List<ApiKey> apiKeys = (List<ApiKey>)request.getAttribute("apiKeys");
    List<ApiKey.Status> statusFilters = (List<ApiKey.Status>)request.getAttribute("statusFilters");%><!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="apple-mobile-web-app-status-bar-style"
          content="black-translucent" />
    <meta name="viewport" content="width=1000, initial-scale=1.0">
    <title>Fluxtream</title>
    <meta name="description" content="">
    <meta name="author" content="">

    <link rel="stylesheet" href="/static/css/bootstrap-2.3.2.min.css">
    <link rel="stylesheet"
          href="/static/css/bootstrap-responsive-2.3.2.min.css">
    <g:compress>
        <link rel="stylesheet" href="/static/css/jquery-ui/jquery-ui-1.10.3.custom.css">
        <link rel="stylesheet"
              href="/static/css/jquery-colorPicker/jquery.colorPicker.css">
        <link rel="stylesheet" href="/static/css/msdropdown/dd.css">
        <link rel="stylesheet" href="/static/css/tagedit/css/jquery.tagedit.css">
    </g:compress>

    <link rel="stylesheet" href="/static/css/font-awesome-3.2.1.css">
    <link href='//fonts.googleapis.com/css?family=Roboto' rel='stylesheet' type='text/css'>
    <link rel="stylesheet" href="/${release}/css/admin.css">

    <script
            src="https://maps-api-ssl.google.com/maps/api/js?libraries=geometry&v=3&sensor=false"
            type="text/javascript"></script>
    <script src="/static/js/hogan-2.0.0.js"></script>

    <link rel="shortcut icon" href="/favicon.ico">
    <script
            src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
    <script>
        window.jQuery
        || document
                .write('<script src="/static/js/jquery-1.10.2.min.js"><\/script>')
    </script>

    <script src="/static/js/bootstrap-2.3.2.min.js"></script>
    <script src="/static/js/jquery-ui-1.10.3.custom.min.js"></script>
    <script src="/static/js/jquery.ui.sortable-1.8.2-bt-1.0.1.js"></script>
    <script src="/static/js/json2-2011.10.19.js"></script>
    <script src="/static/js/jquery.autoGrowInput-1.0.0.js"></script>
    <script src="/static/js/jquery.colorPicker-2012.01.27.js"></script>
    <script src="/static/js/jquery.dd-2.37.5-uncompressed.js"></script>
    <script src="/static/js/jquery.tagedit-1.2.1.js"></script>
    <script src="/static/js/jquery.shorten-1.0.0.js"></script>
    <script src="/static/js/underscore-1.3.3-min.js"></script>
    <script src="/static/js/backbone-1.0.0-custom.1-min.js"></script>
    <script src="/static/js/jquery.ba-dotimeout-1.0.0.min.js"></script>
    <script src="/static/js/jquery.masonry-2.1.03.min.js"></script>
    <script src="/static/js/jquery.xcolor-1.8.js"></script>
    <script src="/static/js/jquery.outerHTML-1.0.0.js"></script>

    <script>
        window.FLX_RELEASE_NUMBER = "${release}";
    </script>

    <script data-main="/${release}/js/admin/main.js" src="/static/js/require-1.0.3.js"></script>

</head>

<body>
<div id="flxUsername" style="display:none;"><%=AuthHelper.getGuest().username%></div>
<div id="flxUID" style="display:none;"><%=AuthHelper.getVieweeId()%></div>

<div id="content">

    <div class="navbar">
        <div class="navbar-inner">
            <div class="container-fluid">
                <a class="btn btn-navbar" data-toggle="collapse"
                   data-target=".nav-collapse"> <span class="icon-bar"></span> <span
                        class="icon-bar"></span> <span class="icon-bar"></span>
                </a>
                <a class="brand" href="/admin"><img
                        src="/${release}/images/header-logo-v4.png" width=94 height=20/></a>
            </div>
        </div>
    </div>

    <style>
        #header-fixed {
            position: fixed;
            top: 0px; display:none;
            background-color:white;
            padding-right: 40px;
        }

        .updater-selection {
            width: 1em;
        }
        .connectorName-column {
            width: 120px;
        }
        .apiKeyReason{
            width: 250px;
        }
        .apiKeyGuestId{
            width: 80px;
        }
        .apiCode{
            width: 80px;
        }
        .apiStatus{
            width: 80px;
        }
        #dashboardTable, #header-fixed {
            table-layout: fixed;
        }

        th, td {
            overflow: hidden;
        }

    </style>

    <div  class="container-fluid">

        <h3>Search (failed) Updaters</h3>

        <form action="/admin/fail">
            <fieldset>
                <div class="control-group">
                    <label>Where clause</label>
                    <input type="text" name="filter" placeholder="more filters" value="${filter}">
                    <input type="text" name="guestId" placeholder="a Guest ID" value="${guestId}">
                </div>
                <span class="help-block">Example filter: apiKey.reason LIKE 'CLIENT_ERROR 400%'</span>
                <!-- Multiple Checkboxes -->
                <div class="control-group">
                    <div class="controls" id="checkboxes">
                        <label class="checkbox inline" for="checkboxes-0">
                            <input type="checkbox" <%=AdminViewHelper.setChecked(statusFilters, ApiKey.Status.STATUS_PERMANENT_FAILURE)%> name="status_1" id="checkboxes-0" value="true">
                            Permanent Fail
                        </label>
                        <label class="checkbox inline" for="checkboxes-1">
                            <input type="checkbox" <%=AdminViewHelper.setChecked(statusFilters, ApiKey.Status.STATUS_TRANSIENT_FAILURE)%> name="status_2" id="checkboxes-1" value="true">
                            Transient Fail
                        </label>
                        <label class="checkbox inline" for="checkboxes-2">
                            <input type="checkbox" <%=AdminViewHelper.setChecked(statusFilters, ApiKey.Status.STATUS_OVER_RATE_LIMIT)%> name="status_3" id="checkboxes-2" value="true">
                            Over Rate Limit
                        </label>
                        <label class="checkbox inline" for="checkboxes-3">
                            <input type="checkbox" <%=AdminViewHelper.setChecked(statusFilters, ApiKey.Status.STATUS_UP)%> name="status_0" id="checkboxes-3" value="true">
                            Up
                        </label>
                    </div>
                </div>
                <button type="submit" class="btn">Submit</button>
            </fieldset>
        </form>

        <c:choose>
            <c:when test="${apiKeys.size()==0}">
                <div class="alert alert-success">
                    Nothing to show here!
                </div>
            </c:when>
            <c:otherwise>
                <table class="table table-bordered" id="header-fixed"></table>

                <button class="btn disabled scheduleUpdateButton" style="width:25em">Update Connectors</button>
                <div style="margin-bottom:1em"></div>

                <table class="table table-bordered" id="dashboardTable">
                    <thead>
                        <th class="updater-selection"><input type="checkbox" id="select-all" value="true"></th>
                        <th class="connectorName-column">Connector Name</th>
                        <th class="apiCode">Api code</th>
                        <th class="apiStatus">Status</th>
                        <th class="apiKeyGuestId">Guest ID</th>
                        <th class="apiKeyReason">Reason</th>
                        <th class="details">Details</th>
                    </thead>
                    <tbody>
                    <c:forEach var="apiKey" items="${apiKeys}">
                        <tr>
                            <td><input class="updater-checkbox" data-apiKeyId="${apiKey.id}" type="checkbox"></td>
                            <td>${apiKey.connector.prettyName}</td>
                            <td>${apiKey.connector.value}</td>
                            <td class="apiKeyStatus">
                                <c:choose>
                                    <c:when test="${empty apiKey.status}">
                                        <a class="btn btn-link" href="/admin/${apiKey.guestId}/${apiKey.id}">
                                            <div title="${apiKey.status}" class="syncStatus-NA">&nbsp;</div>
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        <a class="btn btn-link" href="/admin/${apiKey.guestId}/${apiKey.id}">
                                            <div title="${apiKey.status}" class="syncStatus-${apiKey.status}">
                                                &nbsp;</div>
                                        </a>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>${apiKey.guestId}</td>
                            <td>${apiKey.reason}</td>
                            <td><a href="/admin/${apiKey.guestId}/${apiKey.id}">details</a></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>

    <script>
        $("#select-all").change(function() {
            var checked = $("#select-all").prop("checked");
            $(".updater-checkbox").prop("checked", checked);
            updateScheduleUpdateButtons();
        });
        $(".updater-checkbox").change(updateScheduleUpdateButtons);
        function updateScheduleUpdateButtons() {
            var nSelected = 0;
            var totalItems = 0;
            $(".updater-checkbox:checked").each(function() {
                nSelected++;
            });
            $(".updater-checkbox").each(function() {
                totalItems++;
            });
            $("#select-all").prop("checked", totalItems==nSelected);
            if (nSelected>0) {
                var updateMessage = "Update " + nSelected;
                if (nSelected>1)
                    updateMessage += " Connectors Now";
                else
                    updateMessage += " Connector Now";
                $(".scheduleUpdateButton").html(updateMessage);
                $(".scheduleUpdateButton").removeClass("disabled").addClass("btn-primary");
            } else {
                $(".scheduleUpdateButton").html("Update Connectors");
                $(".scheduleUpdateButton").addClass("disabled").removeClass("btn-primary");
            }
        }
        function getSelectedApiKeyIds() {
            var selectedApiKeyIds = "";
            var i = 0;
            $(".updater-checkbox:checked").each(function() {
                var apiKeyId = $(this).attr("data-apiKeyId");
                if (i>0) selectedApiKeyIds += ",";
                selectedApiKeyIds += apiKeyId;
                i++;
            });
            return selectedApiKeyIds;
        }
        $(".scheduleUpdateButton").click(function(){
            var selectedApiKeyIds = getSelectedApiKeyIds();
            $.ajax({
                url: "/api/admin/batch/update",
                type: "POST",
                data: {"apiKeyIds" : selectedApiKeyIds},
                success: function() {
                    location.reload();
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    alert("there was a problem: " + textStatus + "/" + errorThrown);
                }
            });
            console.log(selectedApiKeyIds);
        });
    </script>

</div>

</body>
</html>
