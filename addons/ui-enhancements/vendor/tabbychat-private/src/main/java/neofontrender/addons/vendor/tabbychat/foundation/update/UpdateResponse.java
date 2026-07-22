package neofontrender.addons.vendor.tabbychat.foundation.update;

import com.google.gson.annotations.SerializedName;

public class UpdateResponse {

    @SerializedName("@MCVERSION@")
    public Version minecraft;

    public class Version {
        double revision;
        String version;
        String changes;
    }
}
