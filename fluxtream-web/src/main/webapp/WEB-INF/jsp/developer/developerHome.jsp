<%--
  Created by IntelliJ IDEA.
  User: candide
  Date: 11/04/14
  Time: 14:14
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:include page="header.jsp"/>

<div id="wrapper">

    <!-- Sidebar -->
    <div id="sidebar-wrapper">
        <ul class="sidebar-nav">
            <li class="sidebar-brand"><a href="#">&nbsp;</a>
            </li>
            <li class="active"><a href="#">Applications</a>
            </li>
            <li><a href="#">Api Docs</a>
            </li>
            <li><a href="/logout">Sign Out</a>
            </li>
        </ul>
    </div>

    <!-- Page content -->
    <div id="page-content-wrapper">
        <div class="content-header">
            <h1>
                <a id="menu-toggle" href="#" class="btn btn-default"><i class="icon-reorder"></i></a>
                Fluxtream Developer
            </h1>
        </div>
        <div class="page-content inset">
            <div class="row">
                &nbsp;&nbsp;&nbsp;&nbsp;<a href="addApplication" class="btn btn-primary">Add Application</a>
            </div>
        </div>
    </div>

</div>

<!-- Custom JavaScript for the Menu Toggle -->
<script>
    //$("#menu-toggle").click(function(e) {
    //    e.preventDefault();
    //    $("#wrapper").toggleClass("active");
    //});
</script>

<jsp:include page="footer.jsp"/>
