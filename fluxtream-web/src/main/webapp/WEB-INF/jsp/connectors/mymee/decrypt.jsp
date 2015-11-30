<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"%>

<%

  String encryptedPayload = (String) request.getAttribute("payload");
  encryptedPayload = encryptedPayload.replaceAll("\n", "\\\\\n");
  String key = (String) request.getAttribute("key");

%>
<script>

  var encryptedPayload = "<%=encryptedPayload%>";

  var json = CryptoJS.AES.decrypt( encryptedPayload, "<%=key%>" ).toString( CryptoJS.enc.Utf8 );
  var mymeeInfo = JSON.parse(json);

  $.ajax({url:"/mymee/setConnectionParams",
    data : mymeeInfo,
    method: "POST",
    success: function(html) {
      $(".addConnectorsMain").html(html);
    }
  });

</script>

<h3>Decrypting your info...</h3>
