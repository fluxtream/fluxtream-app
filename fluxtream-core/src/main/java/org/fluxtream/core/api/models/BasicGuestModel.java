package org.fluxtream.core.api.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.fluxtream.core.domain.Guest;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@ApiModel("Basic information on a guest")
public class BasicGuestModel {

    @ApiModelProperty(value = "The guest's \"full name\" (firstname and lastname)", required = true)
    public String fullname = "This user is no longer";
    @ApiModelProperty(value = "The guest's (unique) username", required = true)
    public String username = "?";
    @ApiModelProperty(value = "The guest's first name", required = true)
    public String firstname = "?";
    @ApiModelProperty(value = "The guest's last name", required = true)
    public String lastname = "?";
    @ApiModelProperty(value = "The guest's unique ID", required = true)
    public long id;

    public BasicGuestModel(Guest guest) {
        // indeed, Guest can be null
        if (guest!= null) {
            this.fullname = guest.getGuestName();
            this.username = guest.username;
            this.firstname = guest.firstname;
            this.lastname = guest.lastname;
            this.id = guest.getId();
        }
    }

}