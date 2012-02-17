function showPictures() {
	$("#picturePanel").show();
	$("#pictureDiv").load("/me/pictures");
}

function hidePictures() {
	$("#picturePanel").hide();
}

function showPicture(url) {
	$.facebox("<img src='"+url+"' width=640/>");
}
