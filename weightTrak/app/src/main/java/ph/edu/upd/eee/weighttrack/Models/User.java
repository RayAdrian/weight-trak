package ph.edu.upd.eee.weighttrack.Models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {

    public String email;
    public String name;
    public String birthdate;
    public String occupation;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String email, String name, String birthdate, String occupation) {
        this.email = email;
        this.name = name;
        this.birthdate = birthdate;
        this.occupation = occupation;
    }

}

