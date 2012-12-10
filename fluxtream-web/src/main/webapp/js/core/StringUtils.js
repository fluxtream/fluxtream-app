define([], function() {
    String.prototype.upperCaseFirst = function() {
        return this.charAt(0).toUpperCase() + this.slice(1);
    };

    var StringUtils = {};

    return StringUtils;
});