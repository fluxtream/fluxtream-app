<div class="modal" id="uploadModal">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">&times;</button>
        <h3 id="myModalLabel">Upload ${connector.prettyName} Data File</h3>
    </div>
    <form class="form-horizontal" id="fileUploadForm" action="javascript:void(0)">
        <div class="control-group">
            <label class="control-label" for="file">File</label>
            <div class="controls">
                <input type="hidden" name="connectorName" value="${connector.name}">
                <input type="file" name="file" id="file">
                <span class="help-inline">
                    ${message}
                </span>
            </div>
        </div>
        <div class="control-group">
            <div class="controls">
                <a href="#" id="submitFileUploadForm" class="btn btn-primary">Upload</a>
            </div>
        </div>
    </form>
</div>
