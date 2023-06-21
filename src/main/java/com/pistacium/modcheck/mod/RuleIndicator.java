package com.pistacium.modcheck.mod;

import java.util.List;
import java.util.Map;

public class RuleIndicator {

    private final String osName;
    private final String category;
    private final boolean medicalIssue;

    public RuleIndicator(String osName, String category, boolean medicalIssue) {
        this.osName = osName;
        this.category = category;
        this.medicalIssue = medicalIssue;
    }

    public boolean checkWithRules(List<ModRule> ruleList) {
        if (ruleList == null) return true;
        for (ModRule modRule : ruleList) {
            boolean allowed = modRule.getAction().equals("allow");
            for (Map.Entry<String, String> entry : modRule.getProperties().entrySet()) {
                if (entry.getKey().equals("category") && entry.getValue().equals(category) != allowed) return false;
                if (entry.getKey().equals("os") && entry.getValue().equals(osName) != allowed) return false;
                if (entry.getKey().equals("condition") && entry.getValue().equals("medical_issue") && medicalIssue != allowed) return false;
            }
        }
        return true;
    }
}
