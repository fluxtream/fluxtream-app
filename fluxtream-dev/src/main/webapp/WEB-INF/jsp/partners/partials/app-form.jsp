<%--
  Created by IntelliJ IDEA.
  User: candide
  Date: 16/04/14
  Time: 08:17
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<div class="page-content inset">
    <div class="row">
        <form role="form" ngController="AppCreateFormCtrl" style="width:400px">
            <div class="form-group">
                <label for="appName">Name</label>
                <input type="text" class="form-control" ng-model="app.name"
                       id="appName" placeholder="Enter your app's name">
            </div>
            <div class="form-group">
                <label for="appName">Website (optional)</label>
                <input type="text" class="form-control" ng-model="app.website"
                       placeholder="Your app's website, if any">
            </div>
            <div class="form-group">
                <label for="appDescription">Description</label>
                <textarea id="appDescription" class="form-control"
                          ng-model="app.description" rows="3"
                          placeholder="Please describe your app"></textarea>
            </div>
            <button type="submit" ng-click="createOrUpdate(app)" class="btn btn-primary">Submit</button>
            <a href="#/apps" class="btn btn-default">Cancel</a>
        </form>
    </div>
</div>
