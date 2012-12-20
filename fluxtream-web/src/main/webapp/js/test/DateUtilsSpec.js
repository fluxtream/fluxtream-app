require(["core/DateUtils"], function(DateUtils) {
    describe("DateUtils", function() {
        var d = new Date("Jul 31, 1986 09:16 EST")
        it("implements Date prototype methods correctly", function () {
            expect(d.getDayName()).toEqual("Thu");
            expect(d.getMonthName()).toEqual("Jul");
            expect(d.getMonthFullName()).toEqual("July");
        });
        it("implements DateUtils methods correctly", function() {
            expect(DateUtils.getMonthFullName(6)).toEqual("July");
            expect(DateUtils.getMonthFromName("Jul")).toEqual(6);
            expect(DateUtils.getMonthFromName("Foo")).toBeNull();
        });
    });
});