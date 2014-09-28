$(document).ready(function() {
    console.log("loading test environment...");
    $("#submit").click(function() {
        $.ajax({
            url: "/api/v1/test/statusCode/" + $("#statuscode").val(),
            success: function(body, statusText, jqXHR) {
                console.log(body);
                console.log(statusText);
                console.log(jqXHR);
            },
            error: function(jqXHR, textStatus, errorThrown){
                console.log(jqXHR);
                console.log(textStatus);
                console.log(errorThrown);
            }
        })
    })
});