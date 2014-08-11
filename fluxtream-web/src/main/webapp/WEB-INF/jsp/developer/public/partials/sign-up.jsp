<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
        prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
        isELIgnored="false"%><%@ page
        import="net.tanesha.recaptcha.*"%>
<h3>Register</h3>

<form role="form" name="createAccountForm" class="form-horizontal" ng-submit="createAccount(developer);">
    <div class="form-group">
        <label for="email" class="col-sm-2 control-label">Email</label>
        <div class="col-xs-4">
            <input class="form-control" type="email" autocorrect="off" autocapitalize="off" id="email" name="email"
                   ng-model="developer.email" placeholder="ex: example@example.com">
            <span ng-show="createAccountForm.email.$error.serverMessage" class="help-block error">{{createAccountForm.email.$error.serverMessage}}</span>
        </div>
    </div>
    <div class="form-group">
        <label for="username" class="col-sm-2 control-label">User Name</label>
        <div class="col-xs-4">
            <input class="form-control" type="text" autocorrect="off" autocapitalize="off" id="username" name="username"
                   ng-model="developer.username" placeholder="ex: Johndoe">
            <span ng-show="createAccountForm.username.$error.serverMessage" class="help-block error">{{createAccountForm.username.$error.serverMessage}}</span>
        </div>
    </div>
    <div class="form-group">
        <label for="firstname" class="col-sm-2 control-label">First Name</label>
        <div class="col-xs-4">
            <input class="form-control" type="text" autocapitalize="off" name="firstname" id="firstname"
                   ng-model="developer.firstname" placeholder="ex: John">
            <span ng-show="createAccountForm.firstname.$error.serverMessage" class="help-block error">{{createAccountForm.firstname.$error.serverMessage}}</span>
        </div>
    </div>
    <div class="form-group">
        <label for="lastname" class="col-sm-2 control-label">Last Name</label>
        <div class="col-xs-4">
            <input class="form-control" type="text" ng-model="developer.lastname" autocapitalize="off"
                   id="lastname" name="lastname" placeholder="ex: Doe">
            <span ng-show="createAccountForm.lastname.$error.serverMessage" class="help-block error">{{createAccountForm.lastname.$error.serverMessage}}</span>
        </div>
    </div>
    <div class="form-group">
        <label for="password" class="col-sm-2 control-label">Password</label>
        <div class="col-xs-4">
            <input class="form-control" type="password" ng-model="developer.password" id="password" name="password"
                   placeholder="Password">
            <span ng-show="createAccountForm.password.$error.serverMessage" class="help-block error">{{createAccountForm.password.$error.serverMessage}}</span>
        </div>
    </div>
    <div class="form-group">
        <label for="password2" class="col-sm-2 control-label">re-type Password</label>
        <div class="col-xs-4">
            <input class="form-control" type="password" ng-model="developer.password2" id="password2" name="password2"
                   placeholder="Password">
            <span ng-show="createAccountForm.password2.$error.serverMessage" class="help-block error">{{createAccountForm.password2.$error.serverMessage}}</span>
        </div>
    </div>
    <div class="form-group">
        <div class="col-sm-offset-2 col-sm-2">
            <button type="submit" class="btn btn-primary">Register</button>
        </div>
        <div class="col-sm-6">
            <div style="display:inline-block;height:37px;line-height:37px;vertical-align: top;margin-right: 15px;"></div>
        </div>
    </div>
</form>
