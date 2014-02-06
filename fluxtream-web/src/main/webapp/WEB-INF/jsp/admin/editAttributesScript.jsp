<script>

    function editApiKeyAttributeValue(apiKeyId, attributeKey, attributeValue){
        var newValue = prompt("Edit Attribute Value", attributeValue);
        if (newValue!=null) {
            $.ajax({
                url: "/api/admin/apiKeys/" + apiKeyId + "/attribute",
                data: {attributeKey: attributeKey, attributeValue : newValue},
                type: "POST",
                success: function(){
                    location.reload();
                }
            });
        }
    }

    function deleteApiKeyAttributeValue(apiKeyId, attributeKey){
        var confirmed = confirm("Are you sure?");
        if (confirmed) {
            $.ajax({
                url: "/api/admin/apiKeys/" + apiKeyId + "/attribute/delete?attributeKey=" + encodeURIComponent(attributeKey),
                type: "DELETE",
                success: function(){
                    location.reload();
                }
            });
        }
    }

    function addApiKeyAttribute(apiKeyId){
        var newKey = prompt("Please, enter Attribute Key (/Name)");
        if (newKey==null) return;
        var newValue = prompt("Please, enter Attribute Value");
        if (newValue!=null) {
            $.ajax({
                url: "/api/admin/apiKeys/" + apiKeyId + "/attribute/add",
                data: {attributeKey: newKey, attributeValue : newValue},
                type: "POST",
                success: function(status){
                    if (status.result==="OK")
                        location.reload();
                    else
                        alert(status.message);
                }
            });
        }
    }

</script>
