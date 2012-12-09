define([], function() {
    var daysOfWeek = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
    var monthsOfYear = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep",
                        "Oct", "Nov", "Dec"];
    var monthsOfYearFull = ["January","February","March","April","May","June","July",
                            "August","September","October","November","December"];

    Date.prototype.getDayName = function() {
        return daysOfWeek[this.getDay()];
    };

    Date.prototype.getMonthName = function() {
        return monthsOfYear[this.getMonth()];
    };

    Date.prototype.getMonthFullName = function() {
        return monthsOfYearFull[this.getMonth()];
    };

    var DateUtils = {};

    DateUtils.getMonthFullName = function(month) {
        return monthsOfYearFull[month];
    };

    DateUtils.getMonthFromName = function(monthName) {
        var month = monthsOfYear.indexOf(monthName);
        if (month === -1) {
            return null;
        }
        return month;
    };

    return DateUtils;
});