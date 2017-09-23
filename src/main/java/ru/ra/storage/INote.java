package ru.ra.storage;

import java.util.Date;

import ru.ra.AuthInfo;

public interface INote {
 
    interface INoteCoord {

        String getId();

        AuthInfo getOwner();
    }
    
    INoteCoord getCoord();

    Date getCreationDate();

    String asText();
}
