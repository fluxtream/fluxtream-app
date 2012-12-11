define([], function() {
    String.prototype.upperCaseFirst = function() {
        var lowerCase = this.toLowerCase();
        return lowerCase.charAt(0).toUpperCase() + lowerCase.slice(1);
    };

    String.prototype.startsWith = function(s) {
        return this.indexOf(s) === 0;
    };

    String.prototype.endsWith = function(s) {
        return this.lastIndexOf(s) === this.length - s.length;
    };

    var StringUtils = {};

    StringUtils.addCommas = function(nStr) {
        nStr += '';
        var x = nStr.split('.'),
            x1 = x[0],
            x2 = x.length > 1 ? '.' + x[1] : '',
            rgx = /(\d+)(\d{3})/;
        while (rgx.test(x1)) {
            x1 = x1.replace(rgx, '$1' + ',' + '$2');
        }
        return x1 + x2;
    };

    return StringUtils;
});