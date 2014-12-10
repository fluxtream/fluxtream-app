<form class="form-horizontal" action="javascript:void(0)">
    <div class="control-group">
        <div class="controls">
            <label>Please enter your Beddit login info</label>
            <p><small>We will use your credentials only once to retrieve an authorization token and then forget about them forever.</small></p>
        </div>
    </div>

    <div class="control-group">
        <label class="control-label" for="inputEmail">Email</label>
        <div class="controls">
            <input type="text" class="email" id="inputEmail" placeholder="Email">
        </div>
    </div>
    <div class="control-group">
        <label class="control-label" for="inputPassword">Password</label>
        <div class="controls">
            <input type="password" class="password" id="inputPassword" placeholder="Password">
        </div>
    </div>
    <div class="control-group">
        <div class="controls">
            <button onclick="Connectors.submitBedditAuthInfo($(this).closest('form'))" type="submit" class="btn">Send</button>
        </div>
    </div>

</form>
