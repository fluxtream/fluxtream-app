package org.fluxtream.core.mvc.models.guest;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.mvc.models.AvatarImageModel;

@ApiModel(value = "General info about a system's guest")
public class GuestModel {

    @ApiModelProperty(value="The guest's fullname", required=true)
    public String fullname;
    @ApiModelProperty(value="The guest's (unique) username", required=true)
	public String username;
    @ApiModelProperty(value="The guest's first name", required=false)
	public String firstname;
    @ApiModelProperty(value="The guest's last name", required=false)
    public String lastname;
    @ApiModelProperty(value="The guest's e-mail address", required=true)
    public String email;
    @ApiModelProperty(value="The guest's technical roles", required=true)
    public String roles;
    @ApiModelProperty(value="The guest's technical ID", required=true)
    public long id;
    @ApiModelProperty(value="Is this one of the logged-in user's buddies?", required=true)
    public boolean isBuddy;
    @ApiModelProperty(value="The guest's avatar", required=false)
    public AvatarImageModel avatar;

    public GuestModel() {}

	public GuestModel(Guest guest, boolean isBuddy) {
        this.fullname = guest.getGuestName();
		this.username = guest.username;
		this.firstname = guest.firstname;
		this.lastname = guest.lastname;
        this.email = guest.email;
        this.roles = guest.getUserRoles().toString();
        this.isBuddy = isBuddy;
        this.id=guest.getId();
    }
	
}
