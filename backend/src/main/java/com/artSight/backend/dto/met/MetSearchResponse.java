package com.artSight.backend.dto.met;

import lombok.Data;
import java.util.List;

@Data
public class MetSearchResponse {
    private int total;
    private List<Long> objectIDs;
}