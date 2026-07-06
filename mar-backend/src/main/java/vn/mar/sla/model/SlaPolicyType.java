package vn.mar.sla.model;

import vn.mar.lead.model.LeadTemperature;

public enum SlaPolicyType {
    HOT,
    NORMAL,
    AFTER_HOURS;

    public static SlaPolicyType fromLeadTemperature(LeadTemperature leadTemperature) {
        if (leadTemperature == null || leadTemperature == LeadTemperature.NORMAL) {
            return NORMAL;
        }
        if (leadTemperature == LeadTemperature.AFTER_HOURS) {
            return AFTER_HOURS;
        }
        return HOT;
    }
}
