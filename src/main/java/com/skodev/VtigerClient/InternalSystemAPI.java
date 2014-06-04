package com.skodev.VtigerClient;

import com.skodev.exceptions.VtigerInterExc;
import java.util.Map;

public interface InternalSystemAPI {
    public void newORDERS(Map obj) throws VtigerInterExc;
}
