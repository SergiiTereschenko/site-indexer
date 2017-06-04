package ua.devchallenge.domain.dto;

import lombok.Value;
import ua.devchallenge.domain.Website;

@Value
public class ScanEvent {
    Website website;
    int existingScanDepth;
}
