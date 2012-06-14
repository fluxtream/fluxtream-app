<%@ page import="com.fluxtream.mvc.controllers.ControllerHelper" %>
<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"%><%@ taglib
    prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
    prefix="sec" uri="http://www.springframework.org/security/tags"%><%@ taglib
    uri="http://granule.com/tags" prefix="g"%><!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style"
    content="black-translucent" />
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Fluxtream - Personal Analytics</title>
<meta name="description" content="">
<meta name="author" content="">

<g:compress>
    <link rel="stylesheet" href="/static/css/bootstrap-2.0.3.min.css">
    <link rel="stylesheet"
        href="/static/css/bootstrap-responsive-2.0.3.min.css">
    <link rel="stylesheet" href="/css/flx.css">
    <link rel="stylesheet" href="/css/bodytrack.css">
    <link rel="stylesheet" href="/css/datepicker.css">
    <link rel="stylesheet" href="/static/css/jquery-ui/jquery-ui-1.8.18.custom.css">
    <link rel="stylesheet"
        href="/static/css/jquery-colorPicker/jquery.colorPicker.css">
    <link rel="stylesheet" href="/static/css/msdropdown/dd.css">
    <link rel="stylesheet" href="/static/css/tagedit/css/jquery.tagedit.css">
</g:compress>

<link rel="stylesheet" href="/static/css/font-awesome.css">

<script
    src="https://maps-api-ssl.google.com/maps/api/js?libraries=geometry&v=3&sensor=false"
    type="text/javascript"></script>

<link rel="shortcut icon" href="/favicon.ico">

</head>

<body style="background:#fff;">

    <div id="content">
    
        <div class="navbar">
            <div class="navbar-inner">
                <div class="container-fluid">
                    <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse"> <span class="icon-bar"></span> <span class="icon-bar"></span> <span class="icon-bar"></span>
                    </a> 
                    <a class="brand" href="/app"><img src="/1.0-alpha-build_20120403/images/header-logo-v4.png" /></a>
                    <a class="brand" href="/app"><img src="/1.0-alpha-build_20120403/images/header-logo-v4.png" /></a>
                    <div class="nav-collapse">
                        <ul class="nav">
                            <li>
                                <div class="btn-group" id="apps-menu" data-toggle="buttons-radio"></div>
                            </li>
                        </ul>
                        <ul class="nav pull-right">
                            <li class="divider-vertical"></li>
                            <li class="dropdown">
                                <a href="#" class="dropdown-toggle" data-toggle="dropdown">Connectors <i class="icon-random icon-large"></i> <b class="caret"></b></a>
                                <ul class="dropdown-menu">
                                    <li><a href="javascript:App.connectors()">Add  <i class="icon-plus icon-large" style="float: right;"></i></a></li>
                                    <li><a href="javascript:App.connectors()">Manage <i class="icon-list icon-large" style="float: right;"></i></a></li>
                                </ul>
                            </li>
                            <li class="divider-vertical"></li>
                            <li class="dropdown">
                                <a href="#" class="dropdown-toggle" data-toggle="dropdown">Test Test <i class="icon-user icon-large"></i> <b class="caret"></b></a>
                                <ul class="dropdown-menu">
                                    <li><a href="javascript:App.settings()">Settings <i class="icon-cog icon-large" style="float: right;"></i></a></li>
                                    <li><a href="javascript:App.addresses()">Addresses <i style="float: right;" class="icon-home icon-large"></i></a></li>
                                    <li><a href="/admin/index">Admin <i class="icon-key icon-large" style="float: right;"></i></a></li>
                                    <li class="divider"></li>
                                    <li><a href="/logout">Logout <i style="float: right" class="icon-off icon-large"></i></a></li>
                                </ul>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>    
