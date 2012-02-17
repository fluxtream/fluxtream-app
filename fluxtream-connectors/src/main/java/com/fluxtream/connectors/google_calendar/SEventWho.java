package com.fluxtream.connectors.google_calendar;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SEventWho implements Serializable {
	
	public String email;
	public String attendeeStatus;
	public String attendeeType;
	public String valueString;
}