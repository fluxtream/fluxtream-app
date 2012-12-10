define([], function() {
    String.prototype.upperCaseFirst = function() {
        var lowerCase = this.toLowerCase();
        return lowerCase.charAt(0).toUpperCase() + lowerCase.slice(1);
    };

    var StringUtils = {};

    return StringUtils;
});