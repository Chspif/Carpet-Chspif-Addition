package chspif;

import carpet.api.settings.Rule;

import static carpet.api.settings.RuleCategory.*;

public class ChspifSettings
{
    public static final String CHSPIF = "chspif";

    @Rule(categories = {CHSPIF, FEATURE})
    public static boolean piglinIgnoreGoldTrim = false;
}
