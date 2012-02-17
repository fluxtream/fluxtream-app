<%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="com.fluxtream.connectors.vos.*"
%><%@ page import="com.fluxtream.connectors.withings.*"
%><%@ page import="com.fluxtream.connectors.sms_backup.*"
%><%@ page import="com.fluxtream.connectors.twitter.*"
%><%@ page import="com.fluxtream.connectors.fitbit.*"
%><%@ page import="com.fluxtream.connectors.zeo.*"
%><%@ page import="com.fluxtream.connectors.picasa.*"
%><%@ page import="com.fluxtream.connectors.lastfm.*"
%><%@ page import="com.fluxtream.mvc.models.HomeModel"
%><%@ page import="com.fluxtream.utils.Utils"
%><%@ page import="java.util.*"
%><%@ page import="org.joda.time.format.*"
%><%@ page import="org.joda.time.*"
%><%@ page import="com.fluxtream.CloudFront"
%><%@ page import="com.fluxtream.mvc.controllers.*"
%><%@ page import="com.fluxtream.connectors.*"
%><%@ page import="com.ocpsoft.pretty.time.PrettyTime"
%>
<div class="icon"></div>
<div class="ListItemDesc" style="overflow:hidden; height:100%;">

<%/* Withings ------------------------------------------------------------ */%>
<%
if (facet.type.startsWith("withings")&&facet.type.contains("weight")) {
	WithingsBodyScaleMeasureFacetVO wf = (WithingsBodyScaleMeasureFacetVO) facet;
%>
<div class="ListItemDate"><%= format.withZone(DateTimeZone.forTimeZone(timezone)).print(facet.start).trim() %></div>
<span class="weight bold"><%=wf.weight %></span> <span class="dataLabel"><%=wf.unitLabel %></span>
<%
	Connector connector = Connector.getConnector("withings");
	WithingsFacetVOCollection facetCollection = (WithingsFacetVOCollection) connector.getJsonFacetCollection();
	List<AbstractFacet> weightHistory = facetsHelper.getFacets(request, connector, true);
	facetCollection.extractFacets(weightHistory, timeInterval,
		settings);
	StringBuilder sb = new StringBuilder();
	float minValue = Float.MAX_VALUE;
	float maxValue = Float.MIN_VALUE;
	for (int i=0;i<facetCollection.weightMeasures.size();i++) {
		float v = facetCollection.weightMeasures.get(i).weight;
		if (v<minValue) minValue = v;
		if (v>maxValue) maxValue = v;
	};
	for (int i=0;i<facetCollection.weightMeasures.size();i++) {
		if (i>0) sb.append(",");
		float v = (float) Math.round(facetCollection.weightMeasures.get(i).weight * 10) / 10;
		v -= minValue;
		v = (float) Math.round(v * 10) / 10;
		v *= 30;
		sb.append(v);
	};
%>
<img src="http://chart.apis.google.com/chart?
	cht=lc
	&chs=100x30
	&chd=t:<%=sb.toString()%>&chco=336699
	&chls=1,1,0
	&chm=o,990000,0,20,4
	&chxt=r,x,y
	&chxs=0,990000,11,0,_|1,990000,1,0,_|2,990000,1,0,_
	&chxp=0,<%=wf.weight %>">
<%/* CALL ------------------------------------------------------------ */%>
<% } else if (facet instanceof CallLogEntryFacetVO) {
	CallLogEntryFacetVO clf = (CallLogEntryFacetVO) ((Object)facet); %>
<div class="ListItemDate"><%= format.withZone(DateTimeZone.forTimeZone(timezone)).print(facet.start).trim() %> <span class="dataLabel"><% if (clf.subType.equals("outgoing")) out.println("Call to"); else out.println("Call from"); %></span> <span class="personName"><%=clf.personName %></span></div>
<span class="talkTime"><%=clf.secondsTalking/60 %></span> <span class="dataLabel">min</span> <span class="talkTime"><%=clf.secondsTalking%60 %></span> <span class="dataLabel">sec</span>

<%/* SMS ------------------------------------------------------------ */%>
<% } else if (facet instanceof SmsEntryFacetVO) {
	SmsEntryFacetVO smsf = (SmsEntryFacetVO) ((Object)facet);%>
<div class="ListItemDate"><%= format.withZone(DateTimeZone.forTimeZone(timezone)).print(facet.start).trim() %> <span class="dataLabel"><% if (smsf.subType.equals("outgoing")) out.println("Text to"); else out.println("Text from"); %></span> <span class="personName"><%=smsf.personName %></span></div>
<span class="text" style="color:#9177b6;"><%=smsf.description %></span>

<%/* TWITTER DM ------------------------------------------------------------ */%>
<% } else if (facet instanceof TwitterDirectMessageFacetVO) {
	TwitterDirectMessageFacetVO dmvo = (TwitterDirectMessageFacetVO) ((Object)facet);%>
	<div class="ListItemDate"><%= format.withZone(DateTimeZone.forTimeZone(timezone)).print(facet.start).trim() %></div>
	<img src="<%=dmvo.profileImageUrl %>"></img>
	<span class="personName"><%=dmvo.userName %></span>
	<span class="text"><%=Utils.replaceLinks(dmvo.description) %></span>
	
<%/* TWITTER Mention ------------------------------------------------------------ */%>
<% } else if (facet instanceof TwitterMentionFacetVO) {
	TwitterMentionFacetVO tmvo = (TwitterMentionFacetVO) ((Object)facet);%>
	<div class="ListItemDate"><%= format.withZone(DateTimeZone.forTimeZone(timezone)).print(facet.start).trim() %></div>
	<img src="<%=tmvo.profileImageUrl %>"></img>
	<span class="personName"><%=tmvo.userName %></span>
	<span class="text"><%=Utils.replaceLinks(tmvo.description)%></span>

<%/* FITBIT ------------------------------------------------------------ */%>
<% } else if (facet instanceof FitbitSleepFacetVO) {
	FitbitSleepFacetVO svo = (FitbitSleepFacetVO) ((Object)facet);%>
	<div class="ListItemDate"><%=shortFormat.withZone(DateTimeZone.forTimeZone(timezone)).print(svo.riseTime.getTime())%> <span class="dataLabel">You slept</span> <span class="text bold"><%=(svo.minutesAsleep>59?svo.minutesAsleep/60:"")%></span> <span class="text">hours</span> <span class="text bold"><%=(svo.minutesAsleep%60) %></span> <span class="text">min</span></div>
	<span class="dataLabel">Bed time</span> <span class="text"><%=shortFormat.withZone(DateTimeZone.forTimeZone(timezone)).print(svo.bedTime.getTime())%></span>
	<span class="dataLabel">Awake</span> <span class="text"><%if (svo.minutesAwake>59) { out.print((svo.minutesAwake) + " hour " + svo.minutesAwake%60+" min"); } else out.print(svo.minutesAwake + "min"); %></span>
	<span class="dataLabel">Wake-up</span> <span class="text"><%=shortFormat.withZone(DateTimeZone.forTimeZone(timezone)).print(svo.riseTime.getTime())%></span>

<%/* ZEO ------------------------------------------------------------ */%>	
<% } else if (facet instanceof ZeoSleepStatsFacetVO) {
	ZeoSleepStatsFacetVO svo = (ZeoSleepStatsFacetVO) ((Object)facet);%>
	<div style="min-height:200px">
		<div class="ListItemDate"><%=shortFormat.withZone(DateTimeZone.forTimeZone(timezone)).print(svo.riseTime.getTime())%>	<span class="dataLabel">You slept </span> <span class="text bold"><%=(svo.minutesAsleep>59?svo.minutesAsleep/60:"")%></span> <span class="text">hours</span> <span class="text bold"><%=(svo.minutesAsleep%60) %></span> <span class="text">min</span></div>
		<span class="dataLabel">Bed time</span> <span class="text"><%=shortFormat.withZone(DateTimeZone.forTimeZone(timezone)).print(svo.bedTime.getTime())%></span>
		<span class="dataLabel">Awake</span> <span class="text"><%if (svo.minutesAwake>59) { out.print((Math.round(svo.minutesAwake/60)) + " hour " + svo.minutesAwake%60+" min"); } else out.print(svo.minutesAwake + "min"); %></span>
		<span class="dataLabel">Wake-up</span> <span class="text"><%=shortFormat.withZone(DateTimeZone.forTimeZone(timezone)).print(svo.riseTime.getTime())%></span>
		<span class="dataLabel">ZQ</span> <span class="text"><%=svo.zq%></span>
		<span class="dataLabel">Morning Feel</span> <span class="text"><%=svo.morningFeel%></span>
		<img height="140" src="https://mysleep.myzeo.com/graphservlet/Hypnogram?t=n&s=<%=zeoTimeFormat.withZone(DateTimeZone.forTimeZone(timezone)).print(svo.bedTime.getTime())%>&e=<%=zeoTimeFormat.withZone(DateTimeZone.forTimeZone(timezone)).print(svo.riseTime.getTime())%>&d=<%=svo.sleepGraph%>"/>
	</div>
<%/* LAST FM ------------------------------------------------------------ */%>	
<% } else if (facet instanceof LastFmRecentTrackFacetVO) {
	LastFmRecentTrackFacetVO lfmvo = (LastFmRecentTrackFacetVO) ((Object)facet);%>
	<div class="ListItemDate"><%=shortFormat.withZone(DateTimeZone.forTimeZone(timezone)).print(lfmvo.start)%><%/*= format.format(date).trim() */%></div>
	<% if (lfmvo.imgUrls!=null) {%>
		<img width="34" height="34" src="<%=CloudFront.cache(lfmvo.imgUrls[0])%>"/>	
	<% } %>
	<span class="dataLabel">Track</span> <span class="text"><%=Utils.replaceLinks(facet.description)%></span>
<%-- 	<span class="dataLabel">Artist</span> <span class="text"><%=lfmvo.artist%></span> --%>

<%/* PICASA ------------------------------------------------------------ */%>	
<% } else if (facet instanceof PicasaPhotoFacetVO) {
	PicasaPhotoFacetVO pvo = (PicasaPhotoFacetVO) ((Object)facet);%>
	<div class="ListItemDate"><%=shortFormat.withZone(DateTimeZone.forTimeZone(timezone)).print(pvo.start)%><%/*= format.format(date).trim() */%></div>
	<img style="cursor:pointer" onclick="showPicture('<%=pvo.photoUrl %>')" src="<%=pvo.thumbnailUrl%>"/>
	
<%/* ------------------------------------------------------------ */%>
<% } else { %>
<div class="ListItemDate"><%= format.withZone(DateTimeZone.forTimeZone(timezone)).print(facet.start).trim() %></div>
<%=facet.description!=null?Utils.replaceLinks(facet.description):"" %>
<% } %>
</div>
