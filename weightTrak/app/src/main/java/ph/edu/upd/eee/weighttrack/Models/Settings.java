package ph.edu.upd.eee.weighttrack.Models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Settings {

    public String weightUnit;
    public boolean autoSync;

    public Settings() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }
    public Settings(String weightUnit, boolean autoSync) {
        this.weightUnit = weightUnit;
        this.autoSync = autoSync;
    }
}
