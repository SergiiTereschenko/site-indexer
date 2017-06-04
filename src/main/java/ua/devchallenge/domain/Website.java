package ua.devchallenge.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Website {
    String url;
    String prevButtonXPath;
    String nextButtonXPath;
    String linkXpath;
    int maxScanDepth;
    double latestPercent;
}
