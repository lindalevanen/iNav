package fi.aalto.tshalaa1.inav.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Linda on 23/08/16.
 */
public class ServerErrorResponse {
    @JsonProperty
    private long id;

    public void ServerErrorResponse(long id) {
        this.id = id;
    }

    public ServerErrorResponse() {}

    public long getID() { return this.id; }

}
