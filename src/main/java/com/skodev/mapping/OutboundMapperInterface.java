package com.skodev.mapping;

import com.skodev.exceptions.MappingException;
import com.skodev.exceptions.VtigerInterExc;
import java.io.File;

public interface OutboundMapperInterface {
    public File newPurchaseOrder(String id) throws MappingException, VtigerInterExc;
    //public void anotherPossibleOperation(JSONObject input);
}
