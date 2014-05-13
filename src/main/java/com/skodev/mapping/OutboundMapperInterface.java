package com.skodev.mapping;

import org.json.simple.JSONObject;

public interface OutboundMapperInterface {
    void newOrder(JSONObject order);
    void anotherPossibleOperation(JSONObject input);
}
