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
    <link rel="stylesheet" href="/css/qtip/jquery.qtip.min.css">
    <link rel="stylesheet" href="/css/qtip/jquery.jqplot.min.css">
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

<body style="padding: 50px; background:#fff;">

    <div id="content">
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
                        <tr class="pouet">
                            <td class="syncStatus"><span class="syncLED-yes"></span></td>
                            <td class="connectors"><img src="/css/devicesPictures/bodymedia.jpg" alt="bodymedia" width="" height="" /></td>
                            <td class="desc"><h4>BodyMedia Armband</h4><p>Activity, calories and sleep pattern</p><a href="#">Manage <i class="icon-cog"></i></a></td>
                            <td class="lastSync"><p>Sept 17 2012 10:03:12</p><a href="#">Sync now <i class="icon-refresh"></i></a></td>
                            <td class="latestData"><p>Sept 27 2012 30:42:55</p><a href="#">View graph <i class="icon-bar-chart"></i></a></td>
                            <td class="remove"><a href="#"><i class="icon-trash"></i></a></td>
                        </tr>
                        <tr>
                            <td class="syncStatus "><span class="syncLED-yes"></span></td>
                            <td class="connectors"><img src="/css/devicesPictures/zeo.jpg" alt="zeo" width="" height="" /></td>
                            <td class="desc"><h4>Zeo Sleep Tracker</h4><p>Sleep quality monitor using a sensor headband</p><a href="#">Manage <i class="icon-cog"></i></a></td>
                            <td class="lastSync"><p>Sept 17 2012 10:03:12</p><a href="#">Sync now <i class="icon-refresh"></i></a></td>
                            <td class="latestData"><p>Sept 27 2012 30:42:55</p><a href="#">View graph <i class="icon-bar-chart"></i></a></td>
                            <td class="remove"><a href="#"><i class="icon-trash"></i></a></td>
                        </tr>
                        <tr>
                            <td class="syncStatus"><span class="syncLED-no"></span></td>
                            <td class="connectors"><img src="/css/devicesPictures/fitbit.jpg" alt="fitbit" width="" height="" /></td>
                            <td class="desc"><h4>Fitbit</h4><p>A wearable tracker for physical activity or sleep</p><a href="#">Manage <i class="icon-cog"></i></a></td>
                            <td class="lastSync"><p>Sept 17 2012 10:03:12</p><a href="#">Sync now <i class="icon-refresh"></i></a></td>
                            <td class="latestData"><p>Sept 27 2012 30:42:55</p><a href="#">View graph <i class="icon-bar-chart"></i></a></td>
                            <td class="remove"><a href="#"><i class="icon-trash"></i></a></td>
                        </tr>
                        <tr class="nowSynchro">
                            <td class="syncStatus"><span class="syncLED-waiting"><img src="/css/devicesPictures/load.gif" alt="load" width="" height="" /></span></td>
                            <td class="connectors"><img src="/css/devicesPictures/mymee.jpg" alt="mymee" width="" height="" /></td>
                            <td class="desc"><h4>Mobile observation log</h4><p>Track my migraine and food consumption</p><a href="#">Manage <i class="icon-cog"></i></a></td>
                            <td class="lastSync"><p>Now synchronizing</p><span href="#">Sync now <i class="icon-refresh"></i></span></td>
                            <td class="latestData"><p>Sept 27 2012 30:42:55</p><a href="#">View graph <i class="icon-bar-chart"></i></a></td>
                            <td class="remove"><a href="#"><i class="icon-trash"></i></a></td>
                        </tr>
                        <tr class="flx-tableSepar">
                            <td colspan="6"></td>
                        </tr>
                        <tr>
                            <td class="syncStatus"><span class="syncLED-no"></span></td>
                            <td class="connectors"><img src="/css/devicesPictures/bodyTrack.jpg" alt="bodyTrack" width="" height="" /></td>
                            <td class="desc"><h4>Basestation</h4><p>Temperature, humidity and air pressure</p></td>
                            <td class="lastSync"><p>Sept 17 2012 10:03:12</p><a href="#">Sync now <i class="icon-refresh"></i></a></td>
                            <td class="latestData"><p>Sept 27 2012 30:42:55</p><a href="#">View graph <i class="icon-bar-chart"></i></a></td>
                            <td class="remove"><a href="#"><i class="icon-trash"></i></a></td>
                        </tr>
                        <tr>
                            <td class="syncStatus"><span class="syncLED-yes"></span></td>
                            <td class="connectors"><img src="/css/devicesPictures/mHealth.jpg" alt="mHealth" width="" height="" /></td>
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
            <h4>Variation 1</h4>
                <div class="span12" style="min-height:50px">
                <br/>
                <br/>
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
            </div>
                            <br/>
                <br/>

            <div class="row">   
            <h4>Variation 2</h4>
                <div class="span12" style="min-height:50px">    
                <br/>
                <br/>
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
            </div>
                            <br/>
                <br/>

            <div class="row">   
            <h4>Variation 3</h4>
                <div class="span12" style="min-height:50px">    
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
            <br/>
            <br/>
            <div class="row">
            <h4>Variation 4 (Candide's)</h4>
                <div class="span12" style="min-height:50px">    
                                <br/>
            <br/>

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

            <div class="row">
            <h4>Variation 5 (Thomas's)</h4>
                <div class="span12" style="min-height:50px">    
                            <br/>
            <br/>

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
            </div>
                        <br/>
            <br/>

            <div class="row">
            <h4>Variation 6 (Peri's)</h4>
                <div class="span12" style="min-height:50px">    
                            <br/>
            <br/>

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
                                <span class="flx-miniGraph"><img src="/css/dashboardImgDummies/w8.png" alt="w8" width="" height="" /></span>
                            </div>
                            
                            <div class="flx-toolTipContext">
                                <div class="flx-toolTipLocation">
                                    <h4><span>9:43</span> last seen here: <strong>RVB Studio, Brussels</strong></h4>
                                    <div>
                                        <img src="/css/dashboardImgDummies/w7.png" alt="w7" width="" height="" />
                                    </div>
                                </div>
                                <div class="flx-toolTipWeather">
                                    <h4><span>15.C</span> Variable with showers</h4>
                                    <div class="weatherIcon">
                                        <img src="/css/dashboardImgDummies/sun%26rain.png" alt="sun-and-rain" width="" height="" />
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
