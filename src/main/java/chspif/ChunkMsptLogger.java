package chspif;

import carpet.logging.Logger;
import carpet.logging.HUDLogger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ChunkMsptLogger extends HUDLogger
{
    private static final Map<String, Boolean> PREV_SUBSCRIPTION = new HashMap<>();

    public ChunkMsptLogger(Field field, String logName, String def, String[] options, boolean strictOptions)
    {
        super(field, logName, def, options, strictOptions);
    }

    @Override
    public void addPlayer(String playerName, String option)
    {
        boolean wasSubscribed = wasSubscribed(playerName);
        super.addPlayer(playerName, option);
        if ("status".equals(option))
        {
            PREV_SUBSCRIPTION.put(playerName, wasSubscribed);
        }
    }

    public static boolean takePreviousSubscription(String playerName)
    {
        Boolean v = PREV_SUBSCRIPTION.remove(playerName);
        return v != null && v;
    }

    @SuppressWarnings("unchecked")
    private boolean wasSubscribed(String playerName)
    {
        try
        {
            Field f = Logger.class.getDeclaredField("subscribedOnlinePlayers");
            f.setAccessible(true);
            Map<String, String> map = (Map<String, String>) f.get(this);
            return map.containsKey(playerName);
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
