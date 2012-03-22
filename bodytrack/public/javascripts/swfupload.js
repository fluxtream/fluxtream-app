// This code was moved from app/views/logphotos/new.html.erb to public/javascripts/swfupload.js on 3/18/2010
// in an attempt to make things more modular.  It requires the following boilerplate in the erb file which uses it:
  // <% form_for [OBJECT], :html => { :multipart => true } do |f| %>
  // <div id="swfupload_degraded_container">
  //   <noscript>You should have Javascript enabled for a nicer upload experience</noscript>
  //   [Insert fallback html here]
  // </div>
  // <div id="swfupload_container" style="display: none">
  //   <span id="spanButtonPlaceholder"></span>
  // </div>
  // <div id="divFileProgressContainer"></div>
  // <% end %>
  //
  //<%= javascript_include_tag 'swfupload' %>
							   //
function fileDialogComplete(numFilesSelected, numFilesQueued) {
  try {
    alert("About to upload " + numFilesQueued + " files");
    if (numFilesQueued > 0) {
      this.startUpload();
    }
  } catch (ex) {
    this.debug(ex);
  }
}

function uploadProgress(file, bytesLoaded) {
  try {
    var percent = Math.ceil((bytesLoaded / file.size) * 100);

    var progress = new FileProgress(file,  this.customSettings.upload_target);
    progress.setProgress(percent);
    if (percent === 100) {
      progress.setStatus("Processing file...");
      progress.toggleCancel(false, this);
    } else {
      progress.setStatus("Uploading...");
      progress.toggleCancel(true, this);
    }
  } catch (ex) {
    this.debug(ex);
  }
}

function uploadSuccess(file, serverData) {
  try {
    var progress = new FileProgress(file,  this.customSettings.upload_target);

    if (serverData.substring(0, 2) === "OK") {
      //addImage("thumbnail.php?id=" + serverData.substring(7));

      progress.setStatus("Upload Successful.");
      progress.toggleCancel(false);
    } else {
      //addImage("/images/swfupload/error.gif");
      progress.setStatus("Error.");
      progress.toggleCancel(false);
      alert(serverData);
    }


  } catch (ex) {
    this.debug(ex);
  }
}

function uploadComplete(file) {
  try {
    /*  I want the next upload to continue automatically so I'll call startUpload here */
    if (this.getStats().files_queued > 0) {
      this.startUpload();
    } else {
      var progress = new FileProgress(file,  this.customSettings.upload_target);
      progress.setComplete();
      progress.setStatus("All files received.");
      progress.toggleCancel(false);
    }
  } catch (ex) {
    this.debug(ex);
  }
}

/* ******************************************
 *	FileProgress Object
 *	Control object for displaying file info
 * ****************************************** */

