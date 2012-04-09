package com.fluxtream.connectors.bodymedia;

import com.fluxtream.connectors.annotations.Updater;

@Updater(prettyName = "BodyMedia", value = 88, objectTypes = {
		BodymediaBurnFacet.class, BodymediaSleepFacet.class,
		BodymediaStepsFacet.class }, hasFacets = true)
public class BodymediaUpdater {

}
