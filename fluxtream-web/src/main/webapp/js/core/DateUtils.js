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

    DateUtils.constrainDate = function(ts, state) {
        var splits = state.split("/");
        var date;
        if (splits[0]==="date"){
            date = moment(splits[1], "YYYY-MM-DD");
            return date.format("YYYY-MM-DD");
        } else if (splits[0]==="week") {
            var year = Number(splits[1]);
            var week = Number(splits[2]);
            var firstDate = moment().year(year).isoWeek(week).day(0);
            var lastDate = moment().year(year).isoWeek(week).day(6);
            if (ts.isBefore(firstDate))
                return firstDate.format("YYYY-MM-DD");
            else if (ts.isAfter(lastDate))
                return lastDate.format("YYYY-MM-DD");
            else return ts.format("YYYY-MM-DD");
        } else if (splits[0]==="month") {
            var year = Number(splits[1]);
            var month = Number(splits[2])-1;
            firstDate = moment().year(year).month(month).date(1);
            lastDate = moment().year(year).month(month).date(31);
            if (ts.isBefore(firstDate))
                return firstDate.format("YYYY-MM-DD");
            else if (ts.isAfter(lastDate))
                return lastDate.format("YYYY-MM-DD");
            else return ts.format("YYYY-MM-DD");
        }
    }

    function getWeekNumber(year, month, date) {
        var week = moment().year(year).month(month).date(date).week();
        return week;
    }

    function getDateRangeForWeek(year, week) {
        if (week == null) return null;
        var startDay = moment().year(year).week(week).day(1);
        var endDay = moment().year(year).week(week).day(7);

        // JavaScript Date objects are 0-based, but our date strings are 1-based
        var range = [new Date(startDay.year(), startDay.month(), startDay.date()),
                     new Date(endDay.year(), endDay.month(), endDay.date())];

        return range;
    }

    DateUtils.getDateRangeForWeek = getDateRangeForWeek;
    DateUtils.getWeekNumber = getWeekNumber;

    return DateUtils;
});