function FileProgress(file, targetID) {
	this.fileProgressID = "divFileProgress";

	this.fileProgressWrapper = document.getElementById(this.fileProgressID);
	if (!this.fileProgressWrapper) {
		this.fileProgressWrapper = document.createElement("div");
		this.fileProgressWrapper.className = "progressWrapper";
		this.fileProgressWrapper.id = this.fileProgressID;

		this.fileProgressElement = document.createElement("div");
		this.fileProgressElement.className = "progressContainer";

		var progressCancel = document.createElement("a");
		progressCancel.className = "progressCancel";
		progressCancel.href = "#";
		progressCancel.style.visibility = "hidden";
		progressCancel.appendChild(document.createTextNode(" "));

		var progressText = document.createElement("div");
		progressText.className = "progressName";
		progressText.appendChild(document.createTextNode(file.name));

		var progressBar = document.createElement("div");
		progressBar.className = "progressBarInProgress";

		var progressStatus = document.createElement("div");
		progressStatus.className = "progressBarStatus";
		progressStatus.innerHTML = "&nbsp;";

		this.fileProgressElement.appendChild(progressCancel);
		this.fileProgressElement.appendChild(progressText);
		this.fileProgressElement.appendChild(progressStatus);
		this.fileProgressElement.appendChild(progressBar);

		this.fileProgressWrapper.appendChild(this.fileProgressElement);

		document.getElementById(targetID).appendChild(this.fileProgressWrapper);
		fadeIn(this.fileProgressWrapper, 0);

	} else {
		this.fileProgressElement = this.fileProgressWrapper.firstChild;
		this.fileProgressElement.childNodes[1].firstChild.nodeValue = file.name;
	}

	this.height = this.fileProgressWrapper.offsetHeight;

}
FileProgress.prototype.setProgress = function (percentage) {
	this.fileProgressElement.className = "progressContainer green";
	this.fileProgressElement.childNodes[3].className = "progressBarInProgress";
	this.fileProgressElement.childNodes[3].style.width = percentage + "%";
};
FileProgress.prototype.setComplete = function () {
	this.fileProgressElement.className = "progressContainer blue";
	this.fileProgressElement.childNodes[3].className = "progressBarComplete";
	this.fileProgressElement.childNodes[3].style.width = "";

};
FileProgress.prototype.setError = function () {
	this.fileProgressElement.className = "progressContainer red";
	this.fileProgressElement.childNodes[3].className = "progressBarError";
	this.fileProgressElement.childNodes[3].style.width = "";

};
FileProgress.prototype.setCancelled = function () {
	this.fileProgressElement.className = "progressContainer";
	this.fileProgressElement.childNodes[3].className = "progressBarError";
	this.fileProgressElement.childNodes[3].style.width = "";

};
FileProgress.prototype.setStatus = function (status) {
	this.fileProgressElement.childNodes[2].innerHTML = status;
};

FileProgress.prototype.toggleCancel = function (show, swfuploadInstance) {
	this.fileProgressElement.childNodes[0].style.visibility = show ? "visible" : "hidden";
	if (swfuploadInstance) {
		var fileID = this.fileProgressID;
		this.fileProgressElement.childNodes[0].onclick = function () {
			swfuploadInstance.cancelUpload(fileID);
			return false;
		};
	}
};

SWFUpload.onload = function() {

  var swf_settings = {
    // SWFObject settings
    minimum_flash_version: "9.0.28",
    swfupload_pre_load_handler: function() {
      $('swfupload_degraded_container').hide();
      $('swfupload_container').show();
    },
    swfupload_load_failed_handler: function() {
      alert('sad');
    },

    post_params: {
      "<%= session_key_name %>": "<%= cookies[session_key_name] %>",
      "authenticity_token": "<%= form_authenticity_token %>",
    },

    upload_url: "<%= escape_javascript(logphotos_path) %>",
    flash_url: '/flash/swfupload/swfupload.swf',

    file_types: "*.jpg",  // TODO: what about jpeg, tiff, png, etc?
    file_types_description: "Image files",
    file_size_limit: "500 MB",

    button_placeholder_id: "spanButtonPlaceholder",
    button_width: 380,
    button_height: 32,
    button_text : '<span class="button">Select Images <span class="buttonSmall">(500 MB Max)</span></span>',
    button_text_style : '.button { font-family: Helvetica, Arial, sans-serif; font-size: 24pt; } .buttonSmall { font-size: 18pt; }',
    button_text_top_padding: 0,
    button_text_left_padding: 18,
    button_window_mode: SWFUpload.WINDOW_MODE.TRANSPARENT,
    button_cursor: SWFUpload.CURSOR.HAND,
//    file_queue_error_handler : fileQueueError,
    file_dialog_complete_handler : fileDialogComplete,
    upload_progress_handler : uploadProgress,
//    upload_error_handler : uploadError,
    upload_success_handler : uploadSuccess,
    upload_complete_handler : uploadComplete,

    custom_settings : {
      upload_target: "divFileProgressContainer"
    }
  }
  var swf_upload = new SWFUpload(swf_settings);
};