<div class="container-fluid">
        <div class="row-fluid">   <!-- New code snippet from peri 2012-21-11 -->
            <div class="span12" id="flx-bodytrack-menubar">
                <div class="flx-bodytrack-menu">
                    <div class="floater">
                        <h3 style="line-height:42px">Untitled view</h3>
                    </div>
                    <div class="floater" id="time-menu-container">
                        <div class="btn-toolbar flx-bodytrack-menu" id="bodytrack-menu">
                            <div class="btn-group">
                                <a class="btn" href="#">New view</a>
                            </div>
                            <div class="btn-group"> 
                                <a class="btn dropdown-toggle" data-toggle="dropdown" href="#">Load view  <span class="caret"></span></a>
                                <ul class="dropdown-menu load-view-subMenu">
                                    <li class="flx-clearfix">
                                        <div class="flx-floatL view-id">
                                            <a href="#">
                                                <h4>june 11th 2012</h4>
                                            </a>
                                        </div>
                                        <ul class="flx-floater view-filters">
                                            <li><a href="#">Time only <i class="icon-time"></i></a></li>
                                            <li><a href="#">Channel only <i class="icon-list"></i></a></li>
                                        </ul>
                                    </li>
                                    <li class="divider"></li>
                                    <li class="flx-clearfix">
                                        <div class="flx-floatL view-id">
                                            <a href="#"><h4>JPF Wed.</h4><p>This is the sample we made in Milan last November</p></a>
                                        </div>
                                        <ul class="flx-floater view-filters">
                                            <li><a href="#">Time only <i class="icon-time"></i></a></li>
                                            <li><a href="#">Channel only <i class="icon-list"></i></a></li>
                                        </ul>
                                    </li>
                                    <li class="divider"></li>
                                    <li class="flx-clearfix">
                                        <div class="flx-floatL view-id">
                                            <a href="#"><h4>Lack of Sleep 1</h4><p>You never know what a body can do...</p></a>
                                        </div>
                                        <ul class="flx-floater view-filters">
                                            <li><a href="#">Time only <i class="icon-time"></i></a></li>
                                            <li><a href="#">Channel only <i class="icon-list"></i></a></li>
                                        </ul>
                                    </li>
                                </ul>
                            </div>
                            <div class="btn-group dropdown save-view-subMenu">
                                <a class="btn">Save view  </a>
                                <a class="btn dropdown-toggle" data-toggle="dropdown"><span class="caret"></span></a>
                                    <ul class="dropdown-menu">
                                        <li>
                                            <form>
                                                <input type="text" title="insert view name" style="display:inline"/>
                                                <a href="#" class="btn btn-mini">save</a>
                                            </form>
                                            <h5>Or save as one of the views bellow</h5>
                                        </li>
                                        <li class="divider"></li>
                                        <li>
                                            <a class="savedView" href="#">
                                                <div><h4>Migraine</h4><p>12/07/10</p></div>
                                                <div>This is the sample we did in Milan in November comparing sleep to migraine intensity</div>
                                            </a>
                                        </li>
                                        <li class="divider"></li>
                                        <li>
                                            <a class="savedView" href="#">
                                                <div><h4>Lack of Sleep 1</h4><p>15/10/11</p></div>
                                                <div>Sleep disorder and (massive) food consumption</div>
                                            </a>
                                        </li> 
                                    </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <br/>
        
<!--
        <ul class="nav nav-tabs" id="calendarTabs">
            <li style="cursor:pointer" class="active">
                <a class="timeline-tab" tabname="timeline" data-toggle="tab">Timeline</a>
            </li>
            <li style="cursor:pointer">
                <a class="views-tab" tabname="view" data-toggle="tab"> Views</a>
            </li>
        </ul>  
-->  
</div>


