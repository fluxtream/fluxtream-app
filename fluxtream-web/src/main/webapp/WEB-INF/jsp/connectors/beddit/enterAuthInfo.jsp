<form action="javascript:void(0)">
    <p>Please enter your Beddit login info</p>

    <fieldset>
        <label>Email: </label>
        <input class="email"/><br>
        <label>Password: </label>
        <input class="password" type="password"/><br>
        <button onclick="Connectors.submitBedditAuthInfo(this.parentElement)" type="submit" class="btn">Send</button>
    </fieldset>
</form>
