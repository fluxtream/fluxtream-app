define(function() {
    var CaloriesBurned = {};
    CaloriesBurned.load = function(digest) {
        console.log("Hello from CaloriesBurned");
        console.log(digest);
    }
    return CaloriesBurned;
})