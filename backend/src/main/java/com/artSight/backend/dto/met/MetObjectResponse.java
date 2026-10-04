package com.artSight.backend.dto.met;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetObjectResponse {
    private long objectID;
    private String title;
    private String artistDisplayName;
    private String objectDate;
    private String medium;
    private String culture;
    private String department;
    private String creditLine;
    private String primaryImage;
    private boolean isPublicDomain;
}

