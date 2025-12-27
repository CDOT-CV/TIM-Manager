package com.trihydro.cvdatacontroller.services;

import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;

import java.util.List;

public interface MilepostService {
    List<Milepost> getMilepostsByStartEndPoint(WydotTim wydotTim) throws Exception;
    List<Milepost> getMilepostsByPointWithBuffer(MilepostBuffer milepostBuffer) throws Exception;
}
