define(function() {
    var HoursSlept = {};
    HoursSlept.load = function(digest) {
        console.log("Hello from HoursSlept");
        console.log(digest);
    }
    return HoursSlept;
})