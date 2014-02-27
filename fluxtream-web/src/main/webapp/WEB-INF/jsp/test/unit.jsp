<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Fluxtream</title>
    <!-- jasmine CSS/JS -->
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/static/jasmine/lib/jasmine-1.3.0/jasmine.css">
    <script src="${pageContext.request.contextPath}/static/jasmine/lib/jasmine-1.3.0/jasmine.js"></script>
    <script src="${pageContext.request.contextPath}/static/jasmine/lib/jasmine-1.3.0/jasmine-html.js"></script>
    <script src="${pageContext.request.contextPath}/static/jasmine/lib/jasmine-1.3.0/jasmine.console_reporter.js"></script>
    <!-- deps -->
    <script>
        window.FLX_RELEASE_NUMBER = "${release}";
    </script>
    <script src="https://maps-api-ssl.google.com/maps/api/js?libraries=geometry&v=3&sensor=false"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/hogan-2.0.0.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/underscore-1.3.3-min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/backbone-1.0.0-custom.1-min.js"></script>
    <script data-main="/${release}/js/test.js" src="${pageContext.request.contextPath}/static/js/require-1.0.3.js"></script>
    <!-- spec files -->
    <script src="/${release}/js/test/FlxStateSpec.js"></script>
    <script src="/${release}/js/test/DateUtilsSpec.js"></script>
    <script>
        (function() {
            var jasmineEnv = jasmine.getEnv();
            jasmineEnv.updateInterval = 1000;

            var htmlReporter = new jasmine.HtmlReporter();
            if (/PhantomJS/.test(navigator.userAgent)) {
                jasmineEnv.addReporter(new jasmine.TrivialReporter());
                jasmineEnv.addReporter(new jasmine.ConsoleReporter());
            } else {
                jasmineEnv.addReporter(new jasmine.HtmlReporter());
            }

            jasmineEnv.specFilter = function(spec) {
                return htmlReporter.specFilter(spec);
            };

            var currentWindowOnload = window.onload;
            window.onload = function() {
                if (currentWindowOnload) {
                    currentWindowOnload();
                }
                execJasmine();
            }

            function execJasmine() {
                jasmineEnv.execute();
            }
        })();
    </script>
</head>
<body>
</body>
</html>