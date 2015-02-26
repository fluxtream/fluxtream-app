package org.fluxtream.core.api.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.GuestDetails;

import java.io.IOException;

import static org.fluxtream.core.utils.Utils.hash;

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
    @ApiModelProperty(value = "URL of the user's profile image", required = false)
    public String photoURL;

    public BasicGuestModel(Guest guest, GuestDetails details) {
        // indeed, Guest can be null
        if (guest!= null) {
            this.fullname = guest.getGuestName();
            this.username = guest.username;
            this.firstname = guest.firstname;
            this.lastname = guest.lastname;
            this.id = guest.getId();
        }
        if (details!=null)
            photoURL = details.avatarImageURL;
        photoURL = getGravatarImageURL(guest);
    }

    private String getGravatarImageURL(Guest guest) {
        String emailHash = hash(guest.email.toLowerCase().trim()); //gravatar specifies the email should be trimmed, taken to lowercase, and then MD5 hashed
        String gravatarURL = String.format("http://www.gravatar.com/avatar/%s?s=256&d=retro", emailHash);
        HttpGet get = new HttpGet(gravatarURL);
        int res = 0;
        try { res = ((new DefaultHttpClient()).execute(get)).getStatusLine().getStatusCode(); }
        catch (IOException e) {e.printStackTrace();}
        return res==200 ? gravatarURL : null;
    }

}