<!-- end snippet 2012/12/11 -->

        <div class="container-fluid">
        <div class="row">
            <div class="span10">
                <div class="synchAll">
                    <span>Though your data are supposedly up to date, you might want to <a href="#" class="btn btn-info">sync all your devices now <i class="icon-refresh"></i></a></span>          
                </div>
            </div>
        </div>
        <div class="row">
            <div class="span10">
                <table class="table manageConnectors">
                    <thead>
                        <tr>
                            <th class="syncStatus">Sync status</th>
                            <th class="connectors">Connectors</th>
                            <th class="desc">Description</th>
                            <th class="lastSync">Last Sync.</th>
                            <th class="latestData">Latest Data</th>
                            <th class="remove">Remove</th>
                        </tr>   
                    </thead>
                    <tbody>
                        <tr>
                            <td class="syncStatus"><span class="syncLED-yes"></span></td>
                            <td class="connectors"><img src="/css/devicesPictures/bodymedia.jpg" alt="bodymedia"/></td>
                            <td class="desc"><h4>BodyMedia Armband</h4><p>Activity, calories and sleep pattern</p><a href="#">Manage <i class="icon-cog"></i></a></td>
                            <td class="lastSync"><p>Sept 17 2012 10:03:12</p><a href="#">Sync now <i class="icon-refresh"></i></a></td>
                            <td class="latestData"><p>Sept 27 2012 30:42:55</p><a href="#">View graph <i class="icon-bar-chart"></i></a></td>
                            <td class="remove"><a href="#"><i class="icon-trash"></i></a></td>
                        </tr>
                        <tr>
                            <td class="syncStatus "><span class="syncLED-yes"></span></td>
                            <td class="connectors"><img src="/css/devicesPictures/zeo.jpg" alt="zeo"/></td>
                            <td class="desc"><h4>Zeo Sleep Tracker</h4><p>Sleep quality monitor using a sensor headband</p><a href="#">Manage <i class="icon-cog"></i></a></td>
                            <td class="lastSync"><p>Sept 17 2012 10:03:12</p><a href="#">Sync now <i class="icon-refresh"></i></a></td>
                            <td class="latestData"><p>Sept 27 2012 30:42:55</p><a href="#">View graph <i class="icon-bar-chart"></i></a></td>
                            <td class="remove"><a href="#"><i class="icon-trash"></i></a></td>
                        </tr>
                        <tr>
                            <td class="syncStatus"><span class="syncLED-no"></span></td>
                            <td class="connectors"><img src="/css/devicesPictures/fitbit.jpg" alt="fitbit"/></td>
                            <td class="desc"><h4>Fitbit</h4><p>A wearable tracker for physical activity or sleep</p><a href="#">Manage <i class="icon-cog"></i></a></td>
                            <td class="lastSync"><p>Sept 17 2012 10:03:12</p><a href="#">Sync now <i class="icon-refresh"></i></a></td>
                            <td class="latestData"><p>Sept 27 2012 30:42:55</p><a href="#">View graph <i class="icon-bar-chart"></i></a></td>
                            <td class="remove"><a href="#"><i class="icon-trash"></i></a></td>
                        </tr>
                        <tr class="nowSynchro">
                            <td class="syncStatus"><span class="syncLED-waiting"><img src="/css/devicesPictures/load.gif" alt="load"/></span></td>
                            <td class="connectors"><img src="/css/devicesPictures/mymee.jpg" alt="mymee"/></td>
                            <td class="desc"><h4>Mobile observation log</h4><p>Track my migraine and food consumption</p><a href="#">Manage <i class="icon-cog"></i></a></td>
                            <td class="lastSync"><p>Now synchronizing</p><span>Sync now <i class="icon-refresh"></i></span></td>
                            <td class="latestData"><p>Sept 27 2012 30:42:55</p><a href="#">View graph <i class="icon-bar-chart"></i></a></td>
                            <td class="remove"><a href="#"><i class="icon-trash"></i></a></td>
                        </tr>
                        <tr class="flx-tableSepar">
                            <td colspan="6"></td>
                        </tr>
                        <tr>
                            <td class="syncStatus"><span class="syncLED-no"></span></td>
                            <td class="connectors"><img src="/css/devicesPictures/bodyTrack.jpg" alt="bodyTrack"/></td>
                            <td class="desc"><h4>Basestation</h4><p>Temperature, humidity and air pressure</p></td>
                            <td class="lastSync"><p>Sept 17 2012 10:03:12</p><a href="#">Sync now <i class="icon-refresh"></i></a></td>
                            <td class="latestData"><p>Sept 27 2012 30:42:55</p><a href="#">View graph <i class="icon-bar-chart"></i></a></td>
                            <td class="remove"><a href="#"><i class="icon-trash"></i></a></td>
                        </tr>
                        <tr>
                            <td class="syncStatus"><span class="syncLED-yes"></span></td>
                            <td class="connectors"><img src="/css/devicesPictures/mHealth.jpg" alt="mHealth"/></td>
                            <td class="desc"><h4>Atrix</h4><p>Illuminance, azimuth and pitch</p></td>
                            <td class="lastSync"><p>Sept 17 2012 10:03:12</p><a href="#">Sync now <i class="icon-refresh"></i></a></td>
                            <td class="latestData"><p>Sept 27 2012 30:42:55</p><a href="#">View graph <i class="icon-bar-chart"></i></a></td>
                            <td class="remove"><a href="#"><i class="icon-trash"></i></a></td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
        
            <div class="row">
                <div class="span12">
                    <h4>Variation 1</h4>
                    <ul class="flx-sourceFilters1">
                        <li>
                            <a href="#" class="fitbit">Fitbit</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active bodytrack">BodyTrack</a>
                        </li>
                        <li>
                            <a href="#" class="flx-inactive lastFM">Last FM</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active withings">Withings</a>
                        </li>
                        <li>
                            <span class="flx-disconnected picasa">Picasa</span>
                        </li>
                        <li>
                            <a href="#" class="flx-active twitter">Twitter</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active ggleCal">Google Cal.</a>
                        </li>
                    </ul>
                </div>
            <br/>
            <br/>

            </div>
            <div class="row">   
                <div class="span12">
                <h4>Variation 2</h4>
                    <ul class="flx-sourceFilters2">
                        <li>
                            <a href="#" class="fitbit">Fitbit</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active bodytrack">BodyTrack</a>
                        </li>
                        <li>
                            <a href="#" class="flx-inactive lastFM">Last FM</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active withings">Withings</a>
                        </li>
                        <li>
                            <span class="flx-disconnected picasa">Picasa</span>
                        </li>
                        <li>
                            <a href="#" class="flx-active twitter">Twitter</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active ggleCal">Google Calendar</a>
                        </li>
                    </ul>
                </div>
            <br/>
            <br/>
            </div>
            <div class="row">   
                <div class="span12">
                <h4>Variation 3</h4>
                    <ul class="flx-sourceFilters3">
                        <li>
                            <a href="#" class="fitbit">Fitbit</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active bodytrack">BodyTrack</a>
                        </li>
                        <li>
                            <a href="#" class="flx-inactive lastFM">Last FM</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active withings">Withings</a>
                        </li>
                        <li>
                            <span class="flx-disconnected picasa">Picasa</span>
                        </li>
                        <li>
                            <a href="#" class="flx-active twitter">Twitter</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active ggleCal">Calendar</a>
                        </li>
                    </ul>
                </div>
            </div>
            <div class="row">
                <div class="span12">    
                <h4>Variation 4 (Candide's)</h4>
                    <div class="flx-filtersWrapper">
                        <ul class="flx-sourceFilters4">
                            <li>
                                <a href="#" class="fitbit">Fitbit</a>
                            </li>
                            <li>
                                <a href="#" class="flx-active bodytrack">BodyTrack</a>
                            </li>
                            <li>
                                <a href="#" class="flx-inactive lastFM">Last FM</a>
                            </li>
                            <li>
                                <a href="#" class="flx-active withings">Withings</a>
                            </li>
                            <li>
                                <span class="flx-disconnected picasa">Picasa</span>
                            </li>
                            <li>
                                <a href="#" class="flx-active twitter">Twitter</a>
                            </li>
                            <li>
                                <a href="#" class="flx-active ggleCal">Calendar</a>
                            </li>
                        </ul>
                    </div>
                </div>
            <br/>
            <br/>
            </div>
            <div class="row">
                <div class="span12">
                <h4 style="margin-top:50px;">Variation 5 (Thomas's)</h4>
                    <ul class="flx-sourceFilters5">
                        <li>
                            <a href="#" class="fitbit">Fitbit</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active bodytrack">BodyTrack</a>
                        </li>
                        <li>
                            <a href="#" class="flx-inactive lastFM">Last FM</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active withings">Withings</a>
                        </li>
                        <li>
                            <span class="flx-disconnected picasa">Picasa</span>
                        </li>
                        <li>
                            <a href="#" class="flx-active twitter">Twitter</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active ggleCal">Calendar</a>
                        </li>
                    </ul>
                </div>
            <br/>
            <br/>
            </div>
            <div class="row-fluid">
                <div class="span12">
                <h4>Variation 6 (synthesis)</h4>
                    <ul class="flx-sourceFilters6">
                        <li>
                            <a href="#" class="fitbit">Fitbit</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active bodytrack">BodyTrack</a>
                        </li>
                        <li>
                            <a href="#" class="flx-inactive lastFM">Last FM</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active withings">Withings</a>
                        </li>
                        <li>
                            <span class="flx-disconnected picasa">Picasa</span>
                        </li>
                        <li>
                            <a href="#" class="flx-active twitter">Twitter</a>
                        </li>
                        <li>
                            <a href="#" class="flx-active ggleCal">Calendar</a>
                        </li>
                    </ul>
                </div>
            <br/>
            <br/>
            </div>
            <div class="row"> 
                <div class="span12">
            <!-- Remove when added to the clock tab -->
                    <div class="flx-clockToolTipContainer">
                        <div class="flx-toolTipBody strongBorder-Right"> <!-- JS : Append strongBorder class with -Top, -Left, -Right, Bottom according to toolTip position / Append the right color class to override default -->
                            <span class="flx-toolTipTail-Right" style="top:50px;"></span> <!-- JS : Append class with -Top, -Left or -Right to change tail direction + define top/bottom/right/left arguments in the style attribute to give the tail it's position -->
                            <div class="flx-toolTipData">
                                <h3 class="flx-dataType">Weight</h3>
                                    <ul class="flx-toolTipLinks">
                                        <li><a href="#">List <i class="icon-list"></i></a></li>
                                        <li><a href="#">Timeline <i class="icon-film"></i></a></li>
                                        <li><a href="#">Bodytrack <i class="icon-lemon"></i></a></li>
                                    </ul>
                                    <span class="flx-deviceIcon withings"></span>
                                <div class="flx-deviceData">
                                    <span class="flx-tTime">9:43</span>
                                    <span class="flx-data">71.05 kg</span>
                                </div>
                                <span class="flx-miniGraph"><img src="/css/dashboardImgDummies/w8.png" alt="w8"/></span>
                            </div>
                            
                            <div class="flx-toolTipContext">
                                <div class="flx-toolTipLocation">
                                    <h4><span>9:43</span> last seen here: <strong>RVB Studio, Brussels</strong></h4>
                                    <div>
                                        <img src="/css/dashboardImgDummies/w7.png" alt="w7"/>
                                    </div>
                                </div>
                                <div class="flx-toolTipWeather">
                                    <h4><span>15.C</span> Variable with showers</h4>
                                    <div class="weatherIcon">
                                        <img src="/css/dashboardImgDummies/sun%26rain.png" alt="sun-and-rain"/>
                                    </div>
                                    <ul class="flx-weatherDetails">
                                        <li>Windspeed <span>7km/h</span></li>
                                        <li>Humidity <span>54%</span></li>
                                        <li>Precipitation <span>2mm</span></li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>


    <jsp:include page="footer.jsp" />

    <script>
        window.FLX_RELEASE_NUMBER = "${release}";
    </script>

    <script
        src="//ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js"></script>
    <script>
        window.jQuery
                || document
                        .write('<script src="/static/js/jquery-1.7.1.min.js"><\/script>')
    </script>
    <g:compress>
        <script src="/static/js/jquery-ui-1.8.18.custom.min.js"></script>
        <script src="/static/js/jquery.ui.sortable-bt.js"></script>
        <script src="/static/tiny_mce/jquery.tinymce.js"></script>
        <script src="/static/js/json2.js"></script>
        <script src="/static/js/hogan-2.0.0.js"></script>
        <script src="/static/js/jquery.autoGrowInput.js"></script>
        <script src="/static/js/jquery.colorPicker.js"></script>
        <script src="/static/js/jquery.dd-uncompressed.js"></script>
        <script src="/static/js/jquery.tagedit.js"></script>
        <script src="/static/js/jquery.shorten.js"></script>
        <script src="/static/js/jquery.jqplot.min.js"></script>
        <script src="/static/js/jqplot-plugins/jqplot.categoryAxisRenderer.min.js"></script>
        <script src="/static/js/jqplot-plugins/jqplot.pointLabels.min.js"></script>
        <script src="/static/js/jqplot-plugins/jqplot.barRenderer.min.js"></script>
        <script src="/static/js/bootstrap-2.0.2.min.js"></script>
        <script src="/static/js/raphael-2.1.0.js"></script>
        <script src="/static/js/underscore-1.3.3-min.js"></script>
        <script src="/static/js/backbone-0.9.2-min.js"></script>
    </g:compress>
</body>
</html>
