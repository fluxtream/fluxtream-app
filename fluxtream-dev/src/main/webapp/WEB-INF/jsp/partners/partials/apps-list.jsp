<div class="col-md-12" ng-controller="AppsListCtrl">
    <h3 class="page-header">Your Applications</h3>
    <div class="row">
        <p>
            <a type="button" class="btn btn-primary" href="#/app-create">Add Application</a>
        </p>
        <div ng-repeat="app in apps" class="well">
            <dl class="dl-horizontal">
                <dt>Name</dt><dd>{{app.name}}</dd>
                <dt>Description</dt><dd>{{app.description}}</dd>
                <dt>Client Secret</dt><dd>{{app.sharedSecret}}</dd>
                <dt>Client ID</dt><dd>{{app.uid}}</dd>
            </dl>
            <p class="text-right">
                <a ng-click="delete(app)" class="btn btn-danger">Delete this App</a>
                <a href="#/app-edit?uid={{app.uid}}" class="btn btn-default">Edit this App</a>
            </p>
        </div>
    </div>
</div>
