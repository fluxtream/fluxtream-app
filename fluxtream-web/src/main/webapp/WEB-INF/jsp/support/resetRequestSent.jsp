<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"%><style><!--
        body {background-color: #e8e8e8;margin: 0;padding: 0;}
        p {padding:5px 0;}
        a img {border: none;}
        a {border: none;}
        .body {background-color: #ffffff;}
        td.content {padding: 14px 0 0 14px;text-align: left;}
        .header {}
        .header h1 {font-family: Georgia;font-size: 32px;font-weight: normal;color: #bfbfbf;display: inline;text-align: left;padding-left:22px;}
        .content p {font-family: 'Lucida Grande';font-size: 12px;color: #333333;margin: 0 0 10px 0;text-align: left;}
        .content a {font-family: Georgia;font-size: 13px;color: #53A3DB;font-style: italic;}
        .content img.inline {border: 1px solid #dedede;padding: 4px;}
        .footer p {font-family: 'Lucida Grande';font-size: 10px;color: #8d8d8d ;margin: 5px 0 0 25px ;text-align: left;}
      
--></style>
<p>&nbsp;</p>
<table cellspacing="0" cellpadding="0" width="100%" bgcolor="#e8e8e8">
	<tbody>
		<tr>
			<td align="center">
				<table width="590" border="0" cellspacing="0" cellpadding="0">
					<tbody>
						<tr>
							<td class="header" height="47" align="left">
								<table border="0" cellspacing="0" cellpadding="0" width="500">
									<tbody>
										<tr>
											<td style="padding-left:42px;">
                                                <img width="94" height="20" alt="Fluxtream" src="/${release}/images/header-logo-v4.png">
											</td>
										</tr>
									</tbody>
								</table>
							</td>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
		<tr>
			<td align="center">
				<table class="body" cellspacing="10" cellpadding="0" width="500"  style="border: solid 1px #DDD;-moz-border-radius:=10px; -webkit-border-radius:10px; border-radius:10px;">
					<tbody>
						<tr>
							<td class="content" align="center" valign="top">
								<p>A password reset request mail has been sent to you.</p>
								<p>Please check your mailbox</p>
							</td>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
		<tr>
			<td align="center">
				<table class="footer" cellspacing="0" cellpadding="0" width="600">
					<tbody>
						<tr>
							<td valign="top">
                                <% int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR); String until = ""; if (currentYear>2011) until = " - " + currentYear; %>
								<p style="padding-left:35px;">Fluxtream - All right reserved &copy; 2010<%=until %></p>
							</td>
						</tr>
					</tbody>
				</table>
			</td>			
		</tr>
	</tbody>
</table>
