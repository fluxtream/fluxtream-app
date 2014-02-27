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

    // Cache used to cut the number of network requests required for getWeekNumber and getDateRangeForWeek
    var _cache = {
        "dateToWeekNumber": {},
        "weekToDateRange": {}
    };

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


    function getWeekNumber(year, month, day) {
        // Let's have java compute that for us and avoid the discrepancy bug that way for now
        var dateString = year + "-" + (month < 9 ? 0 : "") + (month + 1) + "-" + (day < 10 ? 0 : "") + day;
        if (dateString in _cache["weekToDateRange"]) {
            return _cache["weekToDateRange"][dateString];
        }
        var result = null;
        $.ajax({
            url: "/api/calendar/nav/getMeTheJavaComputedWeekForThisDate?formattedDate=" + dateString,
            async: false,
            success: function(data) {
                result = data;
            }
        });
        _cache["weekToDateRange"][dateString] = result;
        return result;
    }

    function getDateRangeForWeek(year, week) {
        if (week == null) return null;
        var key = year + "/" + week;
        if (key in _cache["weekToDateRange"]) {
            return _cache["weekToDateRange"][key];
        }

        var result = null;
        $.ajax({
            url: "/api/calendar/nav/getDateRangeForWeek?year=" + year + "&week=" + week,
            async: false,
            success: function(data) {
                result = data;
            }
        });
        if (result == null) {
            return null;
        }

        var startParts = result[0].split("-");
        var endParts = result[1].split("-");
        if (startParts == null || endParts == null) {
            return null;
        }

        var startYear = startParts[0];
        var startMonth = startParts[1];
        var startDate = startParts[2];

        var endYear = endParts[0];
        var endMonth = endParts[1];
        var endDate = endParts[2];

        // JavaScript Date objects are 0-based, but our date strings are 1-based
        var range = [new Date(startYear, startMonth - 1, startDate),
                     new Date(endYear, endMonth - 1, endDate)];

        _cache["weekToDateRange"][key] = range;
        return range;
    }

    DateUtils.getDateRangeForWeek = getDateRangeForWeek;
    DateUtils.getWeekNumber = getWeekNumber;

    return DateUtils;
});