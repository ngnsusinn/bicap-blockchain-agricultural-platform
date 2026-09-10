package vn.courses.ut.edu.javaprogramming.bicap.service;

import vn.courses.ut.edu.javaprogramming.bicap.dto.IotDataRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.IotData;

public interface IotDataService {
    IotData saveAndCheckThresholds(IotDataRequest request);
    void generateDailySummary();
}
