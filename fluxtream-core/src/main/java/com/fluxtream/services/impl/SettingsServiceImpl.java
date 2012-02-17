package com.fluxtream.services.impl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fluxtream.Configuration;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.GuestAddress;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.domain.GuestSettings.DistanceMeasureUnit;
import com.fluxtream.domain.GuestSettings.LengthMeasureUnit;
import com.fluxtream.domain.GuestSettings.TemperatureUnit;
import com.fluxtream.domain.GuestSettings.WeightMeasureUnit;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SettingsService;
import com.fluxtream.utils.JPAUtils;

@Transactional(readOnly = true)
@Service
public class SettingsServiceImpl implements SettingsService {

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

	@PersistenceContext
	EntityManager em;

	@Override
	@Transactional(readOnly = false)
	public GuestSettings getSettings(long guestId) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		if (settings != null)
			return settings;
		else {
			settings = new GuestSettings();
			settings.guestId = guestId;
			em.persist(settings);
			return settings;
		}
	}

	@Override
	@Transactional(readOnly = false)
	public void setFirstname(long guestId, String firstname) {
		Guest guest = guestService.getGuestById(guestId);
		guest.firstname = firstname;
		em.merge(guest);
	}

	@Override
	@Transactional(readOnly = false)
	public void setLastname(long guestId, String lastname) {
		Guest guest = guestService.getGuestById(guestId);
		guest.lastname = lastname;
		em.merge(guest);
	}

	@Override
	@Transactional(readOnly = false)
	public void setWeightMeasureUnit(long guestId, WeightMeasureUnit unit) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		settings.weightMeasureUnit = unit;
		em.merge(settings);
	}

	@Override
	public void setTemperatureUnit(long guestId, TemperatureUnit unit) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		settings.temperatureUnit = unit;
		em.merge(settings);
	}

	@Override
	@Transactional(readOnly = false)
	public void setLengthMeasureUnit(long guestId, LengthMeasureUnit unit) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		settings.lengthMeasureUnit = unit;
		em.merge(settings);
	}

	@Override
	@Transactional(readOnly = false)
	public void setDistanceMeasureUnit(long guestId, DistanceMeasureUnit unit) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		settings.distanceMeasureUnit = unit;
		em.merge(settings);
	}

	@Override
	@Transactional(readOnly=false)
	public void addAddress(long guestId, String add, double latitude,
			double longitude, long since, String jsonString) {
		GuestAddress address = new GuestAddress();
		address.guestId = guestId;
		address.address = add;
		address.latitude = latitude;
		address.longitude = longitude;
		address.since = since;
		address.jsonStorage = jsonString;
		em.persist(address);
	}

	@Override
	public GuestAddress getAddress(long guestId, long date) {
		GuestAddress guestAddress = JPAUtils.findUnique(em, GuestAddress.class, "address.when", guestId, date, date);
		return guestAddress;
	}

	@Override
	public void removeAddress(long guestId, long addressId) {
		GuestAddress add = em.find(GuestAddress.class, addressId);
		if (add.guestId==guestId)
			em.remove(add);
	}

}
