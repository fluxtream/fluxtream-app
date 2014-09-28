<h3>Please sign in</h3>

<form class="form-horizontal" name="signInForm" role="form" ng-submit="signIn(credentials);" >
    <div class="form-group" id="username-form-group">
        <label for="f_username" class="col-sm-2 control-label">Username</label>
        <div class="col-xs-4">
            <input type="text" class="form-control" ng-model="credentials.f_username"
                   id="f_username" name="f_username" placeholder="Email">
        </div>
    </div>
    <div class="form-group" id="password-form-group">
        <label for="f_password" class="col-sm-2 control-label">Password</label>
        <div class="col-xs-4">
            <input type="password" class="form-control" ng-model="credentials.f_password"
                   id="f_password" name="f_password" placeholder="Password">
            <span ng-show="signInForm.f_password.$error.serverMessage" class="help-block">{{signInForm.f_password.$error.serverMessage}}</span>
        </div>
    </div>
    <div class="form-group">
        <div class="col-sm-offset-2 col-xs-4">
            <button type="submit" class="btn btn-primary">Sign in</button>
        </div>
    </div>
</form>