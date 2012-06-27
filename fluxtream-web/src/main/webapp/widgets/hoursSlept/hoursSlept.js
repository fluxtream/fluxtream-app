define(function() {
    var HoursSlept = {};
    HoursSlept.load = function(metadata, digest) {
        console.log("Hello from HoursSlept");
        console.log(digest);
    }
    return HoursSlept;
})