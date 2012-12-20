require(["core/FlxState"], function(FlxState) {
    describe("FlxState", function() {
        it("can save app state successfully", function() {
            expect(function() {
                FlxState.saveState("app1", {app: 1})
            }).not.toThrow();
            expect(function() {
                FlxState.saveState("app2", {app: 2})
            }).not.toThrow();
        });
        it("can fetch app state successfully", function() {
            expect(FlxState.getState("app1")).toEqual({app: 1});
            expect(FlxState.getState("app2")).toEqual({app: 2});
        });
        it("fetches null for non-existent apps", function() {
            expect(FlxState.getState("app0")).toBeNull();
        });
        it("can update app state", function() {
            expect(function() {
                FlxState.saveState("app1", {app: 1, foo: 42})
            }).not.toThrow();
            expect(FlxState.getState("app1")).toEqual({app: 1, foo: 42});
            expect(FlxState.getState("app2")).toEqual({app: 2});
        });
        it("can save tab states successfully", function() {
            expect(function() {
                FlxState.saveTabState("app1", "tab1", {app: 1, tab: 1})
            }).not.toThrow();
            expect(function() {
                FlxState.saveTabState("app2", "tab2", {app: 2, tab: 2})
            }).not.toThrow();
            expect(function() {
                FlxState.saveTabState("app2", "tab1", {app: 2, tab: 1})
            }).not.toThrow();
        });
        it("can fetch tab state successfully", function() {
            expect(FlxState.getTabState("app1", "tab1")).toEqual({app: 1, tab: 1});
            expect(FlxState.getTabState("app2", "tab2")).toEqual({app: 2, tab: 2});
            expect(FlxState.getTabState("app2", "tab1")).toEqual({app: 2, tab: 1});
        });
        it("fetches null for non-existent tabs", function() {
            expect(FlxState.getTabState("app0", "tab1")).toBeNull();
            expect(FlxState.getTabState("app1", "tab2")).toBeNull();
        });
        it("can update tab state", function() {
            expect(function() {
                FlxState.saveTabState("app1", "tab1", {app: 1, tab: 1, bar: 1729})
            }).not.toThrow();
            expect(FlxState.getTabState("app1", "tab1")).toEqual({app: 1, tab: 1, bar: 1729});
            expect(FlxState.getTabState("app2", "tab2")).toEqual({app: 2, tab: 2});
            expect(FlxState.getTabState("app2", "tab1")).toEqual({app: 2, tab: 1});
        });
    });